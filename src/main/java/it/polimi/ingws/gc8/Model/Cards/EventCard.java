package it.polimi.ingws.gc8.Model.Cards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

import java.util.List;
import java.util.function.Consumer;

public class EventCard extends Card {

    private final EventType eventType;
    private final boolean isFinal;

    // Event
    private final Consumer<List<Player>> method;

    // Constructor
    public EventCard(String front, String back, Era era, boolean isFinal, EventType eventType, Consumer<List<Player>> method) {
        super(front, back, era);
        this.isFinal = isFinal;
        this.eventType = eventType;
        this.method = method;
    }

    // Execute the event's effect on the list of players
    public void executeEvent(List<Player> players) {
        if (this.method != null) {
            this.method.accept(players);
        }
    }

    // Getters/setters
    public boolean isFinal() {
        return isFinal;
    }

    public EventType getEventType() {
        return eventType;
    }

}
