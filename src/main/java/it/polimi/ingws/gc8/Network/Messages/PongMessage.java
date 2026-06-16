package it.polimi.ingws.gc8.Network.Messages;

/**
 * Application-level heartbeat reply sent by a client in response to a
 * {@link PingMessage} (FA3). Resets the server-side liveness timer for the
 * sending player.
 */
public final class PongMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;
}
