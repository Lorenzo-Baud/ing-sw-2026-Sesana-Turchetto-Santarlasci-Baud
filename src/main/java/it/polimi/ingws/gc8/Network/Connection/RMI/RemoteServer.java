package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteServer extends Remote {

    /** Returns list of open matches */
    List<MatchInfo> listOpenMatches() throws RemoteException;

    /** creates a match */
    JoinResult createMatch(String matchName, String nickname, RemoteView clientView) throws RemoteException;

    /** Method to join a match*/
    JoinResult joinMatch(String matchId, String nickname, RemoteView clientView) throws RemoteException;

    /** Sets expected number of players for a match */
    void setExpectedPlayers(int playerId, String matchId, int n) throws RemoteException;

    /** Request game startup */
    void requestStartGame(int playerId, String matchId) throws RemoteException;

    /** Submits action to server */
    void submitAction(PlayerAction action, String matchId) throws RemoteException;

    /** FA3: heartbeat reply from the client to keep its liveness timer fresh. */
    void pong(int playerId, String matchId) throws RemoteException;

    /** Logs out */
    void logout(int playerId, String matchId) throws RemoteException;
}