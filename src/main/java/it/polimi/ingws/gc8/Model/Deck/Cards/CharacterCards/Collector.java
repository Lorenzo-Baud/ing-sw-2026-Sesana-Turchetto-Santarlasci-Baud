package it.polimi.ingws.gc8.Model.Deck.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.CharacterCard;

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
