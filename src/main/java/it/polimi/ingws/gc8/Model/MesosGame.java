package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Deck.Cards.BuildingCard;
import it.polimi.ingws.gc8.Model.Deck.Cards.Card;
import it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Model.Board.Board;

import java.util.List;
import java.util.ArrayList;

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

    public MesosGame() {
        this.players = new ArrayList<>();
        this.currentRound = 0;
        this.currentEra = Era.ERA_1;
        this.phase = GamePhase.SETUP;
        this.currentPlayerIndex = 0;
    }

    // SETUP

    @Override
    public void setupGame(int numPlayers) {
        this.numPlayers = numPlayers;
        initializePlayers(numPlayers);
        initializeBoard(numPlayers);
        randomizeTurnOrder();
        distributeInitialFood();
        this.currentRound = 1;
        this.phase = GamePhase.POSITIONING;
        // TODO: notify observers that the game has started
    }

    /**
     * Creates Players.
     */
    private void initializePlayers(int numPlayers) {
        // TODO
    }

    /**
     * Creates the Board: loads Tribe and Building cards, prepares decks,
     * reveals initial rows and positions the Offer tiles.
     */
    private void initializeBoard(int numPlayers) {
        // TODO: create the Board
        // TODO: filter Offer tiles based on player count
        // TODO: build the Offer track
        // TODO: select the correct Turn Order tile
        // TODO: filter Tribe cards based on player count
        // TODO: build the Tribe deck (Era I on top, Era II, Era III, Final Events at bottom)
        // TODO: load Building cards via BuildingCard.loadFromJson()
        // TODO: filter Building cards based on player count
        // TODO: distribute Building cards into 3 decks by Era
        // TODO: reveal Era I Building cards in the upper row
        // TODO: draw cards for the lower row (numPlayers + 1)
        // TODO: draw cards for the upper row (numPlayers + 4)
    }

    /**
     * Randomly places Totems on the Turn Order tile.
     */
    private void randomizeTurnOrder() {
        // TODO: place Totems in random order on the TurnOrderTrack
    }

    /**
     * Distributes initial food based on position:
     * 1st player: 2 Food, 2nd-3rd: 3 Food, 4th-5th: 4 Food.
     */
    private void distributeInitialFood() {
        // TODO: iterate over players in TurnOrderTrack order
        //       and assign food based on position
    }

    // GETTERS

    @Override
    public int getNumPlayers() {
        return numPlayers;
    }

    @Override
    public int getCurrentRound() {
        return currentRound;
    }

    @Override
    public Era getCurrentEra() {
        return currentEra;
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    @Override
    public List<Player> getPlayers() {
        return players;
    }

    @Override
    public Board getBoard() {
        return board;
    }

    @Override
    public Player getCurrentPlayer() {
        // TODO
        return null;
    }

    // PHASE 1: TOTEM PLACEMENT

    @Override
    public List<Player> getPlayersInTurnOrder() {
        // TODO: read order from the Board's TurnOrderTrack
        return null;
    }

    @Override
    public List<OfferTile> getAvailableOfferTiles() {
        // TODO
        return null;
    }

    @Override
    public void placeTotem(Player player, OfferTile offerTile) {
        // TODO: verify that the current phase is POSITIONING
        // TODO: verify that it is this player's turn
        // TODO: verify that the Offer tile is free
        // TODO: place the Totem on the tile (offerTile.placeTotem(player))
        // TODO: advance to the next player
        // TODO: if all players have placed, switch to GamePhase.ACTION_RESOLUTION
    }

    @Override
    public boolean allTotemsPlaced() {
        // TODO: check that all fillable Offer tiles have a Totem
        //       or that all players have placed
        return false;
    }

    // PHASE 2: ACTION RESOLUTION

    @Override
    public List<Player> getPlayersInOfferOrder() {
        // TODO: return players in the order of their Totems on the Offer track
        //       (left to right, tile A to G)
        return null;
    }

    @Override
    public List<Card> getAvailableCardsFromRow(Row row) {
        // TODO: delegate to board.getUpperRow() or board.getLowerRow()
        // TODO: filter out Event cards (not pickable)
        // TODO: return only Character and Building cards
        return null;
    }

    @Override
    public boolean canPlayerAffordBuilding(Player player, BuildingCard building) {
        // TODO: calculate discount: player.getTotalBuilderDiscount()
        // TODO: calculate effective cost: building.getEffectiveCost(discount)
        // TODO: compare with player.getFood()
        return false;
    }

    @Override
    public void takeCharacterCard(Player player, CharacterCard card) {
        // TODO: verify that the card is in a row accessible to the player
        // TODO: remove the card from the Board row
        // TODO: add to tribe: player.getTribe().addCharacter(player, card)
        //       (this automatically triggers onCardAdded on buildings)
        // TODO: handle immediate Hunter bonus icon effect
    }

    @Override
    public void takeBuildingCard(Player player, BuildingCard card) {
        // TODO: verify that the player can afford it (canPlayerAffordBuilding)
        // TODO: calculate effective cost and remove food from player
        // TODO: remove the card from the Board row
        // TODO: add to tribe: player.getTribe().addBuilding(card)
    }

    @Override
    public void takeFood(Player player) {
        // TODO: verify that the player is on tile A (5-player games only)
        // TODO: add 3 Food to the player
    }

    @Override
    public void returnTotemToTurnOrder(Player player) {
        // TODO: remove the Totem from the Offer tile
        // TODO: place the Totem in the first free slot on the TurnOrderTrack
        // TODO: if the slot has a food bonus, assign it
        // TODO: query buildings for extra food:
        //       for (BuildingCard building : player.getTribe().getBuildings())
        //           extraFood += building.getEffect().getExtraFoodOnTurnOrder();
        //       if base bonus > 0, also add extraFood
        // TODO: if it's the last slot, the player pays 1 food
        //       (if no food, lose 2 PP instead; the building has no effect here)
        // TODO: update turn order for the next round
    }

    @Override
    public boolean canPickExtraCard(Player player) {
        // TODO: iterate over the player's buildings
        //       and check if any has canPickExtraCard() == true
        return false;
    }

    @Override
    public void takeExtraCard(Player player, Card card) {
        // TODO: verify that canPickExtraCard(player) is true
        // TODO: verify that the card is in the upper row
        // TODO: if it's a Building, the player must pay the cost
        // TODO: remove the card from the upper row
        // TODO: add to tribe (same as takeCharacterCard or takeBuildingCard)
    }

    // END OF ROUND: EVENT RESOLUTION

    @Override
    public List<EventCard> getEventsToResolve() {
        // TODO: collect Event cards from the lower row
        // TODO: if it's the last round, also include those from the upper row
        // TODO: order them: Sustenance always last
        // TODO: if two events of the same type, order by Era
        return null;
    }

    @Override
    public void resolveHunt(EventCard event) {
        // TODO: for each player:
        //   - count Hunters: player.getTribe().countByType(HUNTER)
        //   - assign base food: +1 per Hunter
        //   - assign base PP: event.getPPPerUnit() per Hunter
        //   - trigger buildings: player.getTribe().triggerOnEvent(player, event)
        //     (ConfigurableEffect with triggerEvent=HUNT adds extra food and PP)
    }

    @Override
    public void resolveShamanicRitual(EventCard event) {
        // TODO: STEP 1 - collect icons for all players:
        //   for each player:
        //   - base icons: player.getTribe().getTotalShamanIcons()
        //   - building bonus icons: player.getTribe().getTotalShamanIconBonus()
        //   - total = base + bonus

        // TODO: STEP 2 - determine max and min across all players

        // TODO: STEP 3 - apply bonuses and penalties:
        //   for each player:
        //   - if has max:
        //     - pp = event.getBonusPP()
        //     - if player.getTribe().hasShamanDouble(): pp *= 2
        //     - assign PP
        //   - if has min:
        //     - if !player.getTribe().hasShamanShield(): lose event.getPenaltyPP()
        //   - handle tie (all tied: first gain, then lose)
        //   - handle gainsOnShamanTie (building that grants PP even on tie)
    }

    @Override
    public void resolveCavePainting(EventCard event) {
        // TODO: for each player:
        //   - count Artists: player.getTribe().countByType(ARTIST)
        //   - if artists < event minimum threshold: lose penalty PP
        //   - if artists >= threshold: gain PP per Artist
        //   - trigger buildings: player.getTribe().triggerOnEvent(player, event)
        //     (ConfigurableEffect with triggerEvent=CAVE_PAINTING adds food)
    }

    @Override
    public void resolveSustenance(EventCard event) {
        // TODO: for each player:
        //   - count Characters: player.getTribe().getTotalCharacters()
        //   - Collector discount: player.getTribe().getCollectorDiscount()
        //   - building discount: player.getTribe().getTotalSustenanceReduction(player)
        //   - food to pay = max(0, characters - collectorDiscount - buildingDiscount)
        //   - pay as much food as possible (player MUST pay)
        //   - for each unfed Character: lose event.getPenaltyPP()
    }

    // END OF ROUND: ROW AND ERA MANAGEMENT

    @Override
    public void discardLowerRow() {
        // TODO
    }

    @Override
    public void moveUpperRowToLower() {
        // TODO
    }

    @Override
    public void refreshUpperRow() {
        // TODO
    }

    @Override
    public boolean checkNewEra() {
        // TODO
        return false;
    }

    /**
     * Handles the transition to a new Era:
     * moves/discards Building cards and reveals those of the new Era.
     */
    private void handleNewEra() {
        // TODO: 1. discard Building cards from the lower row
        //          (only happens at the start of Era III)
        // TODO: 2. move Building cards from the upper row to the lower row
        //          (happens at the start of Era II and Era III)
        // TODO: 3. place the new Era's Building cards in the upper row
        //          next to the Tribe cards
    }

    // END OF GAME

    @Override
    public boolean isGameOver() {
        // TODO: return true if currentRound > 10
        //       (Tribe deck is exhausted and rows can no longer be refreshed)
        return false;
    }

    @Override
    public void calculateFinalScores() {
        // TODO: for each player:
        //   Tribe tribe = player.getTribe();
        //
        //   // Inventor PP: numInventors * numDistinctInventionIcons
        //   int inventorPP = tribe.countByType(INVENTOR) * tribe.getDistinctInventionIcons();
        //
        //   // Artist PP: 10 PP per pair
        //   int artistPP = tribe.getArtistPairs() * 10;
        //
        //   // Builder PP: summed directly from cards
        //
        //   // Printed Building PP
        //   int printedBuildingPP = tribe.getPrintedBuildingPrestige();
        //
        //   // Building effect PP
        //   int effectPP = tribe.triggerOnGameEnd(player);
        //
        //   player.addPrestigePoints(inventorPP + artistPP + printedBuildingPP + effectPP);
    }

    @Override
    public Player getWinner() {
        // TODO: find the player with the most PP
        //  on tie, the one with the most food wins
        //  on further tie, return null (shared victory)
        return null;
    }
}