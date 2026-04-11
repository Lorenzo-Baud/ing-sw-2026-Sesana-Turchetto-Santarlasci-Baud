package it.polimi.ingws.gc8.Network;

/**
 * Abstracts the bidirectional communication channel with a client.
 * Implementations handle the transport-specific details (Socket, RMI, etc.)
 * while {@link ClientHandler} works only with this interface.
 */
public interface ClientConnection {

    /**
     * Sends a message to the client. Must be thread-safe:
     * multiple threads (e.g., ViewUpdater broadcasting) may call this concurrently.
     *
     * @param message the message to send
     */
    void sendMessage(Object message);

    /**
     * Blocking read: waits for and returns the next message from the client.
     *
     * @return the received message
     * @throws Exception if the connection is lost or an error occurs
     */
    Object receiveMessage() throws Exception;

    /**
     * Closes the connection and releases resources.
     */
    void close();

    /**
     * @return true if the connection is still active
     */
    boolean isConnected();
}
