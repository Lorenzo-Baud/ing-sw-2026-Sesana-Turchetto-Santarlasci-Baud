package it.polimi.ingws.gc8.Network.Messages;

/**
 * Sent to players in the waiting room to update them on the joined players count.
 */
public class LobbyUpdateMessage implements NetworkMessage {

    private final int currentPlayers;
    private final int expectedPlayers;

    public LobbyUpdateMessage(int currentPlayers, int expectedPlayers) {
        this.currentPlayers = currentPlayers;
        this.expectedPlayers = expectedPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getExpectedPlayers() {
        return expectedPlayers;
    }
}