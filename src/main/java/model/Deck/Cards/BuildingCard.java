package model.Deck.Cards;

public class BuildingCard extends Card {

    // Attributes
    private final int foodCost;
    private final int prestigeBonus;

    // Effect
    // TODO: tbdf

    // Constructor
    public BuildingCard(String front, String back, Era era,
                        int foodCost, int prestigeBonus) {
        super(front, back, era);
        this.foodCost = foodCost;
        this.prestigeBonus = prestigeBonus;
    }

    // Getters/setters
    public int getFoodCost() {
        return foodCost;
    }
    public int getPrestigeBonus() {
        return prestigeBonus;
    }

}
