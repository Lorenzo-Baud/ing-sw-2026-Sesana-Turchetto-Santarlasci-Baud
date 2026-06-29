package it.polimi.ingws.gc8.View.TUI;

import it.polimi.ingws.gc8.Controller.actions.PlaceTotemAction;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.actions.ReturnTotemAction;
import it.polimi.ingws.gc8.Controller.actions.TakeCardAction;
import it.polimi.ingws.gc8.Controller.actions.TakeFoodAction;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;
import it.polimi.ingws.gc8.Network.Messages.ConnectionUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.EventResolvedMessage;
import it.polimi.ingws.gc8.Network.Messages.GameOverMessage;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ModelUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.Network.Messages.PhaseUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.TurnUpdateMessage;
import it.polimi.ingws.gc8.View.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Procedural TUI (no external libraries: just {@code Scanner} + ANSI).
 *
 * <h2>Threading Model</h2>
 * Two threads:
 * <ul>
 *   <li><b>main (UI thread)</b> — executes {@link #run()}, prints screens
 *       and reads from stdin. Waits for its turn by polling lightly
 *       on {@code gameView}.</li>
 *   <li><b>update-dispatcher</b> — daemon launched once, reads the
 *       {@code messages} queue (populated by the network pump of ClientController)
 *       and applies updates to {@code gameView} without touching stdin.
 *       Prints errors to screen, ignores informative notifications (phase/turn).</li>
 * </ul>
 *
 * <h2>Input</h2>
 * Everything via numbers + ENTER; {@code 0} is always "back / skip".
 * The {@code Scanner} is used defensively ({@code readInt}, {@code readLine})
 * to tolerate malformed input.
 *
 * <h2>Windows Notes</h2>
 * We do not manage clear-screen or raw-mode: every redraw is an append to stdout,
 * compatible with cmd.exe without VT. ANSI escapes are used only for colors:
 * on terminals that don't support them they appear as noisy sequences but the
 * TUI remains playable.
 */
public class TuiView extends View {

    private final Scanner in = new Scanner(System.in);

    /** While true, the loop in {@link #run()} continues. */
    private volatile boolean running = true;

    /** Thread that drains the {@code messages} queue in background. */
    private Thread updateDispatcher;

    /** Timestamp of the last ErrorMessage received (epoch ms). */
    private volatile long lastErrorMessageAt;

    /**
     * Queue of UI notifications produced by the dispatcher thread that the
     * main UI thread must print. The dispatcher MUST NOT write to stdout
     * directly. Each entry is a tuple of (severity, text); the main thread drains
     * it whenever it is about to render a screen or read input.
     */
    private final ConcurrentLinkedQueue<UiNotice> pendingNotices = new ConcurrentLinkedQueue<>();

    // Pick counters for the in-flight ACTION_RESOLUTION turn, resynced every
    // frame from the authoritative snapshot (see runResolutionPhase). The server
    // is the single source of truth, so these never drift and reset for free when
    // the turn passes (the new snapshot carries zeros).
    private int turnUpperTaken = 0;
    private int turnLowerTaken = 0;
    private boolean turnFoodTaken = false;

    private enum NoticeLevel { INFO, ERROR }
    private record UiNotice(NoticeLevel level, String text) {}

    private void enqueueNotice(NoticeLevel level, String text) {
        pendingNotices.offer(new UiNotice(level, text));
    }

    /** Called by the UI thread before any render / prompt. */
    private void drainPendingNotices() {
        UiNotice n;
        while ((n = pendingNotices.poll()) != null) {
            switch (n.level()) {
                case INFO  -> Printer.printInfo(n.text());
                case ERROR -> Printer.printError(n.text());
            }
        }
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    @Override
    public void run() {
        startUpdateDispatcher();

        Printer.printInfo("=====================================");
        Printer.printInfo("       MESOS — TUI client");
        Printer.printInfo("=====================================");

        askNickname();
        waitForLobbyStart();

        // Main loop: each iteration is a turn for the player.
        while (running) {
            drainPendingNotices();
            GameDTO g = gameView;
            if (g == null) { sleep(100); continue; }

            if (g.phase() == GamePhase.GAME_OVER) {
                printGameOver(g);
                running = false;
                break;
            }

            if (isMyTurn()) {
                switch (g.phase()) {
                    case POSITIONING        -> runPositioningPhase();
                    case ACTION_RESOLUTION  -> runResolutionPhase();
                    case END_ROUND, SETUP   -> waitForMyTurn();
                    case GAME_OVER          -> { /* handled above */ }
                }
            } else {
                runSpectatorMenu();
            }
        }

        stopUpdateDispatcher();
    }

    // ------------------------------------------------------------------
    // Preludio: nickname + lobby
    // ------------------------------------------------------------------

    /**
     * Defensive fallback: in normal flows {@code Bootstrap} has already
     * collected the nickname and {@code ClientMain} has injected it via
     * {@link #setNickname(String)} <em>before</em> calling {@link #run()},
     * so this loop exits immediately. It runs only if the nickname was
     * not initialised, e.g. when the View is invoked outside the main
     * client bootstrap flow (tests, debug, alternative entry points).
     *
     * <p>The nickname is sent to the server by
     * {@code connection.login(nickname)} during the client bootstrap —
     * not from here — so no {@code PlayerAction} is dispatched.</p>
     */
    private void askNickname() {
        while (nickname == null || nickname.isBlank()) {
            Printer.printPrompt("Choose your nickname (max " + MAX_NICKNAME_LEN + " chars): ");
            String n = readLine();
            if (n == null) {
                running = false;
                return;
            }
            n = n.trim();
            if (n.isBlank()) {
                Printer.printError("Invalid nickname.");
                continue;
            }
            if (n.length() > MAX_NICKNAME_LEN) {
                Printer.printError("Nickname too long (max " + MAX_NICKNAME_LEN + " chars).");
                continue;
            }
            setNickname(n);
            Printer.printSuccess("Hi, " + getNickname() + "!");
        }
    }

    /**
     * Maximum nickname length tolerated by the TUI render. Mirrors the
     * width of the {@code pad(..)} cell used for nicknames in the offer
     * track / leaderboard / order track, so names always fit without
     * being silently truncated.
     */
    private static final int MAX_NICKNAME_LEN = 12;

    /** Waits for the first game snapshot, printing lobby updates. */
    private void waitForLobbyStart() {
        Printer.printInfo("Waiting for other players… (type Q + ENTER to quit)");
        while (running && gameView == null) {
            drainPendingNotices();
            if (checkQuitRequested()) {
                running = false;
                return;
            }
            sleep(200);
        }
        drainPendingNotices();
    }

    /**
     * Non-blocking check: returns {@code true} if the user typed Q/q on stdin.
     * Used by waiting loops (lobby / between turns) so the user is never
     * stuck without a way to exit.
     */
    private boolean checkQuitRequested() {
        try {
            if (System.in.available() > 0) {
                String line = readLine();
                if (line != null && line.trim().equalsIgnoreCase("Q")) {
                    Printer.printInfo("Quit requested by user.");
                    return true;
                }
            }
        } catch (java.io.IOException ignored) {}
        return false;
    }

    // ------------------------------------------------------------------
    // POSITIONING
    // ------------------------------------------------------------------

    private void runPositioningPhase() {
        GameDTO g = gameView;
        if (g == null) return;

        Printer.printHeader("YOUR TURN — POSITIONING");
        GamePrinter.printBoard(g, getNickname());

        List<OfferTileDTO> free = freeTiles(g);
        if (free.isEmpty()) {
            // Defensive: if it's our turn in POSITIONING the server guarantees
            // at least one free tile. Reaching this branch means desynced
            // state — surface the error and return to the main loop.
            Printer.printError("Inconsistent state: no offer tile available "
                    + "during your POSITIONING turn. Returning to main menu.");
            return;
        }

        System.out.println();
        Printer.printInfo("Choose the offer tile on which to place your totem:");
        for (int i = 0; i < free.size(); i++) {
            OfferTileDTO t = free.get(i);
            System.out.println("  " + (i + 1) + ") "
                    + t.id() + "  " + GamePrinter.iconFor(t.id()));
        }
        // MESOS rules: placing the totem is mandatory, no skip allowed.

        int choice = chooseInRange(1, free.size());

        if (!ensureMyPlayerId()) return;
        OfferTileDTO chosen = free.get(choice - 1);
        PlayerAction action = new PlaceTotemAction(myPlayerId(), chosen.id().name());
        Printer.printSuccess("Placing totem on tile " + chosen.id() + "…");
        SendResult res = sendAndAwaitUpdate(action);
        if (res == SendResult.ERROR) {
            Printer.printError("Totem placement was rejected by the server.");
        }
    }

    /**
     * Guard: verify the local player id is known before sending any
     * action. Without it the server would reject the action with id=-1
     * and the user would see a generic error.
     */
    private boolean ensureMyPlayerId() {
        if (myPlayerId() >= 0) return true;
        Printer.printError("Player id not yet assigned by the server. "
                + "Wait a moment and retry.");
        return false;
    }

    // ------------------------------------------------------------------
    // ACTION_RESOLUTION
    // ------------------------------------------------------------------

    private void runResolutionPhase() {
        while (running && isMyTurn()) {
            // Single snapshot per frame: every quantity displayed in this
            // iteration must come from the same `g`, otherwise the prompt
            // and the board can refer to different game states.
            GameDTO g = gameView;
            if (g == null || g.phase() != GamePhase.ACTION_RESOLUTION) return;

            // Pick counters come straight from the authoritative snapshot (the
            // server is the single source of truth). Reading them every frame
            // keeps the local view from drifting and also handles turn
            // boundaries for free: when the turn passes, the server resets these
            // to zero and the next snapshot reflects it. This also closes the
            // old "Back" exploit without manual turn-id tracking.
            turnUpperTaken = g.currentUpperTaken();
            turnLowerTaken = g.currentLowerTaken();
            turnFoodTaken = g.currentFoodTaken();

            OfferTileDTO myTile = findMyTile(g);
            String tileLabel = myTile == null
                    ? "?"
                    : myTile.id() + " " + GamePrinter.iconFor(myTile.id());
            int maxUpper = myTile == null ? 0 : maxUpperPicks(myTile.id());
            int maxLower = myTile == null ? 0 : maxLowerPicks(myTile.id());
            boolean canFood = myTile != null && allowsFood(myTile.id());

            // MESOS rule (pag.4): if there are still Character cards in a
            // row from which you should still be picking, you MUST take
            // them. The TUI hides "Return totem" until those mandatory
            // picks are exhausted (or no Character cards remain there).
            boolean mustPickUpper = turnUpperTaken < maxUpper
                    && hasCharacter(g.upperRowCards());
            boolean mustPickLower = turnLowerTaken < maxLower
                    && hasCharacter(g.lowerRowCards());
            boolean mustPick = mustPickUpper || mustPickLower;

            Printer.printHeader("YOUR TURN — ACTION RESOLUTION");
            GamePrinter.printBoard(g, getNickname());

            System.out.println();
            Printer.printInfo("You are on tile " + tileLabel + ". What do you want to do?");
            if (mustPick) {
                Printer.printInfo("(You must take the remaining mandatory Character cards "
                        + "before returning the totem.)");
            }

            // Create action choices
            List<String> tags = new ArrayList<>();
            int n = 1;
            if (turnUpperTaken < maxUpper) {
                System.out.println("  " + n++ + ") Take card from UPPER row (" + turnUpperTaken + "/" + maxUpper + ")");
                tags.add("UPPER");
            }
            if (turnLowerTaken < maxLower) {
                System.out.println("  " + n++ + ") Take card from LOWER row (" + turnLowerTaken + "/" + maxLower + ")");
                tags.add("LOWER");
            }
            if (canFood && !turnFoodTaken) {
                System.out.println("  " + n++ + ") Take food");
                tags.add("FOOD");
            }
            if (!mustPick) {
                String costHint = isLastReturnSlot(g)
                        ? "  [last slot: pays 1 food, or -2 PP if no food]"
                        : "";
                System.out.println("  " + n + ") Return totem (end turn)" + costHint);
                tags.add("RETURN");
            }
            // No "Back / skip turn" here: MESOS rules forbid skipping a turn.
            // The only way to end the turn is "Return totem", which is only
            // offered when no mandatory Character pick is still owed.

            int choice = chooseInRange(1, tags.size());
            if (!ensureMyPlayerId()) return;

            // Perform action based on choice — only update local counters
            // when the server actually confirmed the action (SendResult.OK).
            boolean didAct = false;
            switch (tags.get(choice - 1)) {
                case "UPPER" -> {
                    if (takeCardFromRow(g.upperRowCards(), CardRow.UPPER, "UPPER")) {
                        turnUpperTaken++;
                        didAct = true;
                    }
                }
                case "LOWER" -> {
                    if (takeCardFromRow(g.lowerRowCards(), CardRow.LOWER, "LOWER")) {
                        turnLowerTaken++;
                        didAct = true;
                    }
                }
                case "FOOD"  -> {
                    Printer.printSuccess("Taking food…");
                    if (sendAndAwaitUpdate(new TakeFoodAction(myPlayerId())) == SendResult.OK) {
                        turnFoodTaken = true;
                        didAct = true;
                    }
                }
                case "RETURN" -> {
                    Printer.printSuccess("Returning totem…");
                    if (sendAndAwaitUpdate(new ReturnTotemAction(myPlayerId())) == SendResult.OK) {
                        return;
                    }
                    // On ERROR/TIMEOUT we stay in the loop so the user can retry.
                }
            }

            // Auto-return the totem when no further pick is possible at all:
            // all of UPPER/LOWER/FOOD quotas are exhausted, so there is
            // nothing left to do but end the turn. Saves the player a click
            // and removes a class of "I forgot to press Return" mistakes.
            if (didAct
                    && turnUpperTaken == maxUpper
                    && turnLowerTaken == maxLower
                    && (!canFood || turnFoodTaken)) {
                Printer.printSuccess("All picks done — returning totem automatically.");
                if (sendAndAwaitUpdate(new ReturnTotemAction(myPlayerId())) == SendResult.OK) {
                    return;
                }
                // If the server rejects the auto-return (shouldn't happen in
                // normal flow), fall through to the next loop iteration so
                // the player sees the menu and can retry.
            }
        }
    }

    /** Searches for the tile occupied by the current player. */
    private static OfferTileDTO findMyTile(GameDTO g) {
        if (g == null || g.offerTrack() == null) return null;
        if (g.currentPlayer() == null) return null;
        int currentPlayerId = g.currentPlayer().playerId();
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.playerIndexOccupant() == currentPlayerId) return t;
        }
        return null;
    }



    /**
     * Sub-menu: choose a card from one of two rows. Returns {@code true}
     * only if the server confirmed the pick with a fresh snapshot — so
     * the caller can safely increment its taken-cards counter.
     *
     * <p>EVENT cards are <b>shown</b> in the row (the player must see
     * which events are out there, both for placement strategy and for
     * end-of-Era resolution) but cannot be selected — MESOS rule pag.4:
     * "Non potete mai prendere carte Evento da nessuna fila". The TUI
     * marks them with {@code [EVENT — not pickable]} and rejects the
     * choice client-side, before any action is sent.</p>
     */
    private boolean takeCardFromRow(List<CardDTO> cards, CardRow row, String label) {
        if (cards == null || cards.isEmpty()) {
            Printer.printError("Row " + label + " is empty.");
            return false;
        }
        System.out.println();
        Printer.printInfo("Available cards in row " + label + ":");
        for (int i = 0; i < cards.size(); i++) {
            CardDTO c = cards.get(i);
            String marker = isEvent(c) ? "  [EVENT — not pickable]" : "";
            System.out.println("  " + (i + 1) + ") "
                    + GamePrinter.formatCardAttributes(c) + marker);
        }
        System.out.println("  0) Back");

        int choice = chooseInRange(0, cards.size());
        if (choice == 0) return false;

        int cardIndex = choice - 1;
        CardDTO chosen = cards.get(cardIndex);
        if (isEvent(chosen)) {
            Printer.printError("Event cards cannot be taken. Pick a Character or Building card.");
            return false;
        }
        if (!ensureMyPlayerId()) return false;
        Printer.printSuccess("Taking: " + GamePrinter.formatCardAttributes(chosen));
        return sendAndAwaitUpdate(new TakeCardAction(myPlayerId(), cardIndex, row))
                == SendResult.OK;
    }

    private static boolean isEvent(CardDTO c) {
        return c != null && c.type() == CardType.EVENT;
    }

    /** True if the row contains at least one CHARACTER card. */
    private static boolean hasCharacter(List<CardDTO> row) {
        if (row == null) return false;
        for (CardDTO c : row) {
            if (c != null && c.type() == CardType.CHARACTER) return true;
        }
        return false;
    }

    /**
     * MESOS rule (pag.4): the player who places their totem on the LAST
     * slot of the Turn Order track pays 1 food (or -2 PP if they have
     * none). Heuristic: the last slot is occupied if the track is
     * already filled to {@code numberOfPlayers - 1} entries — placing
     * one more (this player's totem) lands on the last slot.
     */
    private static boolean isLastReturnSlot(GameDTO g) {
        if (g == null || g.turnOrderTrack() == null) return false;
        int filled = 0;
        for (PlayerDTO p : g.turnOrderTrack()) {
            if (p != null) filled++;
        }
        return filled == g.numberOfPlayers() - 1;
    }

    // ------------------------------------------------------------------
    // Spectator menu: not my turn
    // ------------------------------------------------------------------

    /**
     * Shows the board and automatically updates every time the server
     * sends a new snapshot, without requiring user input.
     *
     * Uses {@code System.in.available()} to detect input without blocking:
     * if the user has not pressed anything, the loop sleeps 50 ms and rechecks
     * if {@code gameView} has changed (automatic update) or if there is input.
     */
    private void runSpectatorMenu() {
        drainPendingNotices();
        GameDTO g = gameView;
        if (g == null) { sleep(100); return; }

        printSpectatorBoard(g);
        printSpectatorOptions();

        GameDTO snapshot = g;
        while (running) {
            if (isMyTurn()) return;
            GameDTO cur = gameView;
            if (cur != null && cur.phase() == GamePhase.GAME_OVER) return;

            // Auto-refresh: new snapshot from server → reprint board.
            // Debounce: when multiple ModelUpdates arrive within a short window
            // (e.g. placeTotem + setPhase during POSITIONING→ACTION_RESOLUTION),
            // wait briefly and pick up the latest snapshot, so we print once.
            if (cur != snapshot) {
                sleep(80);
                cur = gameView;
                snapshot = cur;
                if (cur != null && cur.phase() == GamePhase.GAME_OVER) return;
                if (cur != null && !isMyTurn()) {
                    System.out.println();
                    Printer.printInfo(">>> Board updated <<<");
                    printSpectatorBoard(cur);
                    printSpectatorOptions();
                } else {
                    return; // became my turn
                }
                continue;
            }

            // Non-blocking input: only consume if the user has actually
            // typed something. Empty ENTER → reprint board; Q → quit;
            // unknown → explicit feedback (no silent default).
            try {
                if (System.in.available() > 0) {
                    String raw = readLine();
                    if (raw == null) {
                        running = false;
                        return;
                    }
                    String line = raw.trim();
                    if (line.equalsIgnoreCase("Q") || line.equals("0")) {
                        Printer.printInfo("Quit requested by user.");
                        running = false;
                        return;
                    }
                    if (line.isEmpty() || line.equals("1")) {
                        // Refresh: just return so the main loop redraws.
                        return;
                    }
                    GameDTO current = gameView;
                    if (current == null) current = snapshot;
                    int choice;
                    try {
                        choice = Integer.parseInt(line);
                    } catch (NumberFormatException nfe) {
                        Printer.printError("Unrecognised input: '" + line
                                + "'. Use one of the listed options.");
                        printSpectatorOptions();
                        continue;
                    }
                    switch (choice) {
                        case 2 -> { GamePrinter.printRowDetails("UPPER", current.upperRowCards()); pressEnter(); }
                        case 3 -> { GamePrinter.printRowDetails("LOWER", current.lowerRowCards()); pressEnter(); }
                        case 4 -> { GamePrinter.printBuildingsDetails(current); pressEnter(); }
                        case 5 -> { GamePrinter.printTribe(current, getNickname()); pressEnter(); }
                        case 6 -> showOtherPlayerTribe();
                        default -> {
                            Printer.printError("Unknown option: " + choice + ".");
                            printSpectatorOptions();
                            continue;
                        }
                    }
                    return; // main loop calls runSpectatorMenu again (reprint board)
                }
            } catch (java.io.IOException ignored) {}

            sleep(50);
        }
    }

    /** Prints the spectator menu options. */
    private void printSpectatorOptions() {
        System.out.println();
        Printer.printInfo("The board updates automatically. Press ENTER (or 1) to reprint it.");
        System.out.println("  1) Refresh board");
        System.out.println("  2) See UPPER row cards");
        System.out.println("  3) See LOWER row cards");
        System.out.println("  4) See buildings");
        System.out.println("  5) See your tribe");
        System.out.println("  6) See another player's tribe");
        System.out.println("  0) Quit (or Q)");
        Printer.printPrompt("> ");
    }

    private void showOtherPlayerTribe() {
        GameDTO g = gameView;
        if (g == null) return;
        List<PlayerDTO> others = new ArrayList<>();
        for (PlayerDTO p : g.players()) {
            if (!p.nickname().equals(getNickname())) others.add(p);
        }
        if (others.isEmpty()) {
            Printer.printInfo("You are the only player.");
            return;
        }
        System.out.println();
        Printer.printInfo("Whose tribe do you want to see?");
        for (int i = 0; i < others.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + others.get(i).nickname());
        }
        System.out.println("  0) Back");
        int c = chooseInRange(0, others.size());
        if (c == 0) return;
        // Mask Inventor icons until GAME_OVER:
        // opponents must not learn other tribes' Inventor icons mid-game.
        GameDTO snap = gameView;
        boolean hideInventor = snap == null || snap.phase() != GamePhase.GAME_OVER;
        GamePrinter.printTribe(snap, others.get(c - 1).nickname(), hideInventor);
        pressEnter();
    }

    /** Prints the complete board in spectator mode. */
    private void printSpectatorBoard(GameDTO g) {
        Printer.printHeader("WAITING — turn of "
                + (g.currentPlayer() == null ? "?" : g.currentPlayer().nickname()));
        GamePrinter.printBoard(g, getNickname());
    }

    // ------------------------------------------------------------------
    // Update dispatcher: drain server queue -> update gameView
    // ------------------------------------------------------------------

    private void startUpdateDispatcher() {
        updateDispatcher = new Thread(this::dispatchLoop, "tui-dispatcher");
        updateDispatcher.setDaemon(true);
        updateDispatcher.start();
    }

    private void stopUpdateDispatcher() {
        if (updateDispatcher != null) updateDispatcher.interrupt();
    }

    private void dispatchLoop() {
        while (running) {
            NetworkMessage msg = waitForMessage();
            if (msg == null) {
                // waitForMessage returns null only on interrupt (shutdown)
                // or when the underlying message bus is closed. In the
                // former case `running` will already be false; in the
                // latter we surface a hint to the user instead of dying
                // silently.
                if (running) {
                    enqueueNotice(NoticeLevel.ERROR,
                            "Lost connection to server (message bus closed).");
                }
                return;
            }
            applyMessage(msg);
        }
    }

    /**
     * Applies a server message: updates {@code gameView} or prints errors.
     *
     * <p>Only updates state. Does NOT print the spectator board: that is the
     * sole responsibility of the UI thread inside {@code runSpectatorMenu()},
     * which polls {@code gameView} and reprints atomically. Printing here
     * (from the dispatcher thread) would interleave with the UI thread and
     * produce duplicate boards plus a missing options menu.</p>
     */
    private void applyMessage(NetworkMessage msg) {
        if (msg instanceof ModelUpdateMessage m) {
            GameDTO previous = gameView;
            GameDTO next = m.getGameModelDTO();
            setGameView(next);
            announceEraOrRoundTransitions(previous, next);

        } else if (msg instanceof GameOverMessage m) {
            setGameView(m.getFinalState());
            // run() detects GAME_OVER and terminates

        } else if (msg instanceof LobbyUpdateMessage m) {
            enqueueNotice(NoticeLevel.INFO, "Lobby: " + m.getCurrentPlayers()
                    + "/" + m.getExpectedPlayers() + " players");

        } else if (msg instanceof ErrorMessage m) {
            lastErrorMessageAt = System.currentTimeMillis();
            enqueueNotice(NoticeLevel.ERROR,
                    "[" + m.getError() + "] " + m.getErrorDetail());

        } else if (msg instanceof ConnectionUpdateMessage m) {
            String who = m.getNickname() == null ? "?" : m.getNickname();
            String text = m.isConnected()
                    ? who + " reconnected."
                    : who + " disconnected.";
            enqueueNotice(m.isConnected() ? NoticeLevel.INFO : NoticeLevel.ERROR, text);

        } else if (msg instanceof EventResolvedMessage m) {
            for (EventResolvedMessage.ResolvedEvent ev : m.getEvents()) {
                StringBuilder sb = new StringBuilder(">>> Event resolved: ");
                sb.append(ev.eventType());
                if (ev.isFinal()) sb.append(" [FINAL]");
                if (ev.deltas().isEmpty()) {
                    sb.append(" — no prestige changes.");
                } else {
                    sb.append(" — ");
                    for (int i = 0; i < ev.deltas().size(); i++) {
                        EventResolvedMessage.PlayerDelta d = ev.deltas().get(i);
                        if (i > 0) sb.append(", ");
                        sb.append(d.nickname())
                                .append(d.prestigeDelta() >= 0 ? " +" : " ")
                                .append(d.prestigeDelta()).append("PP");
                    }
                }
                enqueueNotice(NoticeLevel.INFO, sb.toString());
            }

        } else if (msg instanceof PhaseUpdateMessage || msg instanceof TurnUpdateMessage) {
            // Informational: next ModelUpdateMessage will bring the state.
        }
    }

    /**
     * Detects era and round transitions by comparing two consecutive
     * snapshots, and enqueues an info notice for the UI thread to print.
     *
     * <ul>
     *   <li>Era change (e.g. ERA_II → ERA_III): explains that Buildings in
     *       the lower row are discarded / shifted up at the start of the
     *       new era.</li>
     *   <li>10th round: announces the final round and the upcoming
     *       end-of-game scoring (visible Events, final pick, leaderboard).</li>
     * </ul>
     */
    private void announceEraOrRoundTransitions(GameDTO previous, GameDTO next) {
        if (next == null) return;
        if (previous != null && previous.currentEra() != next.currentEra()
                && next.currentEra() != null) {
            enqueueNotice(NoticeLevel.INFO,
                    ">>> New era started: " + next.currentEra().name().replace("ERA_", "ERA ")
                            + ". Buildings on the board are reorganised; "
                            + "check the BUILD section for the new layout.");
        }
        if ((previous == null || previous.currentRound() != 10)
                && next.currentRound() == 10) {
            enqueueNotice(NoticeLevel.INFO,
                    ">>> Final round (10/10). At the end of this round the remaining "
                            + "visible Events resolve and the game proceeds to final "
                            + "scoring.");
        }
    }

    // ------------------------------------------------------------------
    // Helper: turn, state
    // ------------------------------------------------------------------


    /** Light polling until my next turn or game ends. Q + ENTER to quit. */
    private void waitForMyTurn() {
        Printer.printInfo("Waiting for my next turn… (type Q + ENTER to quit)");
        while (running) {
            drainPendingNotices();
            if (checkQuitRequested()) {
                running = false;
                return;
            }
            GameDTO g = gameView;
            if (g == null) { sleep(150); continue; }
            if (g.phase() == GamePhase.GAME_OVER) return;
            if (isMyTurn() && (g.phase() == GamePhase.POSITIONING
                    || g.phase() == GamePhase.ACTION_RESOLUTION)) return;
            sleep(150);
        }
    }

    /** Hard cap on how long we wait for the server to acknowledge an action. */
    private static final long SEND_TIMEOUT_MS = 3000;

    /**
     * Implementation of method sendAndAwaitUpdate described in the class-level comment.
     */
    public SendResult sendAndAwaitUpdate(Object action) {
        notifyAllListeners(action);
        // Snapshots taken AFTER notify so that errors from earlier actions
        // (already enqueued before this call) do not look "fresh" to us.
        GameDTO before = gameView;
        long errorBefore = lastErrorMessageAt;
        long deadline = System.currentTimeMillis() + SEND_TIMEOUT_MS;
        while (running && System.currentTimeMillis() < deadline) {
            if (gameView != before) return SendResult.OK;
            if (lastErrorMessageAt != errorBefore) return SendResult.ERROR;
            sleep(50);
        }
        Printer.printError("Server did not respond within "
                + (SEND_TIMEOUT_MS / 1000) + "s. Action may not have been applied.");
        return SendResult.TIMEOUT;
    }

    /** Retrieves my playerId from the current snapshot. -1 if unknown. */
    private int myPlayerId() {
        int id = getPlayerId();
        // If I already have a real id assigned by the server (1,2,3,...) use it immediately
        if (id > 0) return id;

        // Defensive fallback for compatibility with old flows that don't set playerId.
        GameDTO g = gameView;
        if (g == null || getNickname() == null) return -1;
        for (PlayerDTO p : g.players()) {
            if (getNickname().equals(p.nickname())) return p.playerId();
        }
        return -1;
    }

    private static List<OfferTileDTO> freeTiles(GameDTO g) {
        List<OfferTileDTO> out = new ArrayList<>();
        if (g == null || g.offerTrack() == null) return out;
        int n = g.numberOfPlayers();
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.playerIndexOccupant() < 0 && validForPlayerCount(t.id(), n)) {
                out.add(t);
            }
        }
        return out;
    }

    private static boolean validForPlayerCount(OffertileId id, int playerCount) {
        return switch (id) {
            case A -> playerCount >= 5;
            case D -> playerCount >= 3;
            case G -> playerCount >= 4;
            default -> true;
        };
    }

    // ------------------------------------------------------------------
    // Helper: printing / input
    // ------------------------------------------------------------------

    private void printGameOver(GameDTO g) {
        Printer.printHeader("GAME OVER");
        GamePrinter.printLeaderboard(g.players(), null, getNickname());
        System.out.println();
        Printer.printSuccess("Game concluded. Press ENTER to exit.");
        readLine();
    }

    /**
     * Reads an integer in {@code [min, max]} inclusive, repeating on error.
     *
     * <p>Special inputs:
     * <ul>
     *   <li><b>EOF</b> ({@code readLine() == null}): sets {@code running}
     *       to false and returns {@code min} as a sentinel — prevents an
     *       infinite error-print loop when stdin is closed.</li>
     *   <li><b>Q / q</b>: triggers a graceful quit (same as Ctrl-D),
     *       returning {@code min}. Always available, even when the menu
     *       does not list it explicitly.</li>
     * </ul>
     */
    private int chooseInRange(int min, int max) {
        while (running) {
            Printer.printPrompt("> ");
            String raw = readLine();
            if (raw == null) {
                // EOF: stdin is closed. Don't busy-loop printing errors.
                Printer.printInfo("Input stream closed. Exiting.");
                running = false;
                return min;
            }
            String line = raw.trim();
            if (line.equalsIgnoreCase("Q")) {
                Printer.printInfo("Quit requested by user.");
                running = false;
                return min;
            }
            try {
                int v = Integer.parseInt(line);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) {
                // fall through to error message below
            }
            Printer.printError("Please enter a number between " + min
                    + " and " + max + " (or Q to quit).");
        }
        return min;
    }

    private int readInt(int fallback) {
        String line = readLine();
        if (line == null) return fallback;
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String readLine() {
        try {
            if (!in.hasNextLine()) return null;
            return in.nextLine();
        } catch (Exception e) {
            return null;
        }
    }

    private void pressEnter() {
        Printer.printPrompt("Press ENTER to continue…");
        readLine();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
