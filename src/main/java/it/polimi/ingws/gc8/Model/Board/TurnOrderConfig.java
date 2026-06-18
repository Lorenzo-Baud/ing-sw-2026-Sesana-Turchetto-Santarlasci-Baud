package it.polimi.ingws.gc8.Model.Board;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Configuration settings for turn order track based on player count
 */
public class TurnOrderConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Number of players for this configuration
     */
    private int playerCount;

    /**
     * List of food bonuses granted for each slot position
     */
    private List<Integer> foodBonusesBySlot;

    /**
     * Food resource penalty applied to last slot
     */
    private int lastSlotFoodPenalty;

    /**
     * Prestige points penalty applied to last slot
     */
    private int lastSlotPrestigePenalty;

    /**
     * Constructs turn order configuration from JSON properties
     *
     * @param playerCount number of players for configuration
     * @param foodBonusesBySlot list of food rewards for each track position
     * @param lastSlotFoodPenalty food penalty for player in last position
     * @param lastSlotPrestigePenalty prestige penalty for player in last position
     */
    @JsonCreator
    public TurnOrderConfig(
            @JsonProperty("playerCount")            int playerCount,
            @JsonProperty("foodBonusesBySlot")      List<Integer> foodBonusesBySlot,
            @JsonProperty("lastSlotFoodPenalty")    int lastSlotFoodPenalty,
            @JsonProperty("lastSlotPrestigePenalty") int lastSlotPrestigePenalty
    ) {
        this.playerCount = playerCount;
        this.foodBonusesBySlot = foodBonusesBySlot;
        this.lastSlotFoodPenalty = lastSlotFoodPenalty;
        this.lastSlotPrestigePenalty = lastSlotPrestigePenalty;
    }

    /**
     * Returns player count for this configuration
     * @return number of players
     */
    public int getPlayerCount() { return playerCount; }

    /**
     * Returns list of food bonuses for each slot
     * @return food bonuses by position
     */
    public List<Integer> getFoodBonusesBySlot() { return foodBonusesBySlot; }

    /**
     * Returns food penalty for last position
     * @return last slot food penalty
     */
    public int getLastSlotFoodPenalty() { return lastSlotFoodPenalty; }

    /**
     * Returns prestige penalty for last position
     * @return last slot prestige penalty
     */
    public int getLastSlotPrestigePenalty() { return lastSlotPrestigePenalty; }

}