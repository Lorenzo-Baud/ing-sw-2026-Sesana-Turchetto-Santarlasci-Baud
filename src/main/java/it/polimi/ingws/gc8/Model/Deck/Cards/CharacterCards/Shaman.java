<<<<<<<< Updated upstream:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/TribeCards/CharacterCards/Shaman.java
package it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards.CharacterCard;
========
package model.Deck.Cards.CharacterCards;
import model.Enum.Era;
import model.Deck.Cards.CharacterCard;
>>>>>>>> Stashed changes:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/CharacterCards/Shaman.java

public class Shaman extends CharacterCard {

    // Attributes
    private final int nIcons;

    public Shaman(String front, String back, Era era, int nIcons) {
        super(front, back, era);
        this.nIcons = nIcons;
    }

    // Getter
    public int getnIcons() {
        return nIcons;
    }

}
