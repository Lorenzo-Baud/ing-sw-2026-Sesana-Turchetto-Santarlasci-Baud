package it.polimi.ingws.gc8.Network.DTO;

import java.io.Serializable;

/**
 * Result of a join/reconnect.
 *
 * @param matchId     the match the client is now attached to
 * @param playerId    the per-match player id
 * @param isHost      whether this player is the lobby host
 * @param reconnected true if this was a reconnection to an already-started match
 *                    (FA2/FA3): the client must skip lobby setup and wait for
 *                    the full snapshot instead of asking for the player count.
 */
public record JoinResult(String matchId, int playerId, boolean isHost, boolean reconnected)
        implements Serializable {}
