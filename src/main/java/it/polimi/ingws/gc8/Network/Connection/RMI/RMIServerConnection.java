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

    /** Remote server object */
    private RemoteServer remoteServer;
    /** Client view object */
    private RMIClientView clientView;
    /** Player id */
    private int playerId = -1;
    /** Match id */
    private String matchId;
    /** Signals if the client is connected*/
    private volatile boolean connected;

    /**
     * Connects to a host
     * @param host host address
     * @param port host port
     * @throws Exception if an error occurs during connection
     */
    @Override
    public void connect(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.remoteServer = (RemoteServer) registry.lookup(SERVICE_NAME);
        this.clientView = new RMIClientView();
        this.connected = true;
    }

    /**
     * Returns list of matches
     * @return list of matches
     * @throws Exception if an error occurs during the method
     */
    @Override
    public List<MatchInfo> listMatches() throws Exception {
        ensureConnected();
        return remoteServer.listOpenMatches();
    }

    /**
     * Method to create a match
     * @param matchName match name
     * @param nickname host nickname
     * @return JoinResult object containing match and player information
     * @throws RemoteException if an error occurs during match creation
     */
    @Override
    public JoinResult createMatch(String matchName, String nickname) throws Exception {
        ensureConnected();
        JoinResult result = remoteServer.createMatch(matchName, nickname, clientView);
        this.playerId = result.playerId();
        this.matchId = result.matchId();
        return result;
    }

    /**
     * Method to join a match
     * @param matchId match id
     * @param nickname player nickname
     * @return JoinResult object containing match and player information
     * @throws RemoteException if an error occurs during match join
     */
    @Override
    public JoinResult joinMatch(String matchId, String nickname) throws Exception {
        ensureConnected();
        JoinResult result = remoteServer.joinMatch(matchId, nickname, clientView);
        this.playerId = result.playerId();
        this.matchId = result.matchId();
        return result;
    }

    /**
     * Sets expected number of players in a game
     * @param n number of players
     * @throws RemoteException if an error occurs during the method
     */
    @Override
    public void setExpectedPlayers(int n) throws Exception {
        ensureConnected();
        remoteServer.setExpectedPlayers(playerId, matchId, n);
    }

    /**
     * Requests game startup
     * @throws RemoteException if an error occurs during match startup
     */
    @Override
    public void requestStartGame() throws Exception {
        ensureConnected();
        remoteServer.requestStartGame(playerId, matchId);
    }

    /**
     * Submits action to the server
     * @param action action that has to be submitted
     */
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

    /**
     * Method to handle pong messages from clients
     * @throws Exception if an error occurs during pong method
     */
    @Override
    public void pong() throws Exception {
        if (!connected || remoteServer == null) return;
        try {
            remoteServer.pong(playerId, matchId);
        } catch (RemoteException e) {
            connected = false;
        }
    }

    /**
     * Receives object from server
     * @return received object
     * @throws Exception if an error occurs
     */
    @Override
    public Object receive() throws Exception {
        ensureConnected();
        return clientView.take();
    }

    /**
     * Disconnects form a server
     */
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

    /**
     * Returns true if connected
     * @return true if connected
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns player id
     * @return player id
     */
    public int getPlayerId() {
        return playerId;
    }

    /**
     * Returns match id
     * @return match id
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * Ensure the client is connected to the server
     */
    private void ensureConnected() {
        if (!connected || remoteServer == null) {
            throw new IllegalStateException("Not connected to server");
        }
    }
}