package it.polimi.ingws.gc8.Network.DTO;

import java.io.Serializable;

/**
 * Value returned by the server in response to a successful login.
 *
 * @param playerId the id assigned to the just-registered player
 * @param isFirst  true if this player is the first to join the lobby
 *                 (the "host" who decides the expected number of players)
 */
public record LoginResult(int playerId, boolean isFirst) implements Serializable {}
