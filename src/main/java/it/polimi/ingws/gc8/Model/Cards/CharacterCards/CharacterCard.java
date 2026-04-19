package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;

public class CharacterCard extends Card {

    // Minimum number of players required for the card to be used
    final int numberOfPlayers;

    // Character type
    final CharacterType characterType;

    // Constructor
    public CharacterCard(String front, String back, Era era, int numberOfPlayers, CharacterType characterType) {
        super(front, back, era);
        this.numberOfPlayers = numberOfPlayers;
        this.characterType = characterType;
    }

    // Getter
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }

    /**
     * Creates a Data Transfer Object (DTO) representing the current state of the card.
     * Used for sending card information over the network while hiding logic.
     * * @return a DTO version of the card
     */
    @Override
    public CardDTO toDTO() {
        return new CardDTO(this.front, this.back, false, CardType.CHARACTER);
    }


}
