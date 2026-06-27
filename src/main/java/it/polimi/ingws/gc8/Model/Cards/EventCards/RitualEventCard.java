package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

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
     * Rewards are applied to every player on the maximum and penalties to every
     * player on the minimum; when all players are tied each one first gains and
     * then loses prestige, as required by the rulebook.
     * </p>
     * <p>
     * Building effects relevant to the Shamanic Ritual are honoured:
     * <ul>
     *   <li>extra shaman icons ({@link it.polimi.ingws.gc8.Model.Tribe#getTotalShamanIconBonus()})
     *       count towards the comparison;</li>
     *   <li>a shield ({@link it.polimi.ingws.gc8.Model.Tribe#hasShamanShield()}) cancels the
     *       minority penalty for its owner;</li>
     *   <li>a doubling building ({@link it.polimi.ingws.gc8.Model.Tribe#hasShamanDouble()})
     *       doubles the reward of a sole leader (and, with
     *       {@link it.polimi.ingws.gc8.Model.Tribe#hasShamanGainOnTie()}, of a shared lead too).</li>
     * </ul>
     * </p>
     *
     * @param players the list of players to evaluate.
     */
    @Override
    public void resolveEvent(List<Player> players) {
        if (players == null || players.isEmpty()) return;

        int max = effectiveShamanIcons(players.get(0));
        int min = max;

        // First pass: find global extrema over the effective icon counts
        for(Player p : players) {
            int currentIcons = effectiveShamanIcons(p);
            if(currentIcons > max) max = currentIcons;
            if(currentIcons < min) min = currentIcons;
        }

        // Count how many players share the maximum, to tell a sole leader from a tie
        int leadersCount = 0;
        for(Player p : players) {
            if(effectiveShamanIcons(p) == max) leadersCount++;
        }
        boolean soleLeader = leadersCount == 1 && max > min;

        // Apply rewards to the leaders (sole leader doubling handled in gainFor)
        for(Player p : players) {
            if(effectiveShamanIcons(p) == max) {
                p.addPrestigePoints(gainFor(p, soleLeader));
            }
        }

        // Apply penalties to the laggards, unless a building shields them
        for(Player p : players) {
            if(effectiveShamanIcons(p) == min && !p.getTribe().hasShamanShield()) {
                p.removePrestigePoints(prestigeMalus);
            }
        }
    }

    /**
     * Effective shaman icons of a player: icons printed on Shaman cards plus any
     * extra icons granted by buildings during the Shamanic Ritual.
     *
     * @param player the player to evaluate.
     * @return the effective number of shaman icons.
     */
    private int effectiveShamanIcons(Player player) {
        return player.getTribe().getTotalShamanIcons()
                + player.getTribe().getTotalShamanIconBonus();
    }

    /**
     * Prestige awarded to a leading player. A doubling building doubles the reward
     * when the player has strictly more icons than everyone else; a building that
     * also grants prestige on ties extends the doubling to a shared lead.
     *
     * @param player     the leading player.
     * @param soleLeader whether the player is the unique maximum.
     * @return the prestige points to award.
     */
    private int gainFor(Player player, boolean soleLeader) {
        boolean doubled = player.getTribe().hasShamanDouble()
                && (soleLeader || player.getTribe().hasShamanGainOnTie());
        return doubled ? prestigeBonus * 2 : prestigeBonus;
    }

    /** @return the prestige points granted for the majority. */
    public int getPrestigeBonus() { return prestigeBonus; }

    /** @return the prestige points deducted for the minority. */
    public int getPrestigeMalus() { return prestigeMalus; }
}
