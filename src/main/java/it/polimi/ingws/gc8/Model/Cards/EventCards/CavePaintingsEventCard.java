package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the "Cave Paintings" Event Card.
 * This event rewards players who have invested in Artists with prestige points,
 * while penalizing those who fail to meet a minimum Artist count requirement.
 */
public class CavePaintingsEventCard extends EventCard {

    private static final long serialVersionUID = 1L;

    /** The minimum threshold of Artist cards required to receive the prestige bonus. */
    private final int bonusArtists;

    /** The threshold below which players suffer a prestige point penalty. */
    private final int malusArtists;

    /** The multiplier used to calculate prestige points awarded per Artist. */
    private final int bonusPoints;

    /** The fixed amount of prestige points deducted if the malus condition is met. */
    private final int malusPoints;

    /**
     * Constructs a CavePaintingsEventCard with specific bonus/malus thresholds and values.
     *
     * @param front        the identifier for the front asset.
     * @param back         the identifier for the back asset.
     * @param era          the game era this event belongs to.
     * @param isFinal      true if this is a final era event.
     * @param eventType    the classification of the event.
     * @param bonusArtists minimum artists for bonus eligibility.
     * @param malusArtists threshold for penalty application.
     * @param bonusPoints  points awarded per Artist card.
     * @param malusPoints  points deducted as a penalty.
     */
    public CavePaintingsEventCard(String front, String back, Era era, boolean isFinal, EventType eventType,
                                  int bonusArtists, int malusArtists, int bonusPoints, int malusPoints) {
        super(front, back, era, isFinal, eventType);
        this.bonusArtists = bonusArtists;
        this.malusArtists = malusArtists;
        this.bonusPoints = bonusPoints;
        this.malusPoints = malusPoints;
    }

    /**
     * Executes the event logic for all players.
     * <p>
     * - Players with at least {@code bonusArtists} receive {@code bonusPoints} multiplied
     *   by their total Artist count.
     * - Players with fewer than {@code malusArtists} lose {@code malusPoints}.
     * </p>
     *
     * @param players the list of active players in the game.
     */
    @Override
    public void resolveEvent(List<Player> players) {
        for(Player p : players) {
            int artistCount = p.getTribe().countByType(CharacterType.ARTIST);

            // Reward logic: scaling bonus based on artist count
            if(artistCount >= bonusArtists) {
                p.addPrestigePoints(bonusPoints * artistCount);
            }

            // Penalty logic: fixed point deduction
            if(artistCount < malusArtists) {
                p.removePrestigePoints(malusPoints);
            }
        }
    }

    /** @return the minimum artist count for bonus eligibility. */
    public int getBonusArtists() { return bonusArtists; }

    /** @return the artist threshold for penalty application. */
    public int getMalusArtists() { return malusArtists; }

    /** @return the prestige points awarded per artist. */
    public int getBonusPoints() { return bonusPoints; }

    /** @return the prestige points deducted as penalty. */
    public int getMalusPoints() { return malusPoints; }
}
