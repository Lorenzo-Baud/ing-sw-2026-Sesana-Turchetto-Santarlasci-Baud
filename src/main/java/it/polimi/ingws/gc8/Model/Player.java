package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;

/**
 * Represents a player in the game with identity, resources, and tribe components
 */
public class Player {

    /** Unique identifier for player */
    private final int playerId;

    /** Player display name */
    private final String nickname;

    /** Color of player's physical components */
    private final TotemColor totemColor;

    /** Current amount of food resources */
    private int foodPoints;

    /** Current amount of victory points */
    private int prestigePoints;

    /** Collection of cards and developments owned by player */
    private final Tribe tribe;

    /**
     * Constructs new player with starting resources and empty tribe
     * @param playerId unique id
     * @param nickname player name
     * @param totemColor selected color
     */
    public Player(int playerId, String nickname, TotemColor totemColor) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.totemColor = totemColor;
        this.foodPoints = 0;
        this.prestigePoints = 0;
        this.tribe = new Tribe();
    }

    /**
     * @return current food points
     */
    public int getFood() {
        return foodPoints;
    }

    /**
     * Adds food to the player. Amount must be non-negative.
     *
     * @param amount food to add
     */
    public void addFood(int amount) {
        foodPoints += amount;
    }

    /**
     * Removes food from the player. Food can go negative.
     *
     * @param amount food to remove
     */
    public void removeFood(int amount) {
        foodPoints -= amount;
    }

    // PRESTIGE MANAGEMENT

    /**
     * @return current prestige points
     */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /**
     * Adds prestige points to the player. Amount must be non-negative.
     *
     * @param amount PP to add
     */
    public void addPrestigePoints(int amount) {
        prestigePoints += amount;
    }

    /**
     * Removes prestige points from the player. PP can go negative (penalty).
     *
     * @param amount PP to remove
     */
    public void removePrestigePoints(int amount) {
        prestigePoints -= amount;
    }

    // CARD MANAGEMENT

    /**
     * Adds a Character card to the tribe, triggering building effects.
     *
     * @param card the Character card to add
     */
    public void addCharacterCard(CharacterCard card) {
        tribe.addCharacter(this, card);
    }

    /**
     * Adds a Building card to the tribe.
     *
     * @param card the Building card to add
     */
    public void addBuildingCard(BuildingCard card) {
        tribe.addBuilding(card);
    }

    // TRIBE QUERIES

    /**
     * @return the player's tribe
     */
    public Tribe getTribe() {
        return tribe;
    }

    /**
     * @return total number of Character cards in the tribe
     */
    public int getTotalCharacters() {
        return tribe.getTotalCharacters();
    }

    /**
     * @return total number of Shaman cards in the tribe
     */
    public int getTotalShamans() {
        return tribe.getTotalShamans();
    }

    /**
     * Returns the food discount applied when purchasing a Building card.
     * Based on Builder cards in the tribe (sum of their foodBonus).
     *
     * @return total building purchase discount
     */
    public int getTotalBuildingDiscount() {
        return tribe.getTotalBuilderDiscount();
    }

    /**
     * Returns unique player identifier
     * @return player id
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * Returns player nickname
     * @return player name
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns color associated with player's totem
     * @return totem color
     */
    public TotemColor getTotemColor() {
        return totemColor;
    }

}
