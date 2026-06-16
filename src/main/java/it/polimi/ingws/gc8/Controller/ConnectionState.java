package it.polimi.ingws.gc8.Controller;

/**
 * Connection state of a player within an in-progress match (FA3).
 * Tracked per playerId by {@link MainController} so the game can skip
 * disconnected players and suspend/resume the match.
 */
public enum ConnectionState {
    CONNECTED,
    DISCONNECTED
}
