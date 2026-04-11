package it.polimi.ingws.gc8.Controller.actions;

import java.io.Serializable;

/**
 * Base interface for all actions a player can request.
 * Each concrete action carries the playerId that requested it,
 * plus action-specific data.
 * Serializable so actions can be sent over the network.
 */
public interface PlayerAction extends Serializable {

    /**
     * @return the ID of the player who requested this action
     */
    int getPlayerId();
}
