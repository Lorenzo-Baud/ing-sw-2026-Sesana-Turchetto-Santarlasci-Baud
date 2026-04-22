package it.polimi.ingws.gc8.View.TUI;

import it.polimi.ingws.gc8.Controller.actions.PlaceTotemAction;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.actions.ReturnTotemAction;
import it.polimi.ingws.gc8.Controller.actions.TakeCardAction;
import it.polimi.ingws.gc8.Controller.actions.TakeFoodAction;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
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
        notifyAllListeners(action);

        waitForMyTurn();
    }

    // ------------------------------------------------------------------
    // Fase ACTION_RESOLUTION: il giocatore risolve la propria offer tile
    // ------------------------------------------------------------------

    private void runResolutionPhase() {
        GameDTO g = gameView;
        if (g == null) return;

        Printer.printHeader("TUO TURNO — RISOLUZIONE AZIONI");
        GamePrinter.printBoard(g, getNickname());

        // Trova la tile su cui ho piazzato il totem: è quella occupata dal
        // giocatore di indice g.currentPlayerIndex() (siamo qui solo se è il
        // mio turno, quindi l'indice corrente È il mio).
        OfferTileDTO myTile = findMyTile(g);

        // Calcola le azioni permesse dalle regole della tile.
        // Se per qualche motivo non trovo la tile (stato inconsistente),
        // abilito tutto come fallback: sarà il server a rifiutare.
        boolean canUpper, canLower, canFood;
        String tileLabel;
        if (myTile == null) {
            canUpper = true; canLower = true; canFood = hasTileA(g);
            tileLabel = "?";
        } else {
            canUpper  = allowsUpperPick(myTile.id());
            canLower  = allowsLowerPick(myTile.id());
            canFood   = allowsFood(myTile.id());
            tileLabel = myTile.id() + " " + GamePrinter.iconFor(myTile.id());
        }

        System.out.println();
        Printer.printInfo("Sei sulla tile " + tileLabel + ". Cosa vuoi fare?");

        // Costruisco il menu in modo dinamico: ogni opzione mappa il numero
        // visualizzato al suo "tag" d'azione (UPPER/LOWER/FOOD/RETURN/BACK).
        List<String> tags = new ArrayList<>();
        int n = 1;
        if (canUpper) { System.out.println("  " + n++ + ") Prendi carta dalla riga UPPER"); tags.add("UPPER"); }
        if (canLower) { System.out.println("  " + n++ + ") Prendi carta dalla riga LOWER"); tags.add("LOWER"); }
        if (canFood)  { System.out.println("  " + n++ + ") Prendi food"); tags.add("FOOD"); }
        System.out.println("  " + n++ + ") Restituisci totem (chiudi turno)"); tags.add("RETURN");
        System.out.println("  0) Indietro");

        int choice = chooseInRange(0, tags.size());
        if (choice == 0) return;

        switch (tags.get(choice - 1)) {
            case "UPPER" -> takeCardFromRow(g.upperRowCards(), 1, "UPPER");
            case "LOWER" -> takeCardFromRow(g.lowerRowCards(), 0, "LOWER");
            case "FOOD"  -> {
                Printer.printSuccess("Prendo food…");
                notifyAllListeners(new TakeFoodAction(myPlayerId()));
                waitForMyTurn();
            }
            case "RETURN" -> {
                Printer.printSuccess("Restituisco il totem…");
                notifyAllListeners(new ReturnTotemAction(myPlayerId()));
                waitForMyTurn();
            }
        }
    }

    /** Cerca la tile occupata dal giocatore di turno. */
    private static OfferTileDTO findMyTile(GameDTO g) {
        if (g == null || g.offerTrack() == null) return null;
        int myIdx = g.currentPlayerIndex();
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.playerIndexOccupant() == myIdx) return t;
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

    /** True se l'offer track contiene la tile A (partita a 5 giocatori). */
    private static boolean hasTileA(GameDTO g) {
        if (g == null || g.offerTrack() == null) return false;
        for (OfferTileDTO t : g.offerTrack()) {
            if (t.id() == OffertileId.A) return true;
        }
        return false;
    }

    /** Sotto-menu: sceglie una carta da una delle due righe. */
    private void takeCardFromRow(List<CardDTO> cards, int row, String label) {
        if (cards == null || cards.isEmpty()) {
            Printer.printError("La riga " + label + " è vuota.");
            return;
        }
        System.out.println();
        Printer.printInfo("Carte disponibili nella riga " + label + ":");
        for (int i = 0; i < cards.size(); i++) {
            CardDTO c = cards.get(i);
            System.out.println("  " + (i + 1) + ") "
                    + (c.isFlipped() ? c.back() : c.front()));
        }
        System.out.println("  0) Indietro");

        int choice = chooseInRange(0, cards.size());
        if (choice == 0) return;

        CardDTO chosen = cards.get(choice - 1);
        String cardName = chosen.isFlipped() ? chosen.back() : chosen.front();
        Printer.printSuccess("Prendo: " + cardName);
        notifyAllListeners(new TakeCardAction(myPlayerId(), cardName, row));
        waitForMyTurn();
    }

    // ------------------------------------------------------------------
    // Spectator menu: non è il mio turno
    // ------------------------------------------------------------------

    private void runSpectatorMenu() {
        GameDTO g = gameView;
        if (g == null) { sleep(100); return; }

        Printer.printHeader("ATTESA — turno di "
                + (g.currentPlayer() == null ? "?" : g.currentPlayer().nickname()));
        GamePrinter.printBoard(g, getNickname());

        System.out.println();
        Printer.printInfo("Cosa vuoi fare mentre aspetti?");
        System.out.println("  1) Stampa di nuovo la board");
        System.out.println("  2) Vedi la tua tribù");
        System.out.println("  3) Vedi la tribù di un altro giocatore");
        System.out.println("  0) Attendi il tuo turno (blocca input)");

        int choice = chooseInRange(0, 3);
        switch (choice) {
            case 0 -> waitForMyTurn();
            case 1 -> { /* la prossima iterazione ristampa */ }
            case 2 -> { GamePrinter.printTribe(gameView, getNickname()); pressEnter(); }
            case 3 -> showOtherPlayerTribe();
        }
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
        if (msg instanceof ModelUpdateMessage m) {
            setGameView(m.getGameModelDTO());

        } else if (msg instanceof GameOverMessage m) {
            setGameView(m.getFinalState());
            // run() rileva GAME_OVER e termina

        } else if (msg instanceof LobbyUpdateMessage m) {
            Printer.printInfo("Lobby: " + m.getCurrentPlayers()
                    + "/" + m.getExpectedPlayers() + " giocatori");

        } else if (msg instanceof ErrorMessage m) {
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

    /** Recupera il mio playerId dalla snapshot corrente. -1 se non noto. */
    private int myPlayerId() {
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
