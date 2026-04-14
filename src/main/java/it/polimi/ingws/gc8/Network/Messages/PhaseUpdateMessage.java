package it.polimi.ingws.gc8.Network.Messages;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

/**
 * Notifies clients that the game phase has changed.
 */
public class PhaseUpdateMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;

    private final GamePhase newPhase;

    public PhaseUpdateMessage(GamePhase newPhase) {
        this.newPhase = newPhase;
    }

    public GamePhase getNewPhase() {
        return newPhase;
    }
}