package it.polimi.ingws.gc8.Network.Connection.Socket.commands;

import it.polimi.ingws.gc8.Network.DTO.MatchInfo;

import java.io.Serializable;
import java.util.List;

/** List matches response command */
public record ListMatchesResponse(List<MatchInfo> matches) implements Serializable {}