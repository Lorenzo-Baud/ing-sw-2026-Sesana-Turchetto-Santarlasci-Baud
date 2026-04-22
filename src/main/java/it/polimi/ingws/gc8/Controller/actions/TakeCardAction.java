package it.polimi.ingws.gc8.Controller.actions;

import it.polimi.ingws.gc8.Model.Enum.CardRow;

import java.util.Objects;

/**
 * Action: a player takes a card (Character or Building) from a row.
 * Valid during the ACTION_RESOLUTION phase.
 */
public class TakeCardAction implements PlayerAction {

    private static final long serialVersionUID = 1L;

    private final int playerId;
    private final String cardName;
    private final CardRow row;

    public TakeCardAction(int playerId, String cardName, CardRow row) {
        this.playerId = playerId;
        this.cardName = cardName;
        this.row = Objects.requireNonNull(row, "row");
    }

    @Override
    public int getPlayerId() {
        return playerId;
    }

    /**
     * @return the name/identifier of the chosen card
     */
    public String getCardName() {
        return cardName;
    }

    /**
     * @return the board row containing the chosen card
     */
    public CardRow getRow() {
        return row;
    }
}
