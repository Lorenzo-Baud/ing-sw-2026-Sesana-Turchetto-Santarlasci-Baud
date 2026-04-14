package it.polimi.ingws.gc8.Network.Messages;

/**
 * Notifies clients about whose turn it is.
 */
public class TurnUpdateMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;

    private final String activePlayerNickname;
    private final int activePlayerId;

    public TurnUpdateMessage(String activePlayerNickname, int activePlayerId) {
        this.activePlayerNickname = activePlayerNickname;
        this.activePlayerId = activePlayerId;
    }

    public String getActivePlayerNickname() {
        return activePlayerNickname;
    }

    public int getActivePlayerId() {
        return activePlayerId;
    }
}
