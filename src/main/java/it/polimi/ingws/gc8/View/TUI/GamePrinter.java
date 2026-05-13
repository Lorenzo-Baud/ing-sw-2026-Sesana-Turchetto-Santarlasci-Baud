package it.polimi.ingws.gc8.View.TUI;

import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class (static methods only) to print the state of Mesos
 * in text mode.
 *
 * Unknown Scanner or interaction: every method translates a piece of
 * {@link GameDTO} into output to {@code System.out}. {@link TuiView}
 * composes these building blocks to construct the screens.
 *
 * <h2>Offer tile icons</h2>
 * The content of the seven offer tiles (A..G) is fixed by the rules, so
 * the icons are hardcoded here and not read at runtime from {@code offer_tiles.json}:
 * the client must work even without access to server resources.
 * <pre>
 *   A → F+3  (3 food reward, 5 players)
 *   B → ↓    (1 pick lower)
 *   C → ↑    (1 pick upper)
 *   D → ↓↓   (2 pick lower, 3+ players)
 *   E → ↓↑   (1 lower + 1 upper)
 *   F → ↑↑   (2 pick upper)
 *   G → ↓↑↑  (1 lower + 2 upper, 4+ players)
 * </pre>
 */
public final class GamePrinter {

    private GamePrinter() { /* utility class */ }

    /** Offer tile icons derived from the rules (cfr. {@code offer_tiles.json}). */
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
    // Complete screens
    // ------------------------------------------------------------------

    /**
     * Complete board printout.
     * <p>
     * Layout logic (mirrors the physical board): tribe cards
     * (CHARACTER + EVENT) occupy UPPER and LOWER on the left, where LOWER
     * contains what was in UPPER in the previous round. Buildings
     * (BUILDING), which the model keeps mixed in the same lists, are
     * filtered and shown separately in the BUILDING section (on the right).
     * </p>
     */
    public static void printBoard(GameDTO g, String myNickname) {
        if (g == null) {
            Printer.printInfo("(waiting for state from server…)");
            return;
        }
        printHeader(g);
        printCardRow("UPPER", filterTribe(g.upperRowCards()));
        printOfferTrack(g.offerTrack(), g.players());
        printCardRow("LOWER", filterTribe(g.lowerRowCards()));
        printBuildings(collectBuildings(g.upperRowCards(), g.lowerRowCards()));
        printTurnOrderTrack(g.turnOrderTrack());
        printLeaderboard(g.players(), g.currentPlayer(), myNickname);
    }

    /**
     * Prints the Turn Order track: the sequence of players (1st = leftmost,
     * acts first next round). Drawing this is essential — without it the
     * player cannot reason about who will move next or whether they will
     * pay the food cost on the last slot of tile A.
     */
    public static void printTurnOrderTrack(List<PlayerDTO> order) {
        System.out.print(Printer.BOLD + pad("ORDER", 6) + Printer.RESET + " | ");
        if (order == null || order.isEmpty()) {
            System.out.println(Printer.DIM + "(empty)" + Printer.RESET);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            PlayerDTO p = order.get(i);
            if (p == null) {
                sb.append('[').append(i + 1).append(":-] ");
                continue;
            }
            sb.append('[')
                    .append(i + 1).append(':')
                    .append(ansiFor(p.totemColor()))
                    .append(pad(p.nickname(), 6))
                    .append(Printer.RESET)
                    .append("] ");
        }
        System.out.println(sb.toString());
    }

    /** Header with era, round, phase, current turn. */
    public static void printHeader(GameDTO g) {
        String era   = (g.currentEra() == null) ? "?" : g.currentEra().name().replace("ERA_", "");
        String phase = (g.phase() == null) ? "?" : g.phase().name();
        String cur   = (g.currentPlayer() == null) ? "-" : g.currentPlayer().nickname();
        System.out.println();
        System.out.println(Printer.BOLD + Printer.YELLOW
                + "ERA " + era
                + "   |   Round " + g.currentRound()
                + "   |   Phase " + phase
                + "   |   Turn of: " + cur
                + Printer.RESET);
        Printer.printSeparator(70);
    }

    // ------------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------------

    /** Prints a row of cards (upper or lower) with label. */
    public static void printCardRow(String label, List<CardDTO> cards) {
        System.out.print(Printer.BOLD + pad(label, 6) + Printer.RESET + " | ");
        if (cards == null || cards.isEmpty()) {
            System.out.println(Printer.DIM + "(empty)" + Printer.RESET);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            sb.append('[').append(i + 1).append("] ").append(shortLabel(cards.get(i))).append("   ");
        }
        System.out.println(sb.toString());
    }

    /**
     * Prints the buildings section (on the right of the board) as a compact list
     * of identifiers only (e.g. {@code E1_01}). For details use
     * {@link #printBuildingsDetails(GameDTO)}.
     */
    public static void printBuildings(List<CardDTO> buildings) {
        System.out.print(Printer.BOLD + pad("BUILD", 6) + Printer.RESET + " | ");
        if (buildings == null || buildings.isEmpty()) {
            System.out.println(Printer.DIM + "(no buildings)" + Printer.RESET);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buildings.size(); i++) {
            sb.append('[').append(i + 1).append("] ").append(shortLabel(buildings.get(i))).append("   ");
        }
        System.out.println(sb.toString());
    }

    /** Prints the offer track as a sequence of tiles with icon + nickname/color occupant. */
    public static void printOfferTrack(List<OfferTileDTO> tiles, List<PlayerDTO> players) {
        System.out.print(Printer.BOLD + pad("OFFER", 6) + Printer.RESET + " | ");
        if (tiles == null || tiles.isEmpty()) {
            System.out.println(Printer.DIM + "(empty)" + Printer.RESET);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (OfferTileDTO t : tiles) {
            String icon  = TILE_ICON.getOrDefault(t.id(), "?  ");
            PlayerDTO occ = resolveOccupant(t.playerIndexOccupant(), players);
            String owner = (occ == null)
                    ? "-"
                    : pad(occ.nickname(), 6);
            String color = (occ == null) ? Printer.DIM : ansiFor(occ.totemColor());
            sb.append('[')
                    .append(t.id())
                    .append(':').append(icon)
                    .append(' ').append(color).append(owner).append(Printer.RESET)
                    .append("] ");
        }
        System.out.println(sb.toString());
    }

    /** Players ranking: arrow on current, star on "me". */
    public static void printLeaderboard(List<PlayerDTO> players, PlayerDTO current, String myNickname) {
        System.out.println();
        System.out.println(Printer.BOLD + "PLAYERS" + Printer.RESET);
        if (players == null || players.isEmpty()) {
            System.out.println(Printer.DIM + "  (none)" + Printer.RESET);
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

    /** Prints the tribe (hand of cards) of the player with the given nickname. */
    public static void printTribe(GameDTO g, String nickname) {
        printTribe(g, nickname, false);
    }

    /**
     * Prints a player's tribe, with optional masking of the Inventor icon.
     *
     * <p>CLAUDE.md §11.2 / §11.4 mandate that the Inventor icon must remain
     * hidden from other players until the final scoring (i.e. GAME_OVER).
     * Pass {@code hideInventorIcon = true} when displaying an opponent's
     * tribe before the end of the game.</p>
     */
    public static void printTribe(GameDTO g, String nickname, boolean hideInventorIcon) {
        PlayerDTO p = findByNickname(g, nickname);
        if (p == null) {
            Printer.printError("Player not found: " + nickname);
            return;
        }
        System.out.println();
        System.out.println(Printer.BOLD + "TRIBE of " + p.nickname()
                + " (" + ansiFor(p.totemColor()) + p.totemColor().name() + Printer.RESET + ")" + Printer.RESET);
        List<CardDTO> tribe = p.tribe();
        if (tribe == null || tribe.isEmpty()) {
            System.out.println(Printer.DIM + "  (no cards)" + Printer.RESET);
            return;
        }
        for (int i = 0; i < tribe.size(); i++) {
            System.out.println("  [" + (i + 1) + "] "
                    + formatCardAttributes(tribe.get(i), hideInventorIcon));
        }
    }

    /**
     * Detailed view of a row (UPPER or LOWER).
     * Filters BUILDING: in upper/lower rows only tribe cards are shown
     * (CHARACTER + EVENT). For building details use
     * {@link #printBuildingsDetails(GameDTO)}.
     */
    public static void printRowDetails(String label, List<CardDTO> cards) {
        List<CardDTO> tribe = filterTribe(cards);
        System.out.println();
        System.out.println(Printer.BOLD + "ROW " + label + Printer.RESET);
        if (tribe.isEmpty()) {
            System.out.println(Printer.DIM + "  (empty)" + Printer.RESET);
            return;
        }
        for (int i = 0; i < tribe.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + formatCardAttributes(tribe.get(i)));
        }
    }

    /** Detailed view of board buildings (aggregated from upper+lower). */
    public static void printBuildingsDetails(GameDTO g) {
        System.out.println();
        System.out.println(Printer.BOLD + "BUILDINGS" + Printer.RESET);
        if (g == null) {
            System.out.println(Printer.DIM + "  (empty)" + Printer.RESET);
            return;
        }
        List<CardDTO> buildings = collectBuildings(g.upperRowCards(), g.lowerRowCards());
        if (buildings.isEmpty()) {
            System.out.println(Printer.DIM + "  (no buildings)" + Printer.RESET);
            return;
        }
        for (int i = 0; i < buildings.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + formatBuilding(buildings.get(i)));
        }
    }

    /** Public access to icon map (for menu purposes). */
    public static String iconFor(OffertileId id) {
        return TILE_ICON.getOrDefault(id, "?  ");
    }

    // ------------------------------------------------------------------
    // Card formatting
    // ------------------------------------------------------------------

    /** Complete description of a card based on its DTO attributes. */
    public static String formatCardAttributes(CardDTO c) {
        return formatCardAttributes(c, false);
    }

    /**
     * Same as {@link #formatCardAttributes(CardDTO)} but with the option to
     * mask hidden information (currently: the Inventor icon).
     */
    public static String formatCardAttributes(CardDTO c, boolean hideInventorIcon) {
        if (c.type() == null) return "?";
        return switch (c.type()) {
            case CHARACTER -> formatCharacter(c, hideInventorIcon);
            case BUILDING  -> formatBuilding(c);
            case EVENT     -> formatEvent(c);
        };
    }

    /** Short label for the board summary row. */
    private static String shortLabel(CardDTO c) {
        if (c.type() == null) return "?";
        return switch (c.type()) {
            case CHARACTER -> c.characterType() != null ? c.characterType().name() : "CHAR";
            case BUILDING  -> c.buildingId()    != null ? c.buildingId()           : "BLDG";
            case EVENT     -> c.eventType()     != null ? c.eventType().name()      : "EVT";
        };
    }

    private static String formatCharacter(CardDTO c, boolean hideInventorIcon) {
        String base = pad(c.characterType() != null ? c.characterType().name() : "?", 10)
                + "  min:" + c.numberOfPlayers() + "p";
        if (c.shamanIcons()           != null) return base + "  icons:" + c.shamanIcons();
        if (c.hunterBonus()           != null) return base + "  bonus:" + (c.hunterBonus() ? "yes" : "no");
        if (c.builderFoodBonus()      != null) return base + "  +F:" + c.builderFoodBonus() + "  PP:" + c.builderPrestigePoints();
        if (c.collectorFoodDiscount() != null) return base + "  -F:" + c.collectorFoodDiscount();
        if (c.inventorIcon()          != null) {
            return base + (hideInventorIcon ? "  icon#?" : "  icon#" + c.inventorIcon());
        }
        return base;
    }

    private static String formatBuilding(CardDTO c) {
        return pad(c.buildingId() != null ? c.buildingId() : "?", 6)
                + "  cost:" + c.buildingFoodCost() + "F"
                + "  +" + c.buildingPrestigeBonus() + "PP"
                + (c.buildingEffect() != null ? "  " + c.buildingEffect() : "");
    }

    private static String formatEvent(CardDTO c) {
        StringBuilder sb = new StringBuilder(
                pad(c.eventType() != null ? c.eventType().name() : "?", 13));
        if (Boolean.TRUE.equals(c.eventIsFinal()))  sb.append(" [FINAL]");
        if (c.eventPrestigeBonus() != null) sb.append("  +PP:").append(c.eventPrestigeBonus());
        if (c.eventPrestigeMalus() != null) sb.append("  -PP:").append(c.eventPrestigeMalus());
        if (c.eventBonusArtists()  != null) sb.append("  bonus>=").append(c.eventBonusArtists()).append("art");
        if (c.eventMalusArtists()  != null) sb.append("  malus<").append(c.eventMalusArtists()).append("art");
        if (c.eventBonusPoints()   != null) sb.append("  +").append(c.eventBonusPoints()).append("pp/art");
        if (c.eventMalusPoints()   != null) sb.append("  -").append(c.eventMalusPoints()).append("pp");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    /** Keeps only CHARACTER + EVENT (the "tribe" cards from upper/lower rows). */
    private static List<CardDTO> filterTribe(List<CardDTO> cards) {
        List<CardDTO> out = new ArrayList<>();
        if (cards == null) return out;
        for (CardDTO c : cards) {
            if (c != null && c.type() != CardType.BUILDING) out.add(c);
        }
        return out;
    }

    /** Collects BUILDING from upper+lower (on the board they occupy the section on the right). */
    private static List<CardDTO> collectBuildings(List<CardDTO> upper, List<CardDTO> lower) {
        List<CardDTO> out = new ArrayList<>();
        if (upper != null) for (CardDTO c : upper) if (c != null && c.type() == CardType.BUILDING) out.add(c);
        if (lower != null) for (CardDTO c : lower) if (c != null && c.type() == CardType.BUILDING) out.add(c);
        return out;
    }

    private static PlayerDTO resolveOccupant(int playerId, List<PlayerDTO> players) {
        if (playerId < 0 || players == null) return null;
        for (PlayerDTO p : players) {
            if (p != null && p.playerId() == playerId) return p;
        }
        return null;
    }

    private static PlayerDTO findByNickname(GameDTO g, String nickname) {
        if (g == null || nickname == null) return null;
        for (PlayerDTO p : g.players()) {
            if (nickname.equals(p.nickname())) return p;
        }
        return null;
    }

    /** Maps TotemColor → ANSI foreground sequence. */
    private static String ansiFor(TotemColor color) {
        if (color == null) return Printer.RESET;
        return switch (color) {
            case RED    -> Printer.RED;
            case BLUE   -> Printer.BLUE;
            case YELLOW -> Printer.YELLOW;
            case WHITE  -> Printer.WHITE;
            case BLACK  -> Printer.DIM; // Pure BLACK is invisible on dark terminal
        };
    }

    /**
     * Right-pads {@code s} with spaces up to {@code n} characters.
     * Strings longer than {@code n} are returned unchanged — never
     * truncated — so a long nickname spills into the next column rather
     * than appearing mutilated. Length policing belongs at the input
     * boundary (see {@code Bootstrap#askNickname} / {@code TuiView#askNickname}).
     */
    private static String pad(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}
