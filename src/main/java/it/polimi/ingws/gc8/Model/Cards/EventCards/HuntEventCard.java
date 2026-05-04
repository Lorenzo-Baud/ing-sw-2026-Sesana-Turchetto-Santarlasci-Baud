package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Tribe;

import java.util.List;

/**
 * Represents a Hunt Event card.
 * When resolved, this event grants resources and prestige points based on the
 * number of Hunters in a player's tribe. It also triggers event-specific
 * bonuses from active building effects.
 */
public class HuntEventCard extends EventCard {

    /** The amount of prestige points awarded for each Hunter in the tribe. */
    private final int prestigeBonus;

    /**
     * Constructs a HuntEventCard with the specified prestige multiplier.
     *
     * @param front         the identifier for the front asset.
     * @param back          the identifier for the back asset.
     * @param era           the game era associated with this event.
     * @param isFinal       true if this event occurs during final era scoring.
     * @param eventType     the category of the event.
     * @param prestigeBonus the prestige points granted per Hunter.
     */
    public HuntEventCard(String front, String back, Era era, boolean isFinal, EventType eventType, int prestigeBonus) {
        super(front, back, era, isFinal, eventType);
        this.prestigeBonus = prestigeBonus;
    }

    /**
     * Resolves the hunting event for all players.
     * <p>
     * For each Hunter in their tribe, a player receives:
     * <ul>
     *   <li>1 Unit of Food.</li>
     *   <li>{@code prestigeBonus} Prestige Points.</li>
     * </ul>
     * Additionally, this method invokes {@code tribe.triggerOnEvent} to activate
     * any passive bonuses (e.g., from buildings) that react to hunting events.
     * </p>
     *
     * @param players the list of players participating in the event.
     */
    @Override
    public void resolveEvent(List<Player> players) {
        for (Player player : players) {
            Tribe tribe = player.getTribe();
            int hunters = tribe.countByType(CharacterType.HUNTER);

            // Standard reward distribution
            player.addFood(hunters);
            player.addPrestigePoints(hunters * prestigeBonus);

            // Execute conditional effects triggered by this event
            tribe.triggerOnEvent(player, this);
        }
    }

    /** @return the prestige bonus multiplier per Hunter. */
    public int getPrestigeBonus() {
        return prestigeBonus;
    }
}
