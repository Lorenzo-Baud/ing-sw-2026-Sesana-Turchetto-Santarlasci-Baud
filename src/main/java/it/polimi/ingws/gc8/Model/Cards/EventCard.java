package it.polimi.ingws.gc8.Model.Cards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;

/**
 * Represents an Event card in the Tribe deck.
 * Pure data object — all resolution logic lives in MesosGame.
 */
public class EventCard extends Card {

    private final EventType eventType;
    private final boolean isFinal;

    // Sustenance / Ritual
    private final int prestigeMalus;

    // Hunt / Ritual
    private final int prestigeBonus;

    // Cave Paintings
    private final int bonusArtists;
    private final int malusArtists;
    private final int bonusPoints;
    private final int malusPoints;

    public EventCard(String front, String back, Era era, boolean isFinal, EventType eventType,
                     int prestigeBonus, int prestigeMalus,
                     int bonusArtists, int malusArtists,
                     int bonusPoints, int malusPoints) {
        super(front, back, era);
        this.isFinal = isFinal;
        this.eventType = eventType;
        this.prestigeBonus = prestigeBonus;
        this.prestigeMalus = prestigeMalus;
        this.bonusArtists = bonusArtists;
        this.malusArtists = malusArtists;
        this.bonusPoints = bonusPoints;
        this.malusPoints = malusPoints;
    }

    // Getters

    public boolean isFinal() {
        return isFinal;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    public int getPrestigeBonus() {
        return prestigeBonus;
    }

    public int getBonusArtists() {
        return bonusArtists;
    }

    public int getMalusArtists() {
        return malusArtists;
    }

    public int getBonusPoints() {
        return bonusPoints;
    }

    public int getMalusPoints() {
        return malusPoints;
    }
}
