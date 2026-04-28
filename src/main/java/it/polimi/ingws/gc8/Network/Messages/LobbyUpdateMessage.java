package it.polimi.ingws.gc8.Network.Messages;

import java.util.List;

/**
 * Sent to players in the waiting room to update them on the joined players count
 * and the list of nicknames currently in the lobby.
 */
public class LobbyUpdateMessage implements NetworkMessage {

    private final int currentPlayers;
    private final int expectedPlayers;
    private final List<String> nicknames;

    public LobbyUpdateMessage(int currentPlayers, int expectedPlayers, List<String> nicknames) {
        this.currentPlayers = currentPlayers;
        this.expectedPlayers = expectedPlayers;
        this.nicknames = (nicknames == null) ? List.of() : List.copyOf(nicknames);
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getExpectedPlayers() {
        return expectedPlayers;
    }

    public List<String> getNicknames() {
        return nicknames;
    }
}
