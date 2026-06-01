package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

public record CreateMatchRequest(String nickname) implements Serializable {}