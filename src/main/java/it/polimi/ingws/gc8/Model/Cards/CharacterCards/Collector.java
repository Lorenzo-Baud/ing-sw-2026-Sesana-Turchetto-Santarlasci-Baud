package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.Era;

public class Collector extends CharacterCard {

    // Attributes
    private final int foodDiscount;

    public Collector(String front, String back, Era era, int numberOfPlayers, int foodDiscount) {
        super(front, back, era, numberOfPlayers);
        this.foodDiscount = foodDiscount;
    }

    // Getters
    public int getFoodDiscount() {
        return foodDiscount;
    }

}
