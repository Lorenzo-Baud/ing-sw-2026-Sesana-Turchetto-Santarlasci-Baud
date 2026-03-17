<<<<<<<< Updated upstream:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/TribeCards/CharacterCards/Hunter.java
package it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCard;
========
package model.Deck.Cards.CharacterCards;

import model.Deck.Cards.CharacterCard;
import model.Enum.Era;
>>>>>>>> Stashed changes:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/CharacterCards/Hunter.java

public class Hunter extends CharacterCard {

    // Attribute
    private final boolean bonus;

    public Hunter(String front, String back, Era era, boolean bonus) {
        super(front, back, era);
        this.bonus = bonus;
    }

    // Getter
    public boolean hasBonus() {
        return bonus;
    }
}
