package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

/**
 * Represents the Shaman character card.
 * Shamans provide spiritual icons that are essential for satisfying
 * specific era requirements and unlocking shamanic bonuses.
 */
public class Shaman extends CharacterCard {

    private static final long serialVersionUID = 1L;

    /** The number of shamanic icons provided by this card. */
    private final int nIcons;

    /**
     * Constructs a Shaman card with a specific icon count.
     *
     * @param front           the identifier for the front asset.
     * @param back            the identifier for the back asset.
     * @param era             the game era this card belongs to.
     * @param nIcons          the number of shaman icons granted by this card.
     * @param numberOfPlayers the player count requirement for availability.
     */
    public Shaman(String front, String back, Era era, int nIcons, int numberOfPlayers) {
        super(front, back, era, numberOfPlayers, CharacterType.SHAMAN);
        this.nIcons = nIcons;
    }

    /**
     * Returns the number of shaman icons associated with this card.
     *
     * @return the shaman icon count.
     */
    public int getnIcons() {
        return nIcons;
    }
}
