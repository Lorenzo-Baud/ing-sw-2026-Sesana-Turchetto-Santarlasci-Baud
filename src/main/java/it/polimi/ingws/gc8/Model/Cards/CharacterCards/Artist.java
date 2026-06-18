package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

/**
 * Represents the Artist character card.
 * Artists typically provide effects related to victory points or specialized
 * end-game scoring strategies.
 */
public class Artist extends CharacterCard {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an Artist card with the specified assets and game requirements.
     *
     * @param front           the path or identifier for the front image.
     * @param back            the path or identifier for the back image.
     * @param era             the game era this card belongs to.
     * @param numberOfPlayers the player count requirement for this card's availability.
     */
    public Artist(String front, String back, Era era, int numberOfPlayers) {
        super(front, back, era, numberOfPlayers, CharacterType.ARTIST);
    }
}