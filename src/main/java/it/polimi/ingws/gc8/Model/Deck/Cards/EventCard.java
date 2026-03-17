<<<<<<<< Updated upstream:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/TribeCards/EventCard.java
package it.polimi.ingws.gc8.Model.Deck.Cards.TribeCards;
========
package model.Deck.Cards;
>>>>>>>> Stashed changes:src/main/java/it/polimi/ingws/gc8/Model/Deck/Cards/EventCard.java

import java.util.function.Function;
import model.Enum.Era;

public class EventCard extends Card{

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
