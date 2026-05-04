package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

/**
 * Represents the Builder character card.
 * Builders typically provide immediate resource bonuses (food) and contribute
 * to the player's total prestige points.
 */
public class Builder extends CharacterCard {

    /** Immediate food bonus granted upon acquiring or activating this card. */
    private final int foodBonus;

    /** Prestige points provided by this card for final scoring. */
    private final int prestigePoints;

    /**
     * Constructs a Builder card with specific resource and prestige values.
     *
     * @param front           the path or identifier for the front image.
     * @param back            the path or identifier for the back image.
     * @param era             the game era this card belongs to.
     * @param foodBonus       the amount of food instantly granted by this builder.
     * @param prestigePoints  the amount of prestige points this card is worth.
     * @param numberOfPlayers the player count requirement for this card's availability.
     */
    public Builder(String front, String back, Era era, int foodBonus, int prestigePoints, int numberOfPlayers) {
        super(front, back, era, numberOfPlayers, CharacterType.BUILDER);
        this.foodBonus = foodBonus;
        this.prestigePoints = prestigePoints;
    }

    /** @return the immediate food bonus value. */
    public int getFoodBonus() {
        return foodBonus;
    }

    /** @return the total prestige points value. */
    public int getPrestigePoints() {
        return prestigePoints;
    }
}