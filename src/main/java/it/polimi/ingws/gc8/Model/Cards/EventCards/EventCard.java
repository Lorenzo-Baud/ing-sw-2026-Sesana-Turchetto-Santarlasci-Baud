package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.List;

/**
 * Abstract base class for Event cards within the Tribe deck.
 *
 * This class defines the structure for game events. While it stores core event data,
 * the specific outcome of each event is delegated to subclasses via the
 * {@link #resolveEvent(List)} method, following a Strategy-like pattern.
 */
public abstract class EventCard extends Card {

    /** Persistence: explicit serialVersionUID for stable deserialization. */
    private static final long serialVersionUID = 1L;

    /** Indicates if this event triggers only during the final scoring phase of an Era. */
    private final boolean isFinal;

    /** The specific category of the event, used for filtering or UI representation. */
    private final EventType eventType;

    /**
     * Constructs an EventCard with the specified properties and assets.
     *
     * @param front     the identifier for the front asset.
     * @param back      the identifier for the back asset.
     * @param era       the game era associated with this event.
     * @param isFinal   true if the event is a final era trigger.
     * @param eventType the classification of the event.
     */
    public EventCard(String front, String back, Era era, boolean isFinal, EventType eventType) {
        super(front, back, era, CardType.EVENT);
        this.isFinal = isFinal;
        this.eventType = eventType;
    }

    /**
     * Executes the specific logic associated with this event.
     * Implementation details vary by subclass (e.g., scoring bonuses, penalties,
     * or resource redistribution).
     *
     * @param players the list of players affected by the event.
     */
    public abstract void resolveEvent(List<Player> players);

    /** @return {@code true} if this is a final event, {@code false} otherwise. */
    public boolean isFinal() {
        return isFinal;
    }

    /** @return the {@link EventType} of this card. */
    public EventType getEventType() {
        return eventType;
    }
}