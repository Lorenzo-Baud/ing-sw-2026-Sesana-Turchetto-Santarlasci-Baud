package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Board.Board;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Concrete implementation of the MESOS game model.
 * Manages the game state and implements all game logic.
 */
public class MesosGame implements GameModel {

    /**
     * The main game board that manages the decks, tracks, and cards currently in play.
     */
    private Board board;

    /**
     * The total number of players participating in the game session.
     */
    private int numPlayers;

    /**
     * The current round number within the active era.
     */
    private int currentRound;

    /**
     * The current historical era (e.g., ERA_1, ERA_2, ERA_3) which dictates deck usage.
     */
    private Era currentEra;

    /**
     * The current phase of the game state machine (e.g., PLACEMENT, RESOLUTION).
     */
    private GamePhase phase;

    /**
     * The list of all players in the game, typically sorted by turn order.
     */
    private List<Player> players;

    /**
     * The list of observers registered to receive updates whenever the model's state changes.
     */
    private final List<ModelObserver> observers;

    /**
     * Initializes a new game instance with default setup values.
     * Sets the round to zero and the era to the first era.
     * Initializes empty lists for both players and model observers.
     */
    public MesosGame() {
        this.currentRound = 0;
        this.currentEra = Era.ERA_1;
        this.phase = GamePhase.SETUP;
        this.players = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
    }

    @Override
    public void addObserver(ModelObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(ModelObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers of a change in the model's state.
     * Iterates through the list of observers and calls their update method,
     * passing the current game instance as the subject.
     */
    private void notifyObservers() {
        for (ModelObserver o : observers) {
            o.onModelChanged(this);
        }
    }

    @Override
    public void setPhase(GamePhase phase) {
        this.phase = phase;
        notifyObservers();
    }

    /**
     * Sets up the initial state of the game for a specified number of players.
     * This method initializes the board, players, and turn order, grants starting resources,
     * sets the round to the first round, and alerts all observers that the setup is complete.
     *
     * @param numPlayers the number of players participating in the game.
     */
    @Override
    public void setupGame(int numPlayers, List<Player> players) {

        // Parameters check
        if(players.size() != numPlayers){
            throw new IllegalArgumentException("Players list must be non-null and match the number of players.");
        }

        // Parameters assignment
        this.currentRound = 1;
        this.numPlayers = numPlayers;
        this.board = new Board(numPlayers);
        this.players = new ArrayList<>(players);

        // Game initialization methods invocation
        randomizeTurnOrder();
        distributeInitialFood();

        // Notifies observers
        notifyObservers();

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
    public List<Player> getPlayers() { return players; }

    @Override
    public Board getBoard() { return board; }

    @Override
    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        switch (phase) {
            case POSITIONING:
                // Primo giocatore nell'ordine di turno che non ha ancora piazzato il Totem.
                for (Player p : board.getTurnOrderTrack().getCurrentOrder()) {
                    if (p != null && !board.getOfferTrack().containsPlayer(p)) {
                        return p;
                    }
                }
                return null;
            case ACTION_RESOLUTION:
                // Giocatore con il Totem più a sinistra sul tracciato Offerte.
                List<OfferTile> resolutionOrder = board.getOfferTrack().getResolutionOrder();
                return resolutionOrder.isEmpty() ? null : resolutionOrder.get(0).getOccupant();
            default:
                return null;
        }
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
     * Places the player's Totem on the specified Offer tile
     *
     * @param player    the player placing the Totem
     * @param offerTile the chosen Offer tile (must be free)
     * @return true if placement was successful, false otherwise (e.g., tile occupied)
     */
    @Override
    public boolean placeTotem(Player player, OfferTile offerTile) {
        try {
            board.getOfferTrack().placeTotem(offerTile.getId(), player);
            board.getTurnOrderTrack().returnTotem(player);
            notifyObservers();
            return true;
        } catch (Exception e) {
            return false;
        }
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

    @Override
    public OfferTile getOfferTile(Player player) {
        for (OfferTile tile : board.getOfferTrack().getTiles()) {
            if (player.equals(tile.getOccupant())) {
                return tile;
            }
        }
        return null;
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
    public void pickCharacterCard(Player player, CharacterCard card) {
        board.getLowerRow().remove(card);
        board.getUpperRow().remove(card);
        player.addCharacterCard(card);
        notifyObservers();
    }

    @Override
    public boolean pickBuildingCard(Player player, BuildingCard card) {
        int discount = player.getTribe().getTotalBuilderDiscount();

        // Check affordability before modifying the game state
        if(card.getEffectiveCost(discount) > player.getFood()) {
            return false;
        }

        player.removeFood(card.getEffectiveCost(discount));
        board.getLowerRow().remove(card);
        board.getUpperRow().remove(card);
        player.addBuildingCard(card);
        notifyObservers();

        return true;

    }

    @Override
    public void takeFood(Player player) {
        player.addFood(3);
        notifyObservers();
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
        notifyObservers();
    }

    @Override
    public boolean canPickExtraCard(Player player) {
        return player.getTribe().canPickExtraCard();
    }

    @Override
    public void takeExtraCard(Player player, Card card) {
        if (card instanceof BuildingCard) {
            pickBuildingCard(player, (BuildingCard) card);
        } else if (card instanceof CharacterCard) {
            pickCharacterCard(player, (CharacterCard) card);
        }
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
        notifyObservers();
    }

    @Override
    public void moveUpperRowToLower() {
        board.moveUpperRowTribeCardsToLower();
        notifyObservers();
    }

    @Override
    public void refreshUpperRow() {
        for (int i = 0; i < numPlayers + 4; i++) {
            if (!board.getTribeDeck().isEmpty()) {
                board.getUpperRow().add(board.getTribeDeck().removeFirst());
            }
        }
        currentRound++;
        notifyObservers();
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
        notifyObservers();
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
