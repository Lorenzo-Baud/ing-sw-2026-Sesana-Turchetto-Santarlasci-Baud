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

    private final MatchRegistry registry;
    private final Map<String, RMIClientConnection> connections = new ConcurrentHashMap<>();

    public RMIServer(MatchRegistry registry) throws RemoteException {
        super(EXPORT_PORT);
        this.registry = registry;
    }

    @Override
    public List<MatchInfo> listOpenMatches() throws RemoteException {
        return registry.listOpenMatches();
    }

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

        return result;
    }

    @Override
    public void setExpectedPlayers(int playerId, String matchId, int n) throws RemoteException {
        MainController match = registry.getMatch(matchId);
        if (match != null) {
            match.setExpectedPlayers(playerId, n);
        }
    }

    @Override
    public void requestStartGame(int playerId, String matchId) throws RemoteException {
        MainController match = registry.getMatch(matchId);
        if (match != null) {
            match.requestStartGame();
        }
    }

    @Override
    public void submitAction(PlayerAction action, String matchId) throws RemoteException {
        if (action == null) return;
        String key = matchId + ":" + action.getPlayerId();
        RMIClientConnection conn = connections.get(key);
        if (conn != null) {
            conn.enqueueAction(action);
        }
    }

    @Override
    public void pong(int playerId, String matchId) throws RemoteException {
        MainController match = registry.getMatch(matchId);
        if (match != null) {
            match.onPong(playerId);
        }
    }

    @Override
    public void logout(int playerId, String matchId) throws RemoteException {
        String key = matchId + ":" + playerId;
        RMIClientConnection conn = connections.remove(key);
        if (conn != null) {
            conn.close();
        }
    }
}