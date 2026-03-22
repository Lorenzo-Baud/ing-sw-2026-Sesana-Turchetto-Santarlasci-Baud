package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Enum.Era;

public class Inventor extends CharacterCard {

    // Attribute
    private final int icon;

    public Inventor(String front, String back, Era era, int numberOfPlayers, int icon) {
        super(front, back, era, numberOfPlayers);
        this.icon = icon;
    }

    // Getter
    public int getIcon() {
        return icon;
    }

}
