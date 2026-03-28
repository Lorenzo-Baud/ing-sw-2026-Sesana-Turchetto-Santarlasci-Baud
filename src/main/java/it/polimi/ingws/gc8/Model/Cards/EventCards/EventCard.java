package it.polimi.ingws.gc8.Model.Cards.EventCards;

import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.List;

// TODO: creare e implementare il metodo getPrestigeBonus
/**
 * Represents an Event card in the Tribe deck.
 * Pure data object — all resolution logic lives in MesosGame.
 */
public abstract class EventCard extends Card {

    private final boolean isFinal;
    private final EventType eventType;

    public EventCard(String front, String back, Era era, boolean isFinal, EventType eventType) {
        super(front, back, era);
        this.isFinal = isFinal;
        this.eventType = eventType;
    }

    public abstract void resolveEvent(List<Player> players);

    // Getters
    public boolean isFinal() {
        return isFinal;
    }

    public EventType getEventType() {
        return eventType;
    }

}
