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
 * TUI procedurale (nessuna libreria esterna: solo {@code Scanner} + ANSI).
 *
 * <h2>Modello di thread</h2>
 * Due thread:
 * <ul>
 *   <li><b>main (thread UI)</b> — esegue {@link #run()}, stampa le schermate
 *       e legge da stdin. Attende il proprio turno facendo polling leggero
 *       su {@code gameView}.</li>
 *   <li><b>update-dispatcher</b> — daemon lanciato una volta sola, legge la
 *       coda {@code messages} (popolata dal network pump del ClientController)
 *       e applica gli aggiornamenti a {@code gameView} senza toccare stdin.
 *       Stampa errori a video, ignora le notifiche informative (phase/turn).</li>
 * </ul>
 *
 * <h2>Input</h2>
 * Tutto per numeri + INVIO; {@code 0} è sempre "indietro / salta".
 * Lo {@code Scanner} è usato in modo difensivo ({@code readInt}, {@code readLine})
 * per tollerare input malformato.
 *
 * <h2>Note Windows</h2>
 * Non gestiamo clear-screen né raw-mode: ogni ridisegno è un append su stdout,
 * compatibile con cmd.exe senza VT. ANSI escape sono usati solo per colori:
 * su terminali che non li supportano compaiono come sequenze rumorose ma la
 * TUI resta giocabile.
 */
public class TuiView extends View {

    private final Scanner in = new Scanner(System.in);

    /** Finché true il loop di {@link #run()} continua. */
    private volatile boolean running = true;

    /** Thread che drena la coda {@code messages} in background. */
    private Thread updateDispatcher;

    /** Timestamp dell'ultimo ErrorMessage ricevuto (epoch ms). */
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

        // Loop principale: ogni iterazione è un turno per il giocatore.
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
                    case GAME_OVER          -> { /* gestito sopra */ }
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
            Printer.printPrompt("Scegli il tuo nickname: ");
            String n = readLine();
            if (n != null && !n.isBlank()) {
                setNickname(n.trim());
                Printer.printSuccess("Ciao, " + getNickname() + "!");
            } else {
                Printer.printError("Nickname non valido.");
            }
        }
        // Quando esisterà una SetNicknameAction: notifyAllListeners(new SetNicknameAction(nickname));
    }

    /** Attende la prima snapshot di gioco, stampando gli aggiornamenti lobby. */
    private void waitForLobbyStart() {
        Printer.printInfo("In attesa di altri giocatori…");
        while (running && gameView == null) {
            sleep(200);
        }
    }

    // ------------------------------------------------------------------
    // Fase POSITIONING: il giocatore piazza il totem su una offer tile
    // ------------------------------------------------------------------

    private void runPositioningPhase() {
        GameDTO g = gameView;
        if (g == null) return;

        Printer.printHeader("TUO TURNO — POSIZIONAMENTO");
        GamePrinter.printBoard(g, getNickname());

        List<OfferTileDTO> free = freeTiles(g);
        if (free.isEmpty()) {
            Printer.printError("Nessuna offer tile disponibile.");
            waitForMyTurn();
            return;
        }

        System.out.println();
        Printer.printInfo("Scegli la offer tile su cui piazzare il totem:");
        for (int i = 0; i < free.size(); i++) {
            OfferTileDTO t = free.get(i);
            System.out.println("  " + (i + 1) + ") "
                    + t.id() + "  " + GamePrinter.iconFor(t.id()));
        }
        System.out.println("  0) Indietro / salta turno");

        int choice = chooseInRange(0, free.size());
        if (choice == 0) return;

        OfferTileDTO chosen = free.get(choice - 1);
        PlayerAction action = new PlaceTotemAction(myPlayerId(), chosen.id().name());
        Printer.printSuccess("Piazzo totem su tile " + chosen.id() + "…");
        sendAndAwaitUpdate(action);
    }

    // ------------------------------------------------------------------
    // Fase ACTION_RESOLUTION: il giocatore risolve la propria offer tile
    // ------------------------------------------------------------------

    private void runResolutionPhase() {
        GameDTO g = gameView;
        if (g == null) return;

        OfferTileDTO myTile = findMyTile(g);
        String tileLabel = myTile == null ? "?" : myTile.id() + " " + GamePrinter.iconFor(myTile.id());
        int maxUpper = myTile == null ? 1 : maxUpperPicks(myTile.id());
        int maxLower = myTile == null ? 1 : maxLowerPicks(myTile.id());
        boolean canFood = myTile != null && allowsFood(myTile.id());

        int upperTaken = 0;
        int lowerTaken = 0;
        boolean foodTaken = false;

        while (running && isMyTurn()) {
            g = gameView;
            if (g == null || g.phase() != GamePhase.ACTION_RESOLUTION) return;

            Printer.printHeader("TUO TURNO — RISOLUZIONE AZIONI");
            GamePrinter.printBoard(g, getNickname());

            System.out.println();
            Printer.printInfo("Sei sulla tile " + tileLabel + ". Cosa vuoi fare?");

            List<String> tags = new ArrayList<>();
            int n = 1;
            if (upperTaken < maxUpper) {
                System.out.println("  " + n++ + ") Prendi carta dalla riga UPPER (" + upperTaken + "/" + maxUpper + ")");
                tags.add("UPPER");
            }
            if (lowerTaken < maxLower) {
                System.out.println("  " + n++ + ") Prendi carta dalla riga LOWER (" + lowerTaken + "/" + maxLower + ")");
                tags.add("LOWER");
            }
            if (canFood && !foodTaken) {
                System.out.println("  " + n++ + ") Prendi food");
                tags.add("FOOD");
            }
            System.out.println("  " + n + ") Restituisci totem (chiudi turno)"); tags.add("RETURN");
            System.out.println("  0) Indietro");

            int choice = chooseInRange(0, tags.size());
            if (choice == 0) return;

            switch (tags.get(choice - 1)) {
                case "UPPER" -> { if (takeCardFromRow(g.upperRowCards(), CardRow.UPPER, "UPPER")) upperTaken++; }
                case "LOWER" -> { if (takeCardFromRow(g.lowerRowCards(), CardRow.LOWER, "LOWER")) lowerTaken++; }
                case "FOOD"  -> {
                    Printer.printSuccess("Prendo food…");
                    sendAndAwaitUpdate(new TakeFoodAction(myPlayerId()));
                    foodTaken = true;
                }
                case "RETURN" -> {
                    Printer.printSuccess("Restituisco il totem…");
                    sendAndAwaitUpdate(new ReturnTotemAction(myPlayerId()));
                    return;
                }
            }
        }
    }

    /** Cerca la tile occupata dal giocatore di turno. */
    private static OfferTileDTO findMyTile(GameDTO g) {
        if (g == null || g.offerTrack() == null) return null;
        if (g.currentPlayer() == null) return null;
        int currentPlayerId = g.currentPlayer().playerId();
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.playerIndexOccupant() == currentPlayerId) return t;
        }
        return null;
    }

    // Regole offer tile (cfr. offer_tiles.json):
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

    /** True se l'offer track contiene la tile A (partita a 5 giocatori). */
    private static boolean hasTileA(GameDTO g) {
        if (g == null || g.offerTrack() == null) return false;
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.id() == OffertileId.A) return true;
        }
        return false;
    }

    /** Sotto-menu: sceglie una carta da una delle due righe. Ritorna true se la carta è stata presa. */
    private boolean takeCardFromRow(List<CardDTO> cards, CardRow row, String label) {
        if (cards == null || cards.isEmpty()) {
            Printer.printError("La riga " + label + " è vuota.");
            return false;
        }
        System.out.println();
        Printer.printInfo("Carte disponibili nella riga " + label + ":");
        for (int i = 0; i < cards.size(); i++) {
            System.out.println("  " + (i + 1) + ") "
                    + GamePrinter.formatCardAttributes(cards.get(i)));
        }
        System.out.println("  0) Indietro");

        int choice = chooseInRange(0, cards.size());
        if (choice == 0) return false;

        int cardIndex = choice - 1;
        CardDTO chosen = cards.get(cardIndex);
        Printer.printSuccess("Prendo: " + GamePrinter.formatCardAttributes(chosen));
        sendAndAwaitUpdate(new TakeCardAction(myPlayerId(), cardIndex, row));
        return true;
    }

    // ------------------------------------------------------------------
    // Spectator menu: non è il mio turno
    // ------------------------------------------------------------------

    /**
     * Mostra la board e si aggiorna automaticamente ogni volta che il server
     * invia un nuovo snapshot, senza richiedere input all'utente.
     *
     * Usa {@code System.in.available()} per rilevare input senza bloccare:
     * se l'utente non ha premuto nulla, il loop dorme 50 ms e ricontrolla
     * se {@code gameView} è cambiato (update automatico) o se c'è input.
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

            // Auto-refresh: nuovo snapshot dal server → ristampa board immediatamente
            if (cur != snapshot) {
                snapshot = cur;
                if (cur != null && !isMyTurn()) {
                    System.out.println();
                    Printer.printInfo(">>> Board aggiornata <<<");
                    printSpectatorBoard(cur);
                    printSpectatorOptions();
                } else {
                    return; // è diventato il mio turno
                }
                continue;
            }

            // Input non-bloccante: legge solo se c'è già qualcosa nel buffer
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
                        default -> {} // 1, 0 o non valido: il return sotto ristampa la board
                    }
                    return; // il loop principale richiama runSpectatorMenu (ristampa la board)
                }
            } catch (java.io.IOException ignored) {}

            sleep(50);
        }
    }

    /** Stampa il menu opzioni dello spettatore. */
    private void printSpectatorOptions() {
        System.out.println();
        Printer.printInfo("La board si aggiorna automaticamente. Premi INVIO per ristamparla.");
        System.out.println("  2) Vedi le carte della riga UPPER");
        System.out.println("  3) Vedi le carte della riga LOWER");
        System.out.println("  4) Vedi gli edifici");
        System.out.println("  5) Vedi la tua tribù");
        System.out.println("  6) Vedi la tribù di un altro giocatore");
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
            Printer.printInfo("Sei l'unico giocatore.");
            return;
        }
        System.out.println();
        Printer.printInfo("Di chi vuoi vedere la tribù?");
        for (int i = 0; i < others.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + others.get(i).nickname());
        }
        System.out.println("  0) Indietro");
        int c = chooseInRange(0, others.size());
        if (c == 0) return;
        GamePrinter.printTribe(gameView, others.get(c - 1).nickname());
        pressEnter();
    }

    /** Stampa la board completa nella modalità spettatore. */
    private void printSpectatorBoard(GameDTO g) {
        Printer.printHeader("ATTESA — turno di "
                + (g.currentPlayer() == null ? "?" : g.currentPlayer().nickname()));
        GamePrinter.printBoard(g, getNickname());
    }

    // ------------------------------------------------------------------
    // Update dispatcher: drena la coda server -> aggiorna gameView
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

    /** Applica un messaggio server: aggiorna {@code gameView} o stampa errori. */
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
            // run() rileva GAME_OVER e termina

        } else if (msg instanceof LobbyUpdateMessage m) {
            Printer.printInfo("Lobby: " + m.getCurrentPlayers()
                    + "/" + m.getExpectedPlayers() + " giocatori");

        } else if (msg instanceof ErrorMessage m) {
            lastErrorMessageAt = System.currentTimeMillis();
            Printer.printError("[" + m.getError() + "] " + m.getErrorDetail());

        } else if (msg instanceof PhaseUpdateMessage || msg instanceof TurnUpdateMessage) {
            // Informativi: il prossimo ModelUpdateMessage porterà lo stato.
        }
    }

    // ------------------------------------------------------------------
    // Helper: turno, stato
    // ------------------------------------------------------------------

    private boolean isMyTurn() {
        GameDTO g = gameView;
        if (g == null || g.currentPlayer() == null || getNickname() == null) return false;
        return getNickname().equals(g.currentPlayer().nickname());
    }

    /** Polling leggero finché non torna il mio turno o il game termina. */
    private void waitForMyTurn() {
        Printer.printInfo("Attendo il mio prossimo turno…");
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
     * Invia un'azione al server e aspetta che arrivi un nuovo snapshot del
     * gameView (cioè la conferma che il server l'ha processata).
     *
     * <p>Senza questa attesa, dopo {@code notifyAllListeners} il loop principale
     * rivedrebbe il vecchio {@code gameView} (con il turno ancora del giocatore
     * locale) e ridisegnerebbe immediatamente il prompt prima che la risposta
     * del server arrivi — race condition tipica di chi spedisce e poi legge
     * stato condiviso senza barriera.</p>
     *
     * <p>Confronto per identità di riferimento: il dispatcher sostituisce
     * {@code gameView} con un nuovo {@code GameDTO} ad ogni
     * {@code ModelUpdateMessage}, quindi {@code gameView != before} è il segnale
     * che il server ha risposto.</p>
     *
     * <p>Timeout di 3 secondi per evitare hang in caso il server non risponda
     * (errore silenzioso, connessione persa, ecc.): scaduto il timeout il
     * controllo torna al loop principale, che ricalibrerà sullo stato attuale.</p>
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
                System.out.println("[DEBUG TuiView] sendAndAwaitUpdate EXIT (snapshot cambiato)"
                        + " currentPlayerAfter=" + (after == null || after.currentPlayer() == null ? "null" : after.currentPlayer().nickname()));
                return;
            }
            if (lastErrorMessageAt != errorBefore) {
                System.out.println("[DEBUG TuiView] sendAndAwaitUpdate EXIT (ricevuto ErrorMessage)");
                return;
            }
            sleep(50);
        }
        System.out.println("[DEBUG TuiView] sendAndAwaitUpdate EXIT (TIMEOUT 3s — il server non ha risposto)");
    }

    /** Recupera il mio playerId dalla snapshot corrente. -1 se non noto. */
    private int myPlayerId() {
        int id = getPlayerId();
        //se ho già un id reale assegnato dal server (1,2,3,...) usalo subito
        if (id > 0) return id;

        // Fallback difensivo per compatibilità con vecchi flussi che non impostano playerId.
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
    // Helper di stampa / input
    // ------------------------------------------------------------------

    private void printGameOver(GameDTO g) {
        Printer.printHeader("GAME OVER");
        GamePrinter.printLeaderboard(g.players(), null, getNickname());
        System.out.println();
        Printer.printSuccess("Partita conclusa. Premi INVIO per uscire.");
        readLine();
    }

    /** Legge un intero in {@code [min, max]} inclusi, ripetendo su errore. */
    private int chooseInRange(int min, int max) {
        while (true) {
            Printer.printPrompt("> ");
            int v = readInt(Integer.MIN_VALUE);
            if (v >= min && v <= max) return v;
            Printer.printError("Inserisci un numero tra " + min + " e " + max + ".");
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
        Printer.printPrompt("Premi INVIO per continuare…");
        readLine();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
