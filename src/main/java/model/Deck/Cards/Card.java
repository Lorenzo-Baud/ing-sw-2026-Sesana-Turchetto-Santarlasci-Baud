package model.Deck.Cards;

public abstract class Card {

    // Era enum
    public enum Era {
        ERA_1, ERA_2, ERA_3
    }

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
