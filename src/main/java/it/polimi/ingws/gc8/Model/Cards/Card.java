package it.polimi.ingws.gc8.Model.Cards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;

public abstract class Card {

    protected final Era era;
    protected final String front;
    protected final String back;

    public Card(String front, String back, Era era) {
        this.front = front;
        this.back = back;
        this.era = era;
    }

    // Method to return DTO object
    public CardDTO toDTO() {
        return new CardDTO(this.front, this.back, false);
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
