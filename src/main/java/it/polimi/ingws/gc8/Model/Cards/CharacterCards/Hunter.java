package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

/**
 * Represents the Hunter character card.
 * Hunters are specialized units that typically provide advantages during
 * hunting expeditions or resource gathering phases.
 */
public class Hunter extends CharacterCard {

    private static final long serialVersionUID = 1L;

    /**
     * Indicates whether this specific hunter card grants an additional
     * hunting bonus or special capability.
     */
    private final boolean bonus;

    /**
     * Constructs a Hunter card with the specified attributes and bonus status.
     *
     * @param front           the identifier for the front-facing asset.
     * @param back            the identifier for the back-facing asset.
     * @param era             the game era associated with this card.
     * @param numberOfPlayers the player count threshold for card availability.
     * @param bonus           {@code true} if the card provides a special hunting advantage.
     */
    public Hunter(String front, String back, Era era, int numberOfPlayers, boolean bonus) {
        super(front, back, era, numberOfPlayers, CharacterType.HUNTER);
        this.bonus = bonus;
    }

    /**
     * Checks if this hunter provides a specialized hunting bonus.
     *
     * @return {@code true} if the bonus is active, {@code false} otherwise.
     */
    public boolean hasBonus() {
        return bonus;
    }
}
