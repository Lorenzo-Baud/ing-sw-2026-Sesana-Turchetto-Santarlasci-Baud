package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side implementation of {@link RemoteServer}: the remote object
 * published in the registry and the entry point for RMI clients.
 *
 * <p>Responsibility: translate semantic RMI invocations into calls on
 * {@link MainController}. The rest of the project sees only the
 * transport-agnostic abstraction
 * {@link it.polimi.ingws.gc8.Network.Connection.ClientConnection}.</p>
 */
public class RMIServer extends UnicastRemoteObject implements RemoteServer {

    private final MainController mainController;
    private final Map<Integer, RMIClientConnection> connections;

    public RMIServer(MainController mainController) throws RemoteException {
        super();
        this.mainController = mainController;
        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    public LoginResult login(String nickname, RemoteView clientView) throws RemoteException {
        if (clientView == null || nickname == null || nickname.isBlank()) {
            throw new RemoteException("Invalid login parameters");
        }

        int playerId = mainController.assignPlayerId();
        RMIClientConnection connection = new RMIClientConnection(clientView);
        connections.put(playerId, connection);

        ClientHandler handler = new ClientHandler(connection, mainController, playerId, nickname);
        mainController.registerClient(handler);

        Thread t = new Thread(handler, "client-handler-rmi-" + playerId);
        t.setDaemon(true);
        t.start();

        boolean isFirst = mainController.isHost(playerId);
        return new LoginResult(playerId, isFirst);
    }

    @Override
    public void setExpectedPlayers(int playerId, int n) throws RemoteException {
        mainController.setExpectedPlayers(playerId, n);
    }

    @Override
    public void requestStartGame(int playerId) throws RemoteException {
        // playerId currently kept for logging / future authorization checks
        mainController.requestStartGame();
    }

    @Override
    public void submitAction(PlayerAction action) throws RemoteException {
        if (action == null) return;
        RMIClientConnection conn = connections.get(action.getPlayerId());
        if (conn != null) {
            conn.enqueueAction(action);
        }
    }

    @Override
    public void logout(int playerId) throws RemoteException {
        RMIClientConnection conn = connections.remove(playerId);
        if (conn != null) {
            conn.close();
        }
    }
}