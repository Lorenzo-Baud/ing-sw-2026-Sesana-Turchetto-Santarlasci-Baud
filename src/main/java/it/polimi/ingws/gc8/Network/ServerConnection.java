package it.polimi.ingws.gc8.Network;

/**
 * Interface for the client-side connection handler.
 * It abstracts the communication towards the server, allowing the View
 * to send actions regardless of the transport layer (RMI or Socket).
 */
public interface ServerConnection {

    /**
     * Establishes a connection with the server.
     * @param host the server IP or hostname
     * @param port the server port
     * @throws Exception if connection fails
     */
    void connect(String host, int port) throws Exception;

    /**
     * Sends an action or message from the player to the server.
     * @param action the object representing the player's move
     */
    void sendAction(Object action);

    /**
     * Closes the connection from the client side.
     */
    void disconnect();

    /**
     * @return true if the client is currently connected to a server
     */
    boolean isConnected();
}