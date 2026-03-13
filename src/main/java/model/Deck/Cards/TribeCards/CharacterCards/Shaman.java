package model.Deck.Cards.TribeCards.CharacterCards;

import model.Deck.Cards.TribeCards.CharacterCard;

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
