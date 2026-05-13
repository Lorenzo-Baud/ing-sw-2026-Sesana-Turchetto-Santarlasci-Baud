package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

/**
 * Lobby command sent by a TCP client as the first message after connecting,
 * to register a nickname with the server. The TCP counterpart of the
 * {@code login(...)} method on the RMI {@code RemoteServer}.
 *
 * @param nickname the alias chosen by the player
 */
public record LoginRequest(String nickname) implements Serializable {}
