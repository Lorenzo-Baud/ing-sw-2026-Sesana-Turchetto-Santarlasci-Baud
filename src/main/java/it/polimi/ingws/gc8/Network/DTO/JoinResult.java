package it.polimi.ingws.gc8.Network.DTO;

import java.io.Serializable;

public record JoinResult(String matchId, int playerId, boolean isHost) implements Serializable {}