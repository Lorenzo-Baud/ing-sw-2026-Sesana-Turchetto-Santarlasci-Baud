package it.polimi.ingws.gc8.Model.Cards;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;

public abstract class Card {

    /** Era this card belongs to */
    protected final Era era;

    /** Path for the card's front side asset */
    protected final String front;

    /** Path for the card's back side asset */
    protected final String back;

    /**
     * Constructs a new Card with the specified assets and era.
     * @param front resource identifier for the front of the card
     * @param back  resource identifier for the back of the card
     * @param era   era associated with the card
     */
    public Card(String front, String back, Era era) {
        this.front = front;
        this.back = back;
        this.era = era;
    }

    /**
     * Creates a Data Transfer Object (DTO) representing the current state of the card.
     * Used for sending card information over the network while hiding logic.
     * * @return a DTO version of the card
     */
    public abstract CardDTO toDTO();

    /**
     * Returns the path for the front image of the card.
     * * @return the card's front resource string
     */
    public String getFront() {
        return front;
    }

    /**
     * Returns the path for the back image of the card.
     * * @return the card's back resource string
     */
    public String getBack() {
        return back;
    }

    /**
     * Returns the historical era associated with this card.
     * * @return the card's era
     */
    public Era getEra() {
        return era;
    }

}
