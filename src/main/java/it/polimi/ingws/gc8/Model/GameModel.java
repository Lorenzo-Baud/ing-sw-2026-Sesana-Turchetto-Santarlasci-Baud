package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Board.Board;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.*;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

import java.util.List;

/**
 * Public interface for the MESOS game model.
 * Exposes all operations that the controller/view can invoke,
 * organized by game phase.
 */
public interface GameModel {

    // SETUP

    /**
     * Initializes a new game: creates players, board, decks,
     * distributes initial food and randomizes turn order.
     * @param numPlayers number of players (2-5)
     */
    void setupGame(int numPlayers);

    /**
     * @return number of players in the game
     */
    int getNumPlayers();

    /**
     * @return current round number (1-10)
     */
    int getCurrentRound();

    /**
     * @return current Era (ERA_1, ERA_2, ERA_3)
     */
    Era getCurrentEra();

    /**
     * @return current game phase
     */
    GamePhase getPhase();

    /**
     * @return list of all players
     */
    List<Player> getPlayers();

    /**
     * @return the Board with rows, decks and tracks
     */
    Board getBoard();

    /**
     * @return the player who must act at this moment
     */
    Player getCurrentPlayer();
    /**
     * notifies to the view a model update
     * @param model (the current model)
     */
    void notifyUpdate(GameModel model);


    public void setCurrentPlayer(Player player);

    // PHASE 1: TOTEM PLACEMENT

    /**
     * @return list of players in the order defined by the Turn Order tile
     */
    List<Player> getPlayersInTurnOrder();

    /**
     * @return list of Offer tiles with no Totem on them
     */
    List<OfferTile> getAvailableOfferTiles();

    /**
     * Places the player's Totem on the chosen Offer tile.
     * @param player the player placing the Totem
     * @param offerTile the chosen Offer tile (must be free)
     */
    void placeTotem(Player player, OfferTile offerTile);

    /**
     * @return true if all players have placed their Totem
     */
    boolean allTotemsPlaced();

    // PHASE 2: ACTION RESOLUTION

    /**
     * @return list of players in the order of their Totems on the Offer track
     *         (left to right, tile A to G)
     */
    List<Player> getPlayersInOfferOrder();

    /**
     * @param row the row to pick from (upper or lower)
     * @return list of available cards in that row
     *         (excluding Event cards)
     */
    List<Card> getAvailableCardsFromRow(int row);

    /**
     * Checks whether the player can afford a Building card,
     * considering the Builder discount.
     * @param player the player
     * @param building the Building card
     * @return true if the player has enough food
     */
    boolean canPlayerAffordBuilding(Player player, BuildingCard building);

    /**
     * The player takes a Character card from a row.
     * Triggers onCardAdded on the player's buildings.
     * @param player the player
     * @param card the chosen Character card
     */
    void takeCharacterCard(Player player, CharacterCard card);

    /**
     * The player buys a Building card: pays the cost
     * (with Builder discount) and adds it to the tribe.
     * @param player the player
     * @param card the chosen Building card
     */
    void takeBuildingCard(Player player, BuildingCard card);

    /**
     * The player takes 3 Food (tile A only, 5-player games).
     * @param player the player
     */
    void takeFood(Player player);

    /**
     * Returns the player's Totem to the Turn Order tile
     * in the first available slot. Handles food bonus/penalty
     * and queries buildings for extra food.
     * @param player the player
     */
    void returnTotemToTurnOrder(Player player);

    /**
     * Checks whether the player owns a building that allows
     * picking an extra card from the upper row.
     * @param player the player
     * @return true if the player can pick an extra card
     */
    boolean canPickExtraCard(Player player);

    /**
     * The player takes an extra card from the upper row
     * (effect of the corresponding building).
     * @param player the player
     * @param card the chosen card from the upper row
     */
    void takeExtraCard(Player player, Card card);

    // END OF ROUND: EVENT RESOLUTION

    /**
     * @return list of Event cards in the lower row
     *         (and upper row if it's the last round),
     *         ordered for resolution (Sustenance last)
     */
    List<EventCard> getEventsToResolve();



    // END OF ROUND: ROW AND ERA MANAGEMENT

    /**
     * Discards all Character and Event cards from the lower row.
     * Building cards remain in place.
     */
    void discardLowerRow();

    /**
     * Moves Character and Event cards from the upper row
     * to the lower row. Building cards remain in the upper row.
     */
    void moveUpperRowToLower();

    /**
     * Draws (numPlayers + 4) cards from the Tribe deck
     * to create a new upper row.
     */
    void refreshUpperRow();

    /**
     * Checks whether a new Era has started
     * (first card of the next Era revealed in the upper row).
     * @return true if the Era has changed
     */
    boolean checkNewEra();

    // END OF GAME

    /**
     * @return true if the Tribe deck is exhausted and round 10 is complete
     */
    boolean isGameOver();

    /**
     * Calculates final scores: Builder PP, Inventor PP, Artist PP,
     * printed Building PP, Building effect PP
     * (via triggerOnGameEnd).
     */
    void calculateFinalScores();

    /**
     * @return the player with the most PP. On tie, the one with the most food.
     *         On further tie, null (shared victory).
     */
    Player getWinner();
}