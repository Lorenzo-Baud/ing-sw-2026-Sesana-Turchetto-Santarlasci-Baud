package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

public class CharacterCard extends Card {

    // Minimum number of players required for the card to be used
    final int numberOfPlayers;

    // Character type
    final CharacterType characterType;

    // Constructor
    public CharacterCard(String front, String back, Era era, int numberOfPlayers, CharacterType characterType) {
        super(front, back, era, CardType.CHARACTER);
        this.numberOfPlayers = numberOfPlayers;
        this.characterType = characterType;
    }

    // Getters
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }

}
