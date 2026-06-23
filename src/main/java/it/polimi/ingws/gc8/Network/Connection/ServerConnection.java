package it.polimi.ingws.gc8.Network.Connection;

import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;

import java.util.List;

public interface ServerConnection {

    /** Connects to the server */
    void connect(String host, int port) throws Exception;

    /** Returns list of matches */
    List<MatchInfo> listMatches() throws Exception;

    /** Creates a match */
    JoinResult createMatch(String matchName, String nickname) throws Exception;

    /** Method to join a match */
    JoinResult joinMatch(String matchId, String nickname) throws Exception;

    /** Sets expected number of players inside a match */
    void setExpectedPlayers(int n) throws Exception;

    /** Requests to start a game */
    void requestStartGame() throws Exception;

    /** Sends an action to the server */
    void sendAction(Object action);

    /**
     * FA3: replies to a server heartbeat ({@code PingMessage}). Transport-specific:
     * RMI invokes a remote method, TCP writes a {@code PongMessage} on the stream.
     */
    void pong() throws Exception;

    /** Receives message from the server */
    Object receive() throws Exception;

    /** Disconnects from the server */
    void disconnect();

    /** Returns true if the client is connected to the server */
    boolean isConnected();
}
