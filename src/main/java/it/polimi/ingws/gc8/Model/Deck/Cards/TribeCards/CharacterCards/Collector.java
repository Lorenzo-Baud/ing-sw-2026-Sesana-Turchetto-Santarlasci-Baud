<<<<<<<< Updated upstream:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/TribeCards/CharacterCards/Collector.java
package it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCard;
========
package model.Deck.Cards.CharacterCards;
>>>>>>>> Stashed changes:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/CharacterCards/Collector.java

import model.Deck.Cards.CharacterCard;
import model.Enum.Era;
public class Collector extends CharacterCard {

    // Attributes
    private final int foodDiscount;

    public Collector(String front, String back, Era era, int foodDiscount) {
        super(front, back, era);
        this.foodDiscount = foodDiscount;
    }

    // Getters
    public int getFoodDiscount() {
        return foodDiscount;
    }

}
