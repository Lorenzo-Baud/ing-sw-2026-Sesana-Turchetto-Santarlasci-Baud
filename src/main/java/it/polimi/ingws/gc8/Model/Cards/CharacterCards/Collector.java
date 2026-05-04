package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;

/**
 * Represents the Collector character card.
 * Collectors specialize in resource optimization, typically providing
 * discounts on food requirements or sustenance costs.
 */
public class Collector extends CharacterCard {

    /** The reduction in food cost granted by this collector. */
    private final int foodDiscount;

    /**
     * Constructs a Collector card with a specific resource discount.
     *
     * @param front           the identifier for the front asset.
     * @param back            the identifier for the back asset.
     * @param era             the game era this card belongs to.
     * @param numberOfPlayers the player count requirement for availability.
     * @param foodDiscount    the amount of food saved when this card's effect is active.
     */
    public Collector(String front, String back, Era era, int numberOfPlayers, int foodDiscount) {
        super(front, back, era, numberOfPlayers, CharacterType.COLLECTOR);
        this.foodDiscount = foodDiscount;
    }

    /** @return the food discount value provided by this card. */
    public int getFoodDiscount() {
        return foodDiscount;
    }
}
