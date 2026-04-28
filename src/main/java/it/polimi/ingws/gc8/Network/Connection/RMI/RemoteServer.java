package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface exposed by the server and published in the RMI registry.
 * Defines the semantic lobby and game operations a client can invoke
 * on the server.
 */
public interface RemoteServer extends Remote {

    /**
     * Registers a new client into the lobby and starts a server-side session.
     *
     * @param nickname   nickname chosen by the player
     * @param clientView remote callback used by the server to push notifications
     * @return the {@link LoginResult} containing the assigned playerId and
     *         whether this player is the lobby host (first to join)
     */
    LoginResult login(String nickname, RemoteView clientView) throws RemoteException;

    /**
     * Sets the expected number of players for the match. Only the host
     * (first player to log in) is allowed to call it; the server silently
     * ignores invalid invocations.
     *
     * @param playerId id of the requesting player (must be the host)
     * @param n        expected number of players
     */
    void setExpectedPlayers(int playerId, int n) throws RemoteException;

    /**
     * Requests the start of the game (lobby action). The server normally
     * auto-starts the match as soon as the lobby is full, so this method
     * is reserved for manual / fallback scenarios.
     *
     * @param playerId id of the requesting player
     */
    void requestStartGame(int playerId) throws RemoteException;

    /**
     * Forwards a typed game action to the server.
     *
     * @param action the action to be executed
     */
    void submitAction(PlayerAction action) throws RemoteException;

    /**
     * Disconnects the client and releases its server-side session.
     *
     * @param playerId id of the player ending the session
     */
    void logout(int playerId) throws RemoteException;
}