<<<<<<<< Updated upstream:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/TribeCards/CharacterCards/Builder.java
package it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.Card;
import it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCard;
========
package model.Deck.Cards.CharacterCards;


import model.Deck.Cards.Card;
import model.Deck.Cards.CharacterCard;
import model.Enum.Era;
>>>>>>>> Stashed changes:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/CharacterCards/Builder.java

public class Builder extends CharacterCard {

    // Attributes
    private final int foodBonus;
    private final int prestigePoints;

    // Constructor
    // Constructor
    public Builder(String front, String back, Era era, int foodBonus, int prestigePoints) {
        super(front, back, era);
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
