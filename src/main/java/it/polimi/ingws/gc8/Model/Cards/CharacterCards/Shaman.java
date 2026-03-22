package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

public class Shaman extends CharacterCard {

    // Attributes
    private final int nIcons;

    public Shaman(String front, String back, Era era, int nIcons, int numberOfPlayers) {
        super(front, back, era, numberOfPlayers, CharacterType.SHAMAN);
        this.nIcons = nIcons;
    }

    // Getter
    public int getnIcons() {
        return nIcons;
    }

}
