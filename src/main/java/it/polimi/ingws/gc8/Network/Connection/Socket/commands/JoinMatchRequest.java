package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

public record JoinMatchRequest(String matchId, String nickname) implements Serializable {}