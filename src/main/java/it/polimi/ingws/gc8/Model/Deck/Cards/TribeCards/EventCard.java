package it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards;

import java.util.function.Function;

public class EventCard extends TribeCard{

    private final boolean isFinal;
    // Event
    private Function<Integer, Integer> method;

    // Constructor
    public EventCard(String front, String back, Era era, boolean isFinal) {
        super(front, back, era);
        this.isFinal = isFinal;
    }

    // Getters/setters
    public boolean isFinal() {
        return isFinal;
    }
}
