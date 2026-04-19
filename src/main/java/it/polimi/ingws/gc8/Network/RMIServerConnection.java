package it.polimi.ingws.gc8.Network;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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