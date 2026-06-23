package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Controller.MatchRegistry;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RMIServer extends UnicastRemoteObject implements RemoteServer {

    public static final int EXPORT_PORT = 1099;

    /** Registry of matches */
    private final MatchRegistry registry;
    /** List of active connections */
    private final Map<String, RMIClientConnection> connections = new ConcurrentHashMap<>();

    /** RMIServer constructor */
    public RMIServer(MatchRegistry registry) throws RemoteException {
        super(EXPORT_PORT);
        this.registry = registry;
    }

    /**
     * Returns a list of open matches
     * @return list of open matches
     * @throws RemoteException if an error occurs
     */
    @Override
    public List<MatchInfo> listOpenMatches() throws RemoteException {
        return registry.listOpenMatches();
    }

    /**
     * Method to create a match
     * @param matchName match name
     * @param nickname host nickname
     * @param clientView client view object
     * @return JoinResult object containing match and player information
     * @throws RemoteException if an error occurs during match creation
     */
    @Override
    public JoinResult createMatch(String matchName, String nickname, RemoteView clientView) throws RemoteException {
        if (clientView == null || nickname == null || nickname.isBlank()) {
            throw new RemoteException("Invalid parameters");
        }

        // Validate/reserve the name first, so a collision doesn't leak a connection.
        MainController match;
        try {
            match = registry.createMatch(matchName);
        } catch (IllegalStateException e) {
            throw new RemoteException(e.getMessage());
        }

        RMIClientConnection connection = new RMIClientConnection(clientView);
        ClientHandler handler = new ClientHandler(connection, registry, nickname);

        JoinResult result = match.joinOrReconnect(nickname, handler);

        String key = result.matchId() + ":" + result.playerId();
        connections.put(key, connection);

        Thread t = new Thread(handler, "client-handler-rmi-" + result.playerId());
        t.setDaemon(true);
        t.start();

        return result;
    }

    /**
     * Method to join a match
     * @param matchId match id
     * @param nickname player nickname
     * @param clientView client view object
     * @return JoinResult object containing match and player information
     * @throws RemoteException if an error occurs during match join
     */
    @Override
    public JoinResult joinMatch(String matchId, String nickname, RemoteView clientView) throws RemoteException {
        if (clientView == null || nickname == null || nickname.isBlank() || matchId == null) {
            throw new RemoteException("Invalid parameters");
        }

        MainController match = registry.getMatch(matchId);
        if (match == null) {
            throw new RemoteException("Match non trovato: " + matchId);
        }

        RMIClientConnection connection = new RMIClientConnection(clientView);
        ClientHandler handler = new ClientHandler(connection, registry, nickname);

        JoinResult result;
        try {
            result = match.joinOrReconnect(nickname, handler);
        } catch (IllegalStateException e) {
            throw new RemoteException(e.getMessage());
        }

        String key = result.matchId() + ":" + result.playerId();
        connections.put(key, connection);

        Thread t = new Thread(handler, "client-handler-rmi-" + result.playerId());
        t.setDaemon(true);
        t.start();

        // FA2/FA3: deliver the snapshot after the join result is returned.
        if (result.reconnected()) {
            match.completeReconnection(handler);
        }

        return result;
    }

    /**
     * Sets expected number of players in a game
     * @param playerId host id
     * @param matchId match id
     * @param n number of players
     * @throws RemoteException if an error occurs during the method
     */
    @Override
    public void setExpectedPlayers(int playerId, String matchId, int n) throws RemoteException {
        MainController match = registry.getMatch(matchId);
        if (match != null) {
            match.setExpectedPlayers(playerId, n);
        }
    }

    /**
     * Requests game startup
     * @param playerId host name
     * @param matchId match id
     * @throws RemoteException if an error occurs during match startup
     */
    @Override
    public void requestStartGame(int playerId, String matchId) throws RemoteException {
        MainController match = registry.getMatch(matchId);
        if (match != null) {
            match.requestStartGame();
        }
    }

    /**
     * Submits action to the server
     * @param action action that has to be submitted
     * @param matchId match id
     * @throws RemoteException if an error occurs during action submit
     */
    @Override
    public void submitAction(PlayerAction action, String matchId) throws RemoteException {
        if (action == null) return;
        String key = matchId + ":" + action.getPlayerId();
        RMIClientConnection conn = connections.get(key);
        if (conn != null) {
            conn.enqueueAction(action);
        }
    }

    /**
     * Method to handle pong messages from clients
     * @param playerId player id
     * @param matchId match id
     * @throws RemoteException if an error occurs during pong method
     */
    @Override
    public void pong(int playerId, String matchId) throws RemoteException {
        MainController match = registry.getMatch(matchId);
        if (match != null) {
            match.onPong(playerId);
        }
    }

    /**
     * Logs out a player from the server and closes the connection
     * @param playerId player id
     * @param matchId match id
     * @throws RemoteException if an error occurs during logout
     */
    @Override
    public void logout(int playerId, String matchId) throws RemoteException {
        String key = matchId + ":" + playerId;
        RMIClientConnection conn = connections.remove(key);
        if (conn != null) {
            conn.close();
        }
    }
}