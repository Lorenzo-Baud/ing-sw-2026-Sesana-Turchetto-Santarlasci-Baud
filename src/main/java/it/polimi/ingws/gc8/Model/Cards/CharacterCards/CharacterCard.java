package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

/**
 * Abstract representation of a Character Card within the game.
 * Character cards belong to specific eras and may have availability constraints
 * based on the total number of players in the session.
 */
public abstract class CharacterCard extends Card {

    private static final long serialVersionUID = 1L;

    /** The minimum number of players required for this card to be included in the deck. */
    private final int numberOfPlayers;

    /** The specific category or role of the character. */
    private final CharacterType characterType;

    /**
     * Constructs a CharacterCard with core attributes and network-ready assets.
     *
     * @param front           the identifier for the front-facing asset.
     * @param back            the identifier for the back-facing asset.
     * @param era             the historical era associated with this card.
     * @param numberOfPlayers the player count threshold for card availability.
     * @param characterType   the classification of this character (e.g., Builder, Artist).
     */
    public CharacterCard(String front, String back, Era era, int numberOfPlayers, CharacterType characterType) {
        super(front, back, era, CardType.CHARACTER);
        this.numberOfPlayers = numberOfPlayers;
        this.characterType = characterType;
    }

    /** @return the minimum player count required for this card. */
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    /** @return the {@link CharacterType} assigned to this card. */
    public CharacterType getCharacterType() {
        return characterType;
    }
}