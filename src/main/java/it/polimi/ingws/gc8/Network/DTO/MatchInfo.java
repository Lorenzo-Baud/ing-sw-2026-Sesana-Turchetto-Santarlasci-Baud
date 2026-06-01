package it.polimi.ingws.gc8.Network.DTO;

import java.io.Serializable;

public record MatchInfo(String matchId, String hostNickname,
                        int currentPlayers, int expectedPlayers,
                        MatchStatus status) implements Serializable {}