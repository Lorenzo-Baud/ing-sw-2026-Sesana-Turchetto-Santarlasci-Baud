package it.polimi.ingws.gc8.Controller.actions;

import it.polimi.ingws.gc8.Model.Enum.CardRow;

import java.util.Objects;

/**
 * Action: a player takes a card (Character or Building) from a row.
 * Valid during the ACTION_RESOLUTION phase.
 *
 * The card is identified by its position in the row's available list,
 * not by name: front/back strings are not guaranteed to be unique
 * (and currently empty in the resource files for Character/Event).
 */
public class TakeCardAction implements PlayerAction {

    private static final long serialVersionUID = 1L;

    private final int playerId;
    private final int cardIndex;
    private final CardRow row;

    public TakeCardAction(int playerId, int cardIndex, CardRow row) {
        this.playerId = playerId;
        this.cardIndex = cardIndex;
        this.row = Objects.requireNonNull(row, "row");
    }

    @Override
    public int getPlayerId() {
        return playerId;
    }

    /**
     * @return the 0-based index of the chosen card in the row's available list
     */
    public int getCardIndex() {
        return cardIndex;
    }

    /**
     * @return the board row containing the chosen card
     */
    public CardRow getRow() {
        return row;
    }
}
