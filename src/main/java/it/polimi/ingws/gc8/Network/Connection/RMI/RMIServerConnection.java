package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RMIServerConnection implements ServerConnection {

    public static final String SERVICE_NAME = "MesosServer";

    private RemoteServer remoteServer;
    private RMIClientView clientView;
    private int playerId = -1;
    private String matchId;
    private volatile boolean connected;

    @Override
    public void connect(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.remoteServer = (RemoteServer) registry.lookup(SERVICE_NAME);
        this.clientView = new RMIClientView();
        this.connected = true;
    }

    @Override
    public List<MatchInfo> listMatches() throws Exception {
        ensureConnected();
        return remoteServer.listOpenMatches();
    }

    @Override
    public JoinResult createMatch(String matchName, String nickname) throws Exception {
        ensureConnected();
        JoinResult result = remoteServer.createMatch(matchName, nickname, clientView);
        this.playerId = result.playerId();
        this.matchId = result.matchId();
        return result;
    }

    @Override
    public JoinResult joinMatch(String matchId, String nickname) throws Exception {
        ensureConnected();
        JoinResult result = remoteServer.joinMatch(matchId, nickname, clientView);
        this.playerId = result.playerId();
        this.matchId = result.matchId();
        return result;
    }

    @Override
    public void setExpectedPlayers(int n) throws Exception {
        ensureConnected();
        remoteServer.setExpectedPlayers(playerId, matchId, n);
    }

    @Override
    public void requestStartGame() throws Exception {
        ensureConnected();
        remoteServer.requestStartGame(playerId, matchId);
    }

    @Override
    public void sendAction(Object action) {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }
        if (!(action instanceof PlayerAction playerAction)) {
            throw new IllegalArgumentException("Unsupported action type: "
                    + (action == null ? "null" : action.getClass().getName()));
        }
        try {
            remoteServer.submitAction(playerAction, matchId);
        } catch (RemoteException e) {
            connected = false;
            throw new IllegalStateException("Connection lost while sending action", e);
        }
    }

    @Override
    public void pong() throws Exception {
        if (!connected || remoteServer == null) return;
        try {
            remoteServer.pong(playerId, matchId);
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
            if (remoteServer != null && playerId > 0 && matchId != null) {
                remoteServer.logout(playerId, matchId);
            }
        } catch (RemoteException ignored) {}
        try {
            if (clientView != null) {
                UnicastRemoteObject.unexportObject(clientView, true);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getMatchId() {
        return matchId;
    }

    private void ensureConnected() {
        if (!connected || remoteServer == null) {
            throw new IllegalStateException("Not connected to server");
        }
    }
}