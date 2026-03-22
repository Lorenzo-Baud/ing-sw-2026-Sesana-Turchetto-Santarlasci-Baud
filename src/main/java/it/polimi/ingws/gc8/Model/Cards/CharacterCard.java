package it.polimi.ingws.gc8.Model.Cards;

import it.polimi.ingws.gc8.Model.Enum.Era;

public class CharacterCard extends Card {

    // Minimum number of players required for the card to be used
    int numberOfPlayers;

    // Constructor
    public CharacterCard(String front, String back, Era era, int numberOfPlayers) {
        super(front, back, era);
        this.numberOfPlayers = numberOfPlayers;
    }

}
