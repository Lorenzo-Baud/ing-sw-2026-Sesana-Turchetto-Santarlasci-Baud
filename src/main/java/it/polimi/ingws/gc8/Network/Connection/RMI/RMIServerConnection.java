package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Client-side adapter implementing {@link ServerConnection} on top of RMI.
 * Exposes a transport-agnostic API to the application: the rest of the
 * client (View, ClientController) does not need to know that RMI is
 * the underlying transport.
 */
public class RMIServerConnection implements ServerConnection {

    public static final String SERVICE_NAME = "MesosServer";

    private RemoteServer remoteServer;
    private RMIClientView clientView;
    private int playerId = -1;
    private volatile boolean connected;

    @Override
    public void connect(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.remoteServer = (RemoteServer) registry.lookup(SERVICE_NAME);
        this.clientView = new RMIClientView();
        this.connected = true;
    }

    @Override
    public LoginResult login(String nickname) throws Exception {
        ensureConnected();
        LoginResult result = remoteServer.login(nickname, clientView);
        this.playerId = result.playerId();
        return result;
    }

    @Override
    public void setExpectedPlayers(int n) throws Exception {
        ensureConnected();
        remoteServer.setExpectedPlayers(playerId, n);
    }

    @Override
    public void requestStartGame() throws Exception {
        ensureConnected();
        remoteServer.requestStartGame(playerId);
    }

    @Override
    public void sendAction(Object action) {
        if (!connected) return;
        if (!(action instanceof PlayerAction playerAction)) return;
        try {
            remoteServer.submitAction(playerAction);
        } catch (RemoteException e) {
            connected = false;
        }
    }

    @Override
    public Object receive() throws Exception {
        ensureConnected();
        return clientView.take();
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;
        try {
            if (remoteServer != null && playerId > 0) {
                remoteServer.logout(playerId);
            }
        } catch (RemoteException ignored) {
            // server unreachable: nothing to do
        }
        try {
            if (clientView != null) {
                UnicastRemoteObject.unexportObject(clientView, true);
            }
        } catch (Exception ignored) {
            // already unexported or never exported
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    public int getPlayerId() {
        return playerId;
    }

    private void ensureConnected() {
        if (!connected || remoteServer == null) {
            throw new IllegalStateException("Not connected to server");
        }
    }
}