package it.polimi.ingws.gc8.Model.Deck.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.CharacterCard;

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
