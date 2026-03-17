package it.polimi.ingws.gc8.Model.Deck.Cards;

import model.Enum.Era;

public abstract class Card {

    protected final Era era;
    protected final String front;
    protected final String back;

    public Card(String front, String back, Era era) {
        this.front = front;
        this.back = back;
        this.era = era;
    }

    // Getters
    public String getFront() {
        return front;
    }
    public String getBack() {
        return back;
    }
    public Era getEra() {return era;}

}
