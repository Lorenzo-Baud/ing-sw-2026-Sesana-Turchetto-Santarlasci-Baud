package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

/**
 * Request to join a match
 * @param matchId the id of the match to join
 * @param nickname the nickname of the player joining the match
 */
public record JoinMatchRequest(String matchId, String nickname) implements Serializable {}