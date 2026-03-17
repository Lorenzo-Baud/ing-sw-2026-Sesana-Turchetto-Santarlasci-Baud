package it.polimi.ingws.gc8.Model.Deck.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Deck.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.Era;

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
