package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.Era;

public class Builder extends CharacterCard {

    // Attributes
    private final int foodBonus;
    private final int prestigePoints;

    // Constructor
    public Builder(String front, String back, Era era, int foodBonus, int prestigePoints, int numberOfPlayers) {
        super(front, back, era, numberOfPlayers);
        this.foodBonus = foodBonus;
        this.prestigePoints = prestigePoints;
    }

    // Getters
    public int getFoodBonus() {
        return foodBonus;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

}
