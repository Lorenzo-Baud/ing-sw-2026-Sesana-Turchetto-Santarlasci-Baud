package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

public class Hunter extends CharacterCard {

    // Attribute
    private final boolean bonus;

    public Hunter(String front, String back, Era era, int numberOfPlayers, boolean bonus) {
        super(front, back, era, numberOfPlayers, CharacterType.HUNTER);
        this.bonus = bonus;
    }

    // Getter
    public boolean hasBonus() {
        return bonus;
    }

}
