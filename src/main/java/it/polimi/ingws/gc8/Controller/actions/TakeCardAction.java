package it.polimi.ingws.gc8.Controller.actions;

/**
 * Action: a player takes a card (Character or Building) from a row.
 * Valid during the ACTION_RESOLUTION phase.
 */
public class TakeCardAction implements PlayerAction {

    private static final long serialVersionUID = 1L;

    private final int playerId;
    private final String cardName;
    private final int row; // 0 = lower row, 1 = upper row

    public TakeCardAction(int playerId, String cardName, int row) {
        this.playerId = playerId;
        this.cardName = cardName;
        this.row = row;
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
     * @return 0 for lower row, 1 for upper row
     */
    public int getRow() {
        return row;
    }
}
