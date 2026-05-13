package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

/**
 * Lobby command sent by the TCP host to declare the number of players the
 * match must wait for before starting. The TCP counterpart of
 * {@code setExpectedPlayers(...)} on the RMI {@code RemoteServer}.
 *
 * <p>The player id is not transmitted: the server already knows it from the
 * {@code ClientHandler} that receives the command.</p>
 *
 * @param n the expected number of players
 */
public record SetExpectedPlayersCommand(int n) implements Serializable {}
