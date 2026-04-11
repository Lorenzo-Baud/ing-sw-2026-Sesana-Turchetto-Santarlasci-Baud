package it.polimi.ingws.gc8.Controller.actions;

/**
 * Action: a player returns their totem to the turn order track.
 * Valid during the ACTION_RESOLUTION phase, after picking cards.
 */
public class ReturnTotemAction implements PlayerAction {

    private static final long serialVersionUID = 1L;

    private final int playerId;

    public ReturnTotemAction(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public int getPlayerId() {
        return playerId;
    }
}
