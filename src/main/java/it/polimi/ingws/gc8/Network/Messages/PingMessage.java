package it.polimi.ingws.gc8.Network.Messages;

/**
 * Application-level heartbeat sent by the server to a client (FA3).
 * The client replies with a {@link PongMessage}; if no pong arrives within
 * the configured timeout the server treats the client as disconnected,
 * detecting "silent" drops (cable pull, NAT timeout) that produce no FIN.
 */
public final class PingMessage implements NetworkMessage {
    private static final long serialVersionUID = 1L;
}
