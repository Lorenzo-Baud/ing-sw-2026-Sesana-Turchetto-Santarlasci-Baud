package it.polimi.ingws.gc8.Network.Messages;

import it.polimi.ingws.gc8.Network.DTO.GameDTO;

/**
 * Contains the full state of the game.
 * Used for initial setup, reconnections, or full synchronizations.
 */
public class GameSnapshotMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;

    private final GameDTO gameState;

    public GameSnapshotMessage(GameDTO gameState) {
        this.gameState = gameState;
    }

    public GameDTO getGameState() {
        return gameState;
    }
}