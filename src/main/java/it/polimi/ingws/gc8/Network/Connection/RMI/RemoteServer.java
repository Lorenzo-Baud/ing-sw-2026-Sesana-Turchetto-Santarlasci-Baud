package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteServer extends Remote {

    List<MatchInfo> listOpenMatches() throws RemoteException;

    JoinResult createMatch(String nickname, RemoteView clientView) throws RemoteException;

    JoinResult joinMatch(String matchId, String nickname, RemoteView clientView) throws RemoteException;

    void setExpectedPlayers(int playerId, String matchId, int n) throws RemoteException;

    void requestStartGame(int playerId, String matchId) throws RemoteException;

    void submitAction(PlayerAction action, String matchId) throws RemoteException;

    void logout(int playerId, String matchId) throws RemoteException;
}