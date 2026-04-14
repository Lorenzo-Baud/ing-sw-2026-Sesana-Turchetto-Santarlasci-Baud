package it.polimi.ingws.gc8.Network.Messages;

import it.polimi.ingws.gc8.Network.DTO.GameDTO;

/**
 * Notifies the end of the game, carrying the final board state and a summary message.
 */
public class GameOverMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;

    private final String endMessage;
    private final GameDTO finalState;

    public GameOverMessage(String endMessage, GameDTO finalState) {
        this.endMessage = endMessage;
        this.finalState = finalState;
    }

    public String getEndMessage() {
        return endMessage;
    }

    public GameDTO getFinalState() {
        return finalState;
    }
}