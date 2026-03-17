<<<<<<<< Updated upstream:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/TribeCards/CharacterCards/Inventor.java
package it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCard;
========
package model.Deck.Cards.CharacterCards;

import model.Deck.Cards.CharacterCard;
import model.Enum.Era;
>>>>>>>> Stashed changes:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/CharacterCards/Inventor.java

public class Inventor extends CharacterCard {

    // Attribute
    private final String icon;

    public Inventor(String front, String back, Era era, String icon) {
        super(front, back, era);
        this.icon = icon;
    }

    // Getter
    public String getIcon() {
        return icon;
    }

}
