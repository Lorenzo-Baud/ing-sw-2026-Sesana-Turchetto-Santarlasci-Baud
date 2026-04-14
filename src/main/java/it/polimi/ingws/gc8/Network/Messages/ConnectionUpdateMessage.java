package it.polimi.ingws.gc8.Network.Messages;

/**
 * Notifies the players if someone disconnected or reconnected.
 */
public class ConnectionUpdateMessage implements NetworkMessage {

    private final String nickname;
    private final boolean isConnected; // true = reconnected, false = disconnected

    public ConnectionUpdateMessage(String nickname, boolean isConnected) {
        this.nickname = nickname;
        this.isConnected = isConnected;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isConnected() {
        return isConnected;
    }
}