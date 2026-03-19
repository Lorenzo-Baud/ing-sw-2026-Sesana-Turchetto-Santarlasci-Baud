package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TurnOrderConfig {
    private int playerCount;
    private List<Integer> foodBonusesBySlot;
    private int lastSlotFoodPenalty;
    private int lastSlotPrestigePenalty;

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

    public int getPlayerCount() { return playerCount; }
    public List<Integer> getFoodBonusesBySlot() { return foodBonusesBySlot; }
    public int getLastSlotFoodPenalty() { return lastSlotFoodPenalty; }
    public int getLastSlotPrestigePenalty() { return lastSlotPrestigePenalty; }
}