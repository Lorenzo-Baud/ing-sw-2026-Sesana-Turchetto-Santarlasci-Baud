package it.polimi.ingws.gc8.View.TUI;

import it.polimi.ingws.gc8.Controller.actions.PlaceTotemAction;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.actions.ReturnTotemAction;
import it.polimi.ingws.gc8.Controller.actions.TakeCardAction;
import it.polimi.ingws.gc8.Controller.actions.TakeFoodAction;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
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

    private void askNickname() {
        while (nickname == null || nickname.isBlank()) {
            Printer.printPrompt("Choose your nickname: ");
            String n = readLine();
            if (n != null && !n.isBlank()) {
                setNickname(n.trim());
                Printer.printSuccess("Hi, " + getNickname() + "!");
            } else {
                Printer.printError("Invalid nickname.");
            }
        }
        // When SetNicknameAction exists: notifyAllListeners(new SetNicknameAction(nickname));
    }

    /** Waits for the first game snapshot, printing lobby updates. */
    private void waitForLobbyStart() {
        Printer.printInfo("Waiting for other players…");
        while (running && gameView == null) {
            sleep(200);
        }
    }

    // ------------------------------------------------------------------
    // POSITIONING phase: player places totem on an offer tile
    // ------------------------------------------------------------------

    private void runPositioningPhase() {
        GameDTO g = gameView;
        if (g == null) return;

        Printer.printHeader("YOUR TURN — POSITIONING");
        GamePrinter.printBoard(g, getNickname());

        List<OfferTileDTO> free = freeTiles(g);
        if (free.isEmpty()) {
            Printer.printError("No offer tile available.");
            waitForMyTurn();
            return;
        }

        System.out.println();
        Printer.printInfo("Choose the offer tile on which to place your totem:");
        for (int i = 0; i < free.size(); i++) {
            OfferTileDTO t = free.get(i);
            System.out.println("  " + (i + 1) + ") "
                    + t.id() + "  " + GamePrinter.iconFor(t.id()));
        }
        System.out.println("  0) Back / skip turn");

        int choice = chooseInRange(0, free.size());
        if (choice == 0) return;

        OfferTileDTO chosen = free.get(choice - 1);
        PlayerAction action = new PlaceTotemAction(myPlayerId(), chosen.id().name());
        Printer.printSuccess("Placing totem on tile " + chosen.id() + "…");
        sendAndAwaitUpdate(action);
    }

    // ------------------------------------------------------------------
    // ACTION_RESOLUTION phase: player resolves their offer tile
    // ------------------------------------------------------------------

    private void runResolutionPhase() {
        GameDTO g = gameView;
        if (g == null) return;

        OfferTileDTO myTile = findMyTile(g);
        String tileLabel = myTile == null ? "?" : myTile.id() + " " + GamePrinter.iconFor(myTile.id());
        // Decision: maximum number of cards to take from UPPER and LOWER
        int maxUpper = myTile == null ? 1 : maxUpperPicks(myTile.id());
        int maxLower = myTile == null ? 1 : maxLowerPicks(myTile.id());
        boolean canFood = myTile != null && allowsFood(myTile.id());

        int upperTaken = 0;
        int lowerTaken = 0;
        boolean foodTaken = false;

        while (running && isMyTurn()) {
            g = gameView;
            if (g == null || g.phase() != GamePhase.ACTION_RESOLUTION) return;

            Printer.printHeader("YOUR TURN — ACTION RESOLUTION");
            GamePrinter.printBoard(g, getNickname());

            System.out.println();
            Printer.printInfo("You are on tile " + tileLabel + ". What do you want to do?");

            // Create action choices
            List<String> tags = new ArrayList<>();
            int n = 1;
            if (upperTaken < maxUpper) {
                System.out.println("  " + n++ + ") Take card from UPPER row (" + upperTaken + "/" + maxUpper + ")");
                tags.add("UPPER");
            }
            if (lowerTaken < maxLower) {
                System.out.println("  " + n++ + ") Take card from LOWER row (" + lowerTaken + "/" + maxLower + ")");
                tags.add("LOWER");
            }
            if (canFood && !foodTaken) {
                System.out.println("  " + n++ + ") Take food");
                tags.add("FOOD");
            }
            System.out.println("  " + n + ") Return totem (end turn)"); tags.add("RETURN");
            System.out.println("  0) Back");

            int choice = chooseInRange(0, tags.size());
            if (choice == 0) return;

            // Perform action based on choice
            switch (tags.get(choice - 1)) {
                case "UPPER" -> { if (takeCardFromRow(g.upperRowCards(), CardRow.UPPER, "UPPER")) upperTaken++; }
                case "LOWER" -> { if (takeCardFromRow(g.lowerRowCards(), CardRow.LOWER, "LOWER")) lowerTaken++; }
                case "FOOD"  -> {
                    Printer.printSuccess("Taking food…");
                    sendAndAwaitUpdate(new TakeFoodAction(myPlayerId()));
                    foodTaken = true;
                }
                case "RETURN" -> {
                    Printer.printSuccess("Returning totem…");
                    sendAndAwaitUpdate(new ReturnTotemAction(myPlayerId()));
                    return;
                }
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

    // Offer tile rules (cfr. offer_tiles.json):
    //   A → food+3 (no picks)
    //   B → 1 lower           D → 2 lower
    //   C → 1 upper           F → 2 upper
    //   E → 1 lower + 1 upper G → 1 lower + 2 upper
    private static boolean allowsUpperPick(OffertileId id) {
        return id == OffertileId.C || id == OffertileId.E
                || id == OffertileId.F || id == OffertileId.G;
    }
    private static boolean allowsLowerPick(OffertileId id) {
        return id == OffertileId.B || id == OffertileId.D
                || id == OffertileId.E || id == OffertileId.G;
    }
    private static boolean allowsFood(OffertileId id) {
        return id == OffertileId.A;
    }

    private static int maxUpperPicks(OffertileId id) {
        return switch (id) {
            case C -> 1;
            case E -> 1;
            case F -> 2;
            case G -> 2;
            default -> 0;
        };
    }

    private static int maxLowerPicks(OffertileId id) {
        return switch (id) {
            case B -> 1;
            case D -> 2;
            case E -> 1;
            case G -> 1;
            default -> 0;
        };
    }

    /** True if offer track contains tile A (5-player game). */
    private static boolean hasTileA(GameDTO g) {
        if (g == null || g.offerTrack() == null) return false;
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.id() == OffertileId.A) return true;
        }
        return false;
    }

    /** Sub-menu: choose a card from one of two rows. Returns true if the card was taken. */
    private boolean takeCardFromRow(List<CardDTO> cards, CardRow row, String label) {
        if (cards == null || cards.isEmpty()) {
            Printer.printError("Row " + label + " is empty.");
            return false;
        }
        System.out.println();
        Printer.printInfo("Available cards in row " + label + ":");
        for (int i = 0; i < cards.size(); i++) {
            System.out.println("  " + (i + 1) + ") "
                    + GamePrinter.formatCardAttributes(cards.get(i)));
        }
        System.out.println("  0) Back");

        int choice = chooseInRange(0, cards.size());
        if (choice == 0) return false;

        int cardIndex = choice - 1;
        CardDTO chosen = cards.get(cardIndex);
        Printer.printSuccess("Taking: " + GamePrinter.formatCardAttributes(chosen));
        sendAndAwaitUpdate(new TakeCardAction(myPlayerId(), cardIndex, row));
        return true;
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
        GameDTO g = gameView;
        if (g == null) { sleep(100); return; }

        printSpectatorBoard(g);
        printSpectatorOptions();

        GameDTO snapshot = g;
        while (running) {
            if (isMyTurn()) return;
            GameDTO cur = gameView;
            if (cur != null && cur.phase() == GamePhase.GAME_OVER) return;

            // Auto-refresh: new snapshot from server → reprint board immediately
            if (cur != snapshot) {
                snapshot = cur;
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

            // Non-blocking input: reads only if there's already something in the buffer
            try {
                if (System.in.available() > 0) {
                    int choice = readInt(-1);
                    GameDTO current = gameView;
                    if (current == null) current = snapshot;
                    switch (choice) {
                        case 2 -> { GamePrinter.printRowDetails("UPPER", current.upperRowCards()); pressEnter(); }
                        case 3 -> { GamePrinter.printRowDetails("LOWER", current.lowerRowCards()); pressEnter(); }
                        case 4 -> { GamePrinter.printBuildingsDetails(current); pressEnter(); }
                        case 5 -> { GamePrinter.printTribe(gameView, getNickname()); pressEnter(); }
                        case 6 -> showOtherPlayerTribe();
                        default -> {} // 1, 0 or invalid: return below reprints the board
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
        Printer.printInfo("The board updates automatically. Press ENTER to reprint it.");
        System.out.println("  2) See UPPER row cards");
        System.out.println("  3) See LOWER row cards");
        System.out.println("  4) See buildings");
        System.out.println("  5) See your tribe");
        System.out.println("  6) See another player's tribe");
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
        GamePrinter.printTribe(gameView, others.get(c - 1).nickname());
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
            if (msg == null) return;
            applyMessage(msg);
        }
    }

    /** Applies a server message: updates {@code gameView} or prints errors. */
    private void applyMessage(NetworkMessage msg) {
        System.out.println("[DEBUG TuiView] applyMessage msg=" + msg.getClass().getSimpleName());
        if (msg instanceof ModelUpdateMessage m) {
            GameDTO newView = m.getGameModelDTO();
            System.out.println("[DEBUG TuiView] applyMessage ModelUpdate"
                    + " currentPlayer=" + (newView == null || newView.currentPlayer() == null ? "null" : newView.currentPlayer().nickname())
                    + " phase=" + (newView == null ? "null" : newView.phase()));
            setGameView(newView);
            if (newView != null && !isMyTurn()) {
                printSpectatorBoard(newView);
            }

        } else if (msg instanceof GameOverMessage m) {
            setGameView(m.getFinalState());
            // run() detects GAME_OVER and terminates

        } else if (msg instanceof LobbyUpdateMessage m) {
            Printer.printInfo("Lobby: " + m.getCurrentPlayers()
                    + "/" + m.getExpectedPlayers() + " players");

        } else if (msg instanceof ErrorMessage m) {
            lastErrorMessageAt = System.currentTimeMillis();
            Printer.printError("[" + m.getError() + "] " + m.getErrorDetail());

        } else if (msg instanceof PhaseUpdateMessage || msg instanceof TurnUpdateMessage) {
            // Informational: next ModelUpdateMessage will bring the state.
        }
    }

    // ------------------------------------------------------------------
    // Helper: turn, state
    // ------------------------------------------------------------------

    private boolean isMyTurn() {
        GameDTO g = gameView;
        if (g == null || g.currentPlayer() == null || getNickname() == null) return false;
        return getNickname().equals(g.currentPlayer().nickname());
    }

    /** Light polling until my next turn or game ends. */
    private void waitForMyTurn() {
        Printer.printInfo("Waiting for my next turn…");
        while (running) {
            GameDTO g = gameView;
            if (g == null) { sleep(150); continue; }
            if (g.phase() == GamePhase.GAME_OVER) return;
            if (isMyTurn() && (g.phase() == GamePhase.POSITIONING
                    || g.phase() == GamePhase.ACTION_RESOLUTION)) return;
            sleep(150);
        }
    }

    /**
     * Sends an action to the server and waits for a new gameView snapshot
     * (i.e., confirmation that the server has processed it).
     *
     * <p>Without this wait, after {@code notifyAllListeners} the main loop
     * would see the old {@code gameView} (with the turn still belonging to the
     * local player) and redraw the prompt immediately before the server's response
     * arrives — typical race condition when sending and then reading shared state
     * without a barrier.</p>
     *
     * <p>Reference identity comparison: the dispatcher replaces
     * {@code gameView} with a new {@code GameDTO} on each
     * {@code ModelUpdateMessage}, so {@code gameView != before} signals
     * that the server has responded.</p>
     *
     * <p>3-second timeout to avoid hanging if the server doesn't respond
     * (silent error, connection lost, etc.): after timeout, control returns
     * to the main loop, which will recalibrate on the current state.</p>
     */
    private void sendAndAwaitUpdate(Object action) {
        GameDTO before = gameView;
        long errorBefore = lastErrorMessageAt;
        System.out.println("[DEBUG TuiView] sendAndAwaitUpdate ENTRY action=" + action.getClass().getSimpleName()
                + " currentPlayerBefore=" + (before == null || before.currentPlayer() == null ? "null" : before.currentPlayer().nickname())
                + " myPlayerId=" + myPlayerId());
        notifyAllListeners(action);
        long deadline = System.currentTimeMillis() + 3000;
        while (running && System.currentTimeMillis() < deadline) {
            if (gameView != before) {
                GameDTO after = gameView;
                System.out.println("[DEBUG TuiView] sendAndAwaitUpdate EXIT (snapshot changed)"
                        + " currentPlayerAfter=" + (after == null || after.currentPlayer() == null ? "null" : after.currentPlayer().nickname()));
                return;
            }
            if (lastErrorMessageAt != errorBefore) {
                System.out.println("[DEBUG TuiView] sendAndAwaitUpdate EXIT (received ErrorMessage)");
                return;
            }
            sleep(50);
        }
        System.out.println("[DEBUG TuiView] sendAndAwaitUpdate EXIT (TIMEOUT 3s — server did not respond)");
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
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.playerIndexOccupant() < 0) out.add(t);
        }
        return out;
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

    /** Reads an integer in {@code [min, max]} inclusive, repeating on error. */
    private int chooseInRange(int min, int max) {
        while (true) {
            Printer.printPrompt("> ");
            int v = readInt(Integer.MIN_VALUE);
            if (v >= min && v <= max) return v;
            Printer.printError("Please enter a number between " + min + " and " + max + ".");
        }
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
