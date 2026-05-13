package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

/**
 * Lobby command sent by the TCP host to ask the server to move the lobby
 * into the game phase. The TCP counterpart of {@code requestStartGame(...)}
 * on the RMI {@code RemoteServer}.
 */
public record RequestStartGameCommand() implements Serializable {}
