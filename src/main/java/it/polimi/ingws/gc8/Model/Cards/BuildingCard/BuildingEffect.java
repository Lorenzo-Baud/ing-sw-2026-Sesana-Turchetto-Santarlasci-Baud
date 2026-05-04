package it.polimi.ingws.gc8.Model.Cards.BuildingCard;


import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Player;

/**
 * Interface for Building card effects.
 * All methods have default no-op implementations, so each concrete class
 * only overrides the methods relevant to its specific effect.
 *
 * Methods are divided into two groups:
 * - Triggers: called by Tribe when something happens.
 *   These act directly on the Player.
 * - Queries: called by the game loop to check if a building modifies a rule.
 *   These return values that the game loop uses to make decisions.
 */
public interface BuildingEffect {

    //TRIGGERS

    /**
     * Called when a Character card is added to the player's tribe.
     * @param player the player who owns the building
     * @param card the Character card just added
     */
    default void onCardAdded(Player player, CharacterCard card) {}

    /**
     * Called during event resolution (Hunt, Cave Painting).
     * @param player the player who owns the building
     * @param event the Event card being resolved
     */
    default void onEvent(Player player, EventCard event) {}

    /**
     * Called when the player's Totem returns to the Turn Order tile.
     * @param player the player who owns the building
     * @param slotIndex the slot where the Totem was placed
     */
    default void onTurnEnd(Player player, int slotIndex) {}

    /**
     * Called at the end of the game to calculate bonus PP.
     * @param player the player who owns the building
     * @return bonus PP granted by this effect (0 if none)
     */
    default int onGameEnd(Player player) { return 0; }

    // QUERIES (called by the game loop to check building modifiers)

    /**
     * Returns the additional shaman icon bonus provided by this effect.
     *
     * @return the number of extra shaman icons, defaults to 0.
     */
    default int getShamanIconBonus() { return 0; }

    /**
     * Indicates whether this effect protects the player from penalties
     * associated with shaman requirements.
     *
     * @return {@code true} if the penalty is prevented, {@code false} otherwise (default).
     */
    default boolean preventsShamanPenalty() { return false; }

    /**
     * Determines if this effect amplifies the standard shaman bonus.
     *
     * @return {@code true} if the shaman bonus is doubled, {@code false} otherwise (default).
     */
    default boolean doublesShamanBonus() { return false; }

    /**
     * Specifies if the player is eligible for shaman-related gains even
     * in the event of a tie.
     *
     * @return {@code true} if gains are granted on a tie, {@code false} otherwise (default).
     */
    default boolean gainsOnShamanTie() { return false; }

    /**
     * Calculates the reduction in food sustenance costs provided by this effect
     * for the specified player.
     *
     * @param player the player benefiting from the reduction.
     * @return the amount of food cost to subtract, defaults to 0.
     */
    default int getSustenanceFoodReduction(Player player) { return 0; }

    /**
     * Checks if this effect grants the privilege of drawing an additional card
     * during the relevant game phase.
     *
     * @return {@code true} if an extra card can be picked, {@code false} otherwise (default).
     */
    default boolean canPickExtraCard() { return false; }

    /**
     * Returns the amount of bonus food granted based on the player's
     * position in the turn order track.
     *
     * @return the extra food amount, defaults to 0.
     */
    default int getExtraFoodOnTurnOrder() { return 0; }


    //STATIC FACTORY

    /**
     * Creates a BuildingEffect from a JSON effectType and its parameters.
     * @param type the effectType string from JSON
     * @param params the effectParams JSON object
     * @return a configured BuildingEffect instance
     */
    static BuildingEffect createFrom(String type, JsonNode params) {
        switch (type) {
            case "CONFIGURABLE":
                return ConfigurableEffect.fromJson(params);

            case "SET_COMPLETION":
                int setFoodReward = params.get("foodReward").asInt();
                return new SetCompletionEffect(setFoodReward);

            case "INVENTOR_PAIR":
                int pairFoodReward = params.get("foodReward").asInt();
                return new InventorPairEffect(pairFoodReward);

            default:
                throw new IllegalArgumentException("Unknown effect type: " + type);
        }
    }
}
