package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Tribe;

import java.util.List;

/**
 * Represents a Sustenance Event card.
 * This event forces all players to pay food resources to support their tribe members.
 * Failure to provide sufficient food results in a prestige point penalty proportional
 * to the amount of unfed characters.
 */
public class SustenanceEventCard extends EventCard {

    /** The penalty multiplier applied to prestige points for each unit of missing food. */
    private final int prestigeMalus;

    /**
     * Constructs a SustenanceEventCard with a specific penalty value.
     *
     * @param front         the identifier for the front asset.
     * @param back          the identifier for the back asset.
     * @param era           the game era associated with this event.
     * @param isFinal       true if this is a final era sustenance check.
     * @param eventType     the category of the event.
     * @param prestigeMalus points lost per missing food unit.
     */
    public SustenanceEventCard(String front, String back, Era era, boolean isFinal, EventType eventType,
                               int prestigeMalus) {
        super(front, back, era, isFinal, eventType);
        this.prestigeMalus = prestigeMalus;
    }

    /**
     * Resolves the sustenance requirements for all players.
     * <p>
     * The total food cost is calculated as:
     * {@code Max(0, TotalCharacters - CollectorDiscounts - BuildingReductions)}.
     * </p>
     * <ul>
     *   <li>If the player has enough food, the cost is deducted from their supply.</li>
     *   <li>If the food is insufficient, the player's food supply is emptied, and they
     *       suffer a prestige penalty equal to {@code missingFood * prestigeMalus}.</li>
     * </ul>
     *
     * @param players the list of players to process.
     */
    @Override
    public void resolveEvent(List<Player> players) {
        for (Player player : players) {
            Tribe tribe = player.getTribe();

            int totalCharacters = tribe.getTotalCharacters();
            int collectorDiscount = tribe.getCollectorDiscount();
            int buildingDiscount = tribe.getTotalSustenanceReduction(player);

            // Calculate net food requirement after all modifiers
            int foodToPay = Math.max(0, totalCharacters - collectorDiscount - buildingDiscount);

            if (foodToPay <= player.getFood()) {
                // Sufficient resources
                player.removeFood(foodToPay);
            } else {
                // Famine logic
                int unfed = foodToPay - player.getFood();
                player.removeFood(player.getFood()); // Drain remaining food
                player.removePrestigePoints(unfed * prestigeMalus);
            }
        }
    }

    /** @return the prestige penalty multiplier. */
    public int getPrestigeMalus() {
        return prestigeMalus;
    }
}