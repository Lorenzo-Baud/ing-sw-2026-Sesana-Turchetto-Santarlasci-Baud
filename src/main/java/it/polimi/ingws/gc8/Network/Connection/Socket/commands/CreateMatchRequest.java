package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

/**
 * Request to create a new match. {@code matchName} becomes the match id
 * (must be unique among open matches), {@code nickname} is the host's name.
 */
public record CreateMatchRequest(String matchName, String nickname) implements Serializable {}