package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Ritual Event card.
 * This event triggers a competitive check based on Shaman icon majorities.
 * The player(s) with the highest number of Shaman icons receive a bonus,
 * while those with the lowest count suffer a prestige penalty.
 */
public class RitualEventCard extends EventCard {

    private static final long serialVersionUID = 1L;

    /** Prestige points deducted from the player(s) with the fewest Shaman icons. */
    private final int prestigeMalus;

    /** Prestige points awarded to the player(s) with the most Shaman icons. */
    private final int prestigeBonus;

    /**
     * Constructs a RitualEventCard with specific bonus and penalty values.
     *
     * @param front         the identifier for the front asset.
     * @param back          the identifier for the back asset.
     * @param era           the game era associated with this event.
     * @param isFinal       true if this is a final era scoring event.
     * @param eventType     the category of the event.
     * @param prestigeBonus points granted for the majority.
     * @param prestigeMalus points deducted for the minority.
     */
    public RitualEventCard(String front, String back, Era era, boolean isFinal, EventType eventType,
                           int prestigeBonus, int prestigeMalus) {
        super(front, back, era, isFinal, eventType);
        this.prestigeMalus = prestigeMalus;
        this.prestigeBonus = prestigeBonus;
    }

    /**
     * Resolves the ritual event by comparing Shaman icon counts across all players.
     * <p>
     * The method performs two passes:
     * <ul>
     *   <li>Determines the global maximum and minimum icon counts.</li>
     *   <li>Identifies all players matching those extremes to handle ties correctly.</li>
     * </ul>
     * Finally, {@code prestigeBonus} is added to the leaders and {@code prestigeMalus}
     * is removed from the laggards.
     * </p>
     *
     * @param players the list of players to evaluate.
     */
    @Override
    public void resolveEvent(List<Player> players) {
        if (players == null || players.isEmpty()) return;

        ArrayList<Player> topPlayers = new ArrayList<>();
        ArrayList<Player> bottomPlayers = new ArrayList<>();

        int max = players.get(0).getTribe().getTotalShamanIcons();
        int min = players.get(0).getTribe().getTotalShamanIcons();

        // First pass: find global extrema
        for(Player p : players) {
            int currentIcons = p.getTribe().getTotalShamanIcons();
            if(currentIcons > max) max = currentIcons;
            if(currentIcons < min) min = currentIcons;
        }

        // Second pass: identify all players in tie for max and min
        for(Player p : players) {
            int currentIcons = p.getTribe().getTotalShamanIcons();
            if(currentIcons == max) topPlayers.add(p);
            if(currentIcons == min) bottomPlayers.add(p);
        }

        // Apply rewards
        for(Player p : topPlayers) {
            p.addPrestigePoints(prestigeBonus);
        }

        // Apply penalties
        for(Player p : bottomPlayers) {
            p.removePrestigePoints(prestigeMalus);
        }
    }

    /** @return the prestige points granted for the majority. */
    public int getPrestigeBonus() { return prestigeBonus; }

    /** @return the prestige points deducted for the minority. */
    public int getPrestigeMalus() { return prestigeMalus; }
}
