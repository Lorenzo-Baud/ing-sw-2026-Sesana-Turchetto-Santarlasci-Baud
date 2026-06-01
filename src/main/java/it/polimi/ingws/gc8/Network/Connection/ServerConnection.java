package it.polimi.ingws.gc8.Network.Connection;

import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;

import java.util.List;

public interface ServerConnection {

    void connect(String host, int port) throws Exception;

    List<MatchInfo> listMatches() throws Exception;

    JoinResult createMatch(String nickname) throws Exception;

    JoinResult joinMatch(String matchId, String nickname) throws Exception;

    void setExpectedPlayers(int n) throws Exception;

    void requestStartGame() throws Exception;

    void sendAction(Object action);

    Object receive() throws Exception;

    void disconnect();

    boolean isConnected();
}
