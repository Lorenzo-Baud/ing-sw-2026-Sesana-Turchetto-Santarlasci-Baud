package it.polimi.ingws.gc8.Controller.actions;

/**
 * Action: a player takes food instead of a card.
 * Valid during the ACTION_RESOLUTION phase (tile A, 5-player games).
 */
public class TakeFoodAction implements PlayerAction {

    private static final long serialVersionUID = 1L;

    /** Player id */
    private final int playerId;

    /**
     * Default constructor for TakeFoodAction
     * @param playerId player id
     */
    public TakeFoodAction(int playerId) {

        this.playerId = playerId;
    }

    /**
     * Returns player id
     * @return player id
     */
    @Override
    public int getPlayerId() {
        return playerId;
    }
}
