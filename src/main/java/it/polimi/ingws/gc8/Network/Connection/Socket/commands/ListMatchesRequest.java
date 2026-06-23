package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import java.io.Serializable;

/**
 * Command to request the list of matches from the server.
 */
public record ListMatchesRequest() implements Serializable {}