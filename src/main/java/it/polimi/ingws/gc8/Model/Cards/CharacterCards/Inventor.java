package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

/**
 * Represents the Inventor character card.
 * Inventors typically provide specialized icons or symbols required to
 * satisfy technology requirements or unlock advanced craftable items.
 */
public class Inventor extends CharacterCard {

    private static final long serialVersionUID = 1L;

    /**
     * Icon associated with the inventor
     */
    private final int icon;

    /**
     * Constructs an Inventor card with a specific icon.
     *
     * @param front           the identifier for the front-facing asset.
     * @param back            the identifier for the back-facing asset.
     * @param era             the game era associated with this card.
     * @param numberOfPlayers the player count threshold for card availability.
     * @param icon            the icon associated with this card.
     */
    public Inventor(String front, String back, Era era, int numberOfPlayers, int icon) {
        super(front, back, era, numberOfPlayers, CharacterType.INVENTOR);
        this.icon = icon;
    }

    /**
     * Returns the icon associated with this inventor.
     *
     * @return the icon
     */
    public int getIcon() {
        return icon;
    }
}
