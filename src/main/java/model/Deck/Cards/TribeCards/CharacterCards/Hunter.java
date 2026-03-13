package model.Deck.Cards.TribeCards.CharacterCards;

import model.Deck.Cards.TribeCards.CharacterCard;

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
