package it.polimi.ingws.gc8.Network.Connection;

import it.polimi.ingws.gc8.Network.DTO.LoginResult;

/**
 * Interface for the client-side connection handler.
 * It abstracts the communication towards the server, allowing the View
 * to send actions regardless of the transport layer (RMI or Socket).
 */
public interface ServerConnection {

    /**
     * Establishes a connection with the server.
     *
     * @param host the server IP or hostname
     * @param port the server port
     * @throws Exception if connection fails
     */
    void connect(String host, int port) throws Exception;

    /**
     * Performs the login handshake with the server.
     *
     * @param nickname nickname to register on the server
     * @return a {@link LoginResult} containing the assigned playerId and
     *         whether this client is the lobby host (first to join)
     * @throws Exception if login fails
     */
    LoginResult login(String nickname) throws Exception;

    /**
     * Sets the expected number of players for the lobby. Should be called
     * only by the host (the first player to log in).
     *
     * @param n expected number of players
     * @throws Exception if the request cannot be sent
     */
    void setExpectedPlayers(int n) throws Exception;

    /**
     * Asks the server to start the game (lobby action). Normally not
     * needed: the server auto-starts as soon as the expected number of
     * players is reached.
     *
     * @throws Exception if the request cannot be sent
     */
    void requestStartGame() throws Exception;

    /**
     * Sends an action or message from the player to the server.
     *
     * @param action the object representing the player's move
     */
    void sendAction(Object action);

    /**
     * Blocking pull: returns the next {@code NetworkMessage} arriving from
     * the server. Called from the client-side network pump thread.
     *
     * @return the received message (typically a {@code NetworkMessage})
     * @throws Exception if the connection is closed or a transport error occurs
     */
    Object receive() throws Exception;

    /**
     * Closes the connection from the client side.
     */
    void disconnect();

    /**
     * @return true if the client is currently connected to a server
     */
    boolean isConnected();
}
