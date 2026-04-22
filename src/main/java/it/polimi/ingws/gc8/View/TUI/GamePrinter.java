package it.polimi.ingws.gc8.View.TUI;

import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;

import java.util.List;
import java.util.Map;

/**
 * Classe di utilità (solo metodi statici) per stampare lo stato di Mesos
 * in modalità testuale.
 *
 * Non conosce Scanner né interazione: ogni metodo traduce un pezzo di
 * {@link GameDTO} in output su {@code System.out}. La {@link TuiView}
 * compone questi mattoni per costruire le schermate.
 *
 * <h2>Icone offer tile</h2>
 * Il contenuto delle sette offer tile (A..G) è fisso dal regolamento, quindi
 * le icone sono hardcoded qui e non lette a run-time da {@code offer_tiles.json}:
 * il client deve funzionare anche se non ha accesso alle risorse server.
 * <pre>
 *   A → F+3  (3 food reward, 5 giocatori)
 *   B → ↓    (1 pick lower)
 *   C → ↑    (1 pick upper)
 *   D → ↓↓   (2 pick lower, 3+ giocatori)
 *   E → ↓↑   (1 lower + 1 upper)
 *   F → ↑↑   (2 pick upper)
 *   G → ↓↑↑  (1 lower + 2 upper, 4+ giocatori)
 * </pre>
 */
public final class GamePrinter {

    private GamePrinter() { /* classe di utilità */ }

    /** Icone offer tile derivate dalle regole (cfr. {@code offer_tiles.json}). */
    private static final Map<OffertileId, String> TILE_ICON = Map.of(
            OffertileId.A, "F+3",
            OffertileId.B, "↓  ",
            OffertileId.C, "↑  ",
            OffertileId.D, "↓↓ ",
            OffertileId.E, "↓↑ ",
            OffertileId.F, "↑↑ ",
            OffertileId.G, "↓↑↑"
    );

    // ------------------------------------------------------------------
    // Schermate complete
    // ------------------------------------------------------------------

    /** Stampa completa della board: header + righe carte + offer track + classifica. */
    public static void printBoard(GameDTO g, String myNickname) {
        if (g == null) {
            Printer.printInfo("(in attesa di stato dal server…)");
            return;
        }
        printHeader(g);
        printCardRow("UPPER", g.upperRowCards());
        printCardRow("LOWER", g.lowerRowCards());
        printOfferTrack(g.offerTrack(), g.players());
        printLeaderboard(g.players(), g.currentPlayer(), myNickname);
    }

    /** Header con era, round, fase, turno corrente. */
    public static void printHeader(GameDTO g) {
        String era   = (g.currentEra() == null) ? "?" : g.currentEra().name().replace("ERA_", "");
        String phase = (g.phase() == null) ? "?" : g.phase().name();
        String cur   = (g.currentPlayer() == null) ? "-" : g.currentPlayer().nickname();
        System.out.println();
        System.out.println(Printer.BOLD + Printer.YELLOW
                + "ERA " + era
                + "   |   Round " + g.currentRound()
                + "   |   Fase " + phase
                + "   |   Turno di: " + cur
                + Printer.RESET);
        Printer.printSeparator(70);
    }

    // ------------------------------------------------------------------
    // Mattoni
    // ------------------------------------------------------------------

    /** Stampa una riga di carte (upper o lower) con etichetta. */
    public static void printCardRow(String label, List<CardDTO> cards) {
        System.out.print(Printer.BOLD + pad(label, 6) + Printer.RESET + " | ");
        if (cards == null || cards.isEmpty()) {
            System.out.println(Printer.DIM + "(vuota)" + Printer.RESET);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            CardDTO c = cards.get(i);
            String face = c.isFlipped() ? c.back() : c.front();
            sb.append('[').append(i + 1).append("] ").append(face).append("   ");
        }
        System.out.println(sb.toString());
    }

    /** Stampa l'offer track come sequenza di tile con icona + colore occupante. */
    public static void printOfferTrack(List<OfferTileDTO> tiles, List<PlayerDTO> players) {
        System.out.print(Printer.BOLD + pad("OFFER", 6) + Printer.RESET + " | ");
        if (tiles == null || tiles.isEmpty()) {
            System.out.println(Printer.DIM + "(vuoto)" + Printer.RESET);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (OfferTileDTO t : tiles) {
            String icon  = TILE_ICON.getOrDefault(t.id(), "?  ");
            PlayerDTO occ = resolveOccupant(t.playerIndexOccupant(), players);
            String owner = (occ == null) ? "   " : pad(occ.totemColor().name(), 3);
            String color = (occ == null) ? Printer.DIM : ansiFor(occ.totemColor());
            sb.append('[')
                    .append(t.id())
                    .append(':').append(icon)
                    .append(' ').append(color).append(owner).append(Printer.RESET)
                    .append("] ");
        }
        System.out.println(sb.toString());
    }

    /** Classifica giocatori: freccia sul corrente, stella sul "me". */
    public static void printLeaderboard(List<PlayerDTO> players, PlayerDTO current, String myNickname) {
        System.out.println();
        System.out.println(Printer.BOLD + "GIOCATORI" + Printer.RESET);
        if (players == null || players.isEmpty()) {
            System.out.println(Printer.DIM + "  (nessuno)" + Printer.RESET);
            return;
        }
        for (PlayerDTO p : players) {
            boolean isCurrent = (current != null && current.playerId() == p.playerId());
            boolean isMe      = (myNickname != null && myNickname.equals(p.nickname()));
            String mark = (isCurrent ? "▶ " : "  ") + (isMe ? "* " : "  ");
            System.out.println("  " + mark
                    + ansiFor(p.totemColor()) + pad(p.nickname(), 14) + Printer.RESET
                    + " " + pad(p.totemColor().name(), 7)
                    + "  F:" + p.foodPoints()
                    + "   PP:" + p.prestigePoints());
        }
    }

    /** Stampa la tribù (mano di carte) del giocatore con il nickname indicato. */
    public static void printTribe(GameDTO g, String nickname) {
        PlayerDTO p = findByNickname(g, nickname);
        if (p == null) {
            Printer.printError("Giocatore non trovato: " + nickname);
            return;
        }
        System.out.println();
        System.out.println(Printer.BOLD + "TRIBÙ di " + p.nickname()
                + " (" + ansiFor(p.totemColor()) + p.totemColor().name() + Printer.RESET + ")" + Printer.RESET);
        List<CardDTO> tribe = p.tribe();
        if (tribe == null || tribe.isEmpty()) {
            System.out.println(Printer.DIM + "  (nessuna carta)" + Printer.RESET);
            return;
        }
        for (int i = 0; i < tribe.size(); i++) {
            CardDTO c = tribe.get(i);
            String face = c.isFlipped() ? c.back() : c.front();
            System.out.println("  [" + (i + 1) + "] " + face
                    + "   " + Printer.DIM + "(" + c.type() + ")" + Printer.RESET);
        }
    }

    /** Accesso pubblico alla mappa icone (per eventuali menu). */
    public static String iconFor(OffertileId id) {
        return TILE_ICON.getOrDefault(id, "?  ");
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private static PlayerDTO resolveOccupant(int idx, List<PlayerDTO> players) {
        if (idx < 0 || players == null || idx >= players.size()) return null;
        return players.get(idx);
    }

    private static PlayerDTO findByNickname(GameDTO g, String nickname) {
        if (g == null || nickname == null) return null;
        for (PlayerDTO p : g.players()) {
            if (nickname.equals(p.nickname())) return p;
        }
        return null;
    }

    /** Mappa TotemColor → sequenza ANSI di foreground. */
    private static String ansiFor(TotemColor color) {
        if (color == null) return Printer.RESET;
        return switch (color) {
            case RED    -> Printer.RED;
            case BLUE   -> Printer.BLUE;
            case YELLOW -> Printer.YELLOW;
            case WHITE  -> Printer.WHITE;
            case BLACK  -> Printer.DIM; // BLACK puro è invisibile su terminale scuro
        };
    }

    private static String pad(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}
