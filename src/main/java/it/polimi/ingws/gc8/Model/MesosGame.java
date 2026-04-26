package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Board.Board;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Board.TurnOrderTrack;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.*;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Network.ViewUpdater;

import java.util.*;

/**
 * Concrete implementation of the MESOS game model.
 * Manages the game state and implements all game logic.
 */
public class MesosGame implements GameModel {

    private List<Player> players;
    private Board board;
    private int numPlayers;
    private int currentRound;
    private Era currentEra;
    private GamePhase phase;
    private int currentPlayerIndex;
    ViewUpdater viewUpdater;

    @Override
    public void notifyUpdate(GameModel model){
        if (viewUpdater != null) {
            viewUpdater.broadcastModel(model);
        }
    }

    @Override
    public void setCurrentPlayer(Player player) {
        this.currentPlayerIndex = this.players.indexOf(player);
        notifyUpdate(this);
    }


    public MesosGame(ViewUpdater viewUpdater) {
        this.players = new ArrayList<>();
        this.currentRound = 0;
        this.currentEra = Era.ERA_1;
        this.phase = GamePhase.SETUP;
        this.currentPlayerIndex = 0;
        this.board = new Board(3);
        this.viewUpdater = viewUpdater;
    }

    // SETUP
    @Override
    public void setupGame(int numPlayers) {
        if (numPlayers < 2 || numPlayers > 5) {
            throw new IllegalArgumentException(
                    "Number of players must be between 2 and 5 (got " + numPlayers + ").");
        }
        this.numPlayers = numPlayers;
        initializePlayers(numPlayers);
        initializeBoard(numPlayers);
        randomizeTurnOrder();
        distributeInitialFood();
        this.currentRound = 1;
        this.phase = GamePhase.POSITIONING;
        notifyUpdate(this);
    }


    /**
     * Creates Players with default names and sequential TotemColors.
     */
    private void initializePlayers(int numPlayers) {
        players.clear(); //remove eventual player left from a previews configuration
        TotemColor[] colors = TotemColor.values();
        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player(i + 1, "Player " + (i + 1), colors[i]));
        }
    }

    /**
     * Creates the Board for the given number of players.
     * <p>
     * The {@link Board} constructor already loads the tribe deck, splits the
     * building cards by era (storing Era II / Era III decks internally), and
     * deals the initial lower/upper rows according to the rules
     * (see {@code Board.dealInitialRows}). No additional setup is needed here.
     * </p>
     */
    private void initializeBoard(int numPlayers) {
        board = new Board(numPlayers);
    }

    /**
     * Randomly places Totems on the Turn Order track.
     */
    private void randomizeTurnOrder() {
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        TurnOrderTrack track = board.getTurnOrderTrack();
        for (Player player : shuffled) {
            track.placePlayer(player);
        }
    }

    /**
     * Distributes initial food based on turn-order position:
     * 1st: 2 food, 2nd–3rd: 3 food, 4th–5th: 4 food.
     */
    private void distributeInitialFood() {
        int[] foodByPosition = {2, 3, 3, 4, 4};
        List<Player> order = board.getTurnOrderTrack().getCurrentOrder();
        for (int i = 0; i < order.size(); i++) {
            Player player = order.get(i);
            if (player != null) {
                player.addFood(foodByPosition[i]);
            }
        }
    }

    // GETTERS
    @Override
    public int getNumPlayers() { return numPlayers; }

    @Override
    public int getCurrentRound() { return currentRound; }

    @Override
    public Era getCurrentEra() { return currentEra; }

    @Override
    public GamePhase getPhase() { return phase; }

    @Override
    public void setPhase(GamePhase phase) {
        this.phase = phase;
        notifyUpdate(this);
    }

    @Override
    public List<Player> getPlayers() { return players; }

    @Override
    public Board getBoard() { return board; }

    @Override
    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    // PHASE 1: TOTEM PLACEMENT
    @Override
    public List<Player> getPlayersInTurnOrder() {
        List<Player> order = new ArrayList<>();
        for (Player p : board.getTurnOrderTrack().getCurrentOrder()) {
            if (p != null) order.add(p);
        }
        return order;
    }

    @Override
    public List<OfferTile> getAvailableOfferTiles() {
        return board.getOfferTrack().getAvailableTiles();
    }

    /**
     * Returns the offer tile currently occupied by the given player,
     * or {@code null} if the player has not placed a totem.
     * @param player the player to look up
     * @return the offer tile occupied by the player, or null if none
     */
    public OfferTile getOfferTile(Player player) {
        if (player == null) return null;
        for (OfferTile tile : board.getOfferTrack().getTiles()) {
            Player occupant = tile.getOccupant();
            if (occupant != null && occupant.getPlayerId() == player.getPlayerId()) {
                return tile;
            }
        }
        return null;
    }

    @Override
    public void placeTotem(Player player, OfferTile offerTile) {
        board.getOfferTrack().placeTotem(offerTile.getId(), player);
        currentPlayerIndex = (currentPlayerIndex + 1) % numPlayers;
        if (allTotemsPlaced()) {
            phase = GamePhase.ACTION_RESOLUTION;
            currentPlayerIndex = 0;
        }
        notifyUpdate(this);
    }

    @Override
    public boolean allTotemsPlaced() {
        return board.getOfferTrack().isFull(numPlayers);
    }

    // PHASE 2: ACTION RESOLUTION
    @Override
    public List<Player> getPlayersInOfferOrder() {
        List<Player> order = new ArrayList<>();
        for (OfferTile tile : board.getOfferTrack().getResolutionOrder()) {
            order.add(tile.getOccupant());
        }
        return order;
    }

    /**
     * @param row selected board row
     * @return available (non-Event) cards in the requested row
     */
    @Override
    public List<Card> getAvailableCardsFromRow(CardRow row) {
        List<Card> source;
        switch (Objects.requireNonNull(row, "row")) {
            case LOWER:
                source = board.getLowerRow();
                break;
            case UPPER:
                source = board.getUpperRow();
                break;
            default:
                throw new IllegalArgumentException("Unsupported card row: " + row);
        }

        List<Card> available = new ArrayList<>();
        for (Card card : source) {
            if (!(card instanceof EventCard)) {
                available.add(card);
            }
        }
        return available;
    }

    @Override
    public boolean canPlayerAffordBuilding(Player player, BuildingCard building) {
        int discount = player.getTribe().getTotalBuilderDiscount();
        return building.getEffectiveCost(discount) <= player.getFood();
    }

    @Override
    public void takeCharacterCard(Player player, CharacterCard card) {
        board.getLowerRow().remove(card);
        board.getUpperRow().remove(card);
        player.addCharacterCard(card);
        notifyUpdate(this);
    }

    @Override
    public void takeBuildingCard(Player player, BuildingCard card) {
        int discount = player.getTribe().getTotalBuilderDiscount();
        player.removeFood(card.getEffectiveCost(discount));
        board.getLowerRow().remove(card);
        board.getUpperRow().remove(card);
        player.addBuildingCard(card);
        notifyUpdate(this);
    }

    @Override
    public void takeFood(Player player) {
        player.addFood(3);
        notifyUpdate(this);
    }

    @Override
    public void returnTotemToTurnOrder(Player player) {
        // Remove totem from the offer track
        for (OfferTile tile : board.getOfferTrack().getTiles()) {
            if (player.equals(tile.getOccupant())) {
                tile.removeTotem();
                break;
            }
        }

        // Place in the first free slot on the TurnOrderTrack
        TurnOrderTrack track = board.getTurnOrderTrack();
        int slotIndex = track.placePlayer(player);

        // Food bonus: buildings only add extra food if there is a base food bonus on the slot
        int baseBonus = track.getFoodBonusForSlot(slotIndex);
        if (baseBonus > 0) {
            player.addFood(baseBonus + player.getTribe().getTotalExtraFoodOnTurnOrder());
        }

        // Last slot penalty: pay 1 food, or lose PP if no food available
        if (track.isLastSlot(slotIndex)) {
            int foodPenalty = track.getLastSlotFoodPenalty();
            if (player.getFood() >= foodPenalty) {
                player.removeFood(foodPenalty);
            } else {
                // lastSlotPrestigePenalty is stored as a negative value in the JSON
                player.addPrestigePoints(track.getLastSlotPrestigePenalty());
            }
        }
        notifyUpdate(this);
    }

    @Override
    public boolean canPickExtraCard(Player player) {
        return player.getTribe().canPickExtraCard();
    }

    @Override
    public void takeExtraCard(Player player, Card card) {
        if (card instanceof BuildingCard) {
            takeBuildingCard(player, (BuildingCard) card);
        } else if (card instanceof CharacterCard) {
            takeCharacterCard(player, (CharacterCard) card);
        }
        notifyUpdate(this);
    }

    // END OF ROUND: EVENT RESOLUTION

    @Override
    public List<EventCard> getEventsToResolve() {
        List<EventCard> events = new ArrayList<>(board.getLowerRowEvents());

        // In the final round (tribe deck exhausted), also resolve upper row events
        if (isGameOver()) {
            for (Card card : board.getUpperRow()) {
                if (card instanceof EventCard) {
                    events.add((EventCard) card);
                }
            }
        }

        // Order: Sustenance always last; within the same type, order by Era
        events.sort(Comparator
                .comparing((EventCard e) -> e.getEventType() == EventType.sustenance ? 1 : 0)
                .thenComparing(e -> e.getEra().ordinal()));

        return events;
    }



    // END OF ROUND: ROW AND ERA MANAGEMENT

    @Override
    public void discardLowerRow() {
        board.discardLowerRowTribeCards();
        notifyUpdate(this);
    }

    @Override
    public void moveUpperRowToLower() {

        board.moveUpperRowTribeCardsToLower();
        notifyUpdate(this);
    }

    @Override
    public void refreshUpperRow() {
        for (int i = 0; i < numPlayers + 4; i++) {
            if (!board.getTribeDeck().isEmpty()) {
                board.getUpperRow().add(board.getTribeDeck().removeFirst());
            }
        }
        currentRound++;
        notifyUpdate(this);
    }

    @Override
    public boolean checkNewEra() {
        Era maxEraInRow = currentEra;
        for (Card card : board.getUpperRow()) {
            if (card.getEra().ordinal() > maxEraInRow.ordinal()) {
                maxEraInRow = card.getEra();
            }
        }
        if (maxEraInRow != currentEra) {
            currentEra = maxEraInRow;
            handleNewEra();
            return true;
        }
        return false;
    }

    /**
     * Handles the transition to a new Era:
     * discards lower row buildings (Era III only), moves upper row buildings
     * to the lower row, and reveals the new era's building cards in the upper row.
     */
    private void handleNewEra() {
        if (currentEra == Era.ERA_3) {
            board.discardLowerRowBuilding();
        }
        board.moveUpperRowBuildingToLower();
        board.revealEraBuildings(currentEra == Era.ERA_2 ? 2 : 3);
    }

    // END OF GAME
    /**
     * Returns true when the tribe deck is exhausted (last round in progress).
     * At this point, upper row events are also resolved.
     */
    @Override
    public boolean isGameOver() {
        return board.getTribeDeck().isEmpty();
    }

    @Override
    public void calculateFinalScores() {
        for (Player player : players) {
            Tribe tribe = player.getTribe();

            // Inventor PP: count × distinct invention icons
            int inventorPP = tribe.countByType(CharacterType.INVENTOR)
                    * tribe.getDistinctInventionIcons();

            // Artist PP: 10 PP per pair
            int artistPP = tribe.getArtistPairs() * 10;

            // Builder PP: sum of printed PP on Builder cards
            int builderPP = tribe.getBuilderPrestigeSum();

            // Printed Building PP (prestigeBonus printed on each Building card)
            int printedBuildingPP = tribe.getPrintedBuildingPrestige();

            // Building effect PP (end-game strategies: TYPE, SET, DOUBLE_BUILDER)
            int effectPP = tribe.triggerOnGameEnd(player);

            player.addPrestigePoints(inventorPP + artistPP + builderPP
                    + printedBuildingPP + effectPP);
        }
        notifyUpdate(this);
    }

    @Override
    public Player getWinner() {
        if (players.isEmpty()) return null;
        Player best = players.get(0);
        boolean tied = false;
        for (int i = 1; i < players.size(); i++) {
            Player p = players.get(i);
            int ppCmp = Integer.compare(p.getPrestigePoints(), best.getPrestigePoints());
            if (ppCmp > 0) {
                best = p;
                tied = false;
            } else if (ppCmp == 0) {
                int foodCmp = Integer.compare(p.getFood(), best.getFood());
                if (foodCmp > 0) {
                    best = p;
                    tied = false;
                } else if (foodCmp == 0) {
                    tied = true;
                }
            }
        }
        return tied ? null : best;
    }
}
