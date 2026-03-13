package model.Board;

import java.util.List;

public class TurnOrderTrack {

    // Attributes
    private int playerCount;
    private final List<Integer> foodBonusesBySlot;
    private final int lastSlotFoodPenalty;
    private final int lastSlotPrestigePenalty;

    // Players positions
    // private List<Player> occupantsBySlot;

    // Constructor
    public TurnOrderTrack(int playerCount, List<Integer> foodBonusesBySlot,
                          int lastSlotFoodPenalty, int lastSlotPrestigePenalty) {
        this.playerCount = playerCount;
        this.foodBonusesBySlot = foodBonusesBySlot;
        this.lastSlotFoodPenalty = lastSlotFoodPenalty;
        this.lastSlotPrestigePenalty = lastSlotPrestigePenalty;
    }

    // Method to place players
    // TODO

    // Gets first free slot
    public int getFirstFreeSlotIndex() {
        return 0;
    }

    // Returns player totem
    // TODO

    // Returns food bonus for the specified slot
    public int getFoodBonusForSlot(int slotIndex) {
        return foodBonusesBySlot.get(slotIndex);
    }

    // Gets occupant at specified slot
    // TODO

    // Returns true if the slot is the last
    public boolean isLastSlot(int slotIndex) {
        // TODO: correct
        return slotIndex == 4;
    }

    // Getters
    public int getLastSlotFoodPenalty() {
        return lastSlotFoodPenalty;
    }

    public int getLastSlotPrestigePenalty() {
        return lastSlotPrestigePenalty;
    }

}
