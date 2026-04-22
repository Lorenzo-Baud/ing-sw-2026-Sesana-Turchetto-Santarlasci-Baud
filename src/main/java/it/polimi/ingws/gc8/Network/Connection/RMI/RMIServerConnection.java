package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Network.Connection.ServerConnection;

public class RMIServerConnection implements ServerConnection {

    /**
     * Establishes a connection with the server.
     *
     * @param host the server IP or hostname
     * @param port the server port
     * @throws Exception if connection fails
     */
    @Override
    public void connect(String host, int port) throws Exception {
        
    }

    /**
     * Sends an action or message from the player to the server.
     *
     * @param action the object representing the player's move
     */
    @Override
    public void sendAction(Object action) {

    }

    /**
     * Blocking pull from the server. Stub finché non viene cablato il
     * client-side RMI (userà il remote stub {@code pullMessageFromServer()}).
     */
    @Override
    public Object receive() throws Exception {
        throw new UnsupportedOperationException("RMIServerConnection.receive() not yet implemented");
    }

    /**
     * Closes the connection from the client side.
     */
    @Override
    public void disconnect() {

    }

    /**
     * @return true if the client is currently connected to a server
     */
    @Override
    public boolean isConnected() {
        return false;
    }
}