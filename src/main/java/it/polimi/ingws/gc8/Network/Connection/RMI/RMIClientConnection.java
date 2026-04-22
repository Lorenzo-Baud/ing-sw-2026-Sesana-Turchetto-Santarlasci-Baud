package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Network.Connection.ClientConnection;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Server-client communication, this class is used by the Server to send messages
 * to the Client and receive messages from the Client via RMI. It implements the ClientConnection
 * interface for the Server's use and the RemoteConnectionInterface for the Client's use.
 */
public class RMIClientConnection extends UnicastRemoteObject implements ClientConnection, RemoteConnectionInterface {

    // Queue for messages waiting to be sent to the client
    private BlockingQueue<Object> outboundQueue;

    // Queue for messages received from the client
    private BlockingQueue<Object> inboundQueue;

    private boolean active;

    /**
     * A special message used to unblock receiveMessage() when close() is called.
    */
    public static class PoisonPill {};

    /**
     * Constructs the RMI connection and exports it.
     * @throws RemoteException if the RMI export fails
     */
    public RMIClientConnection() throws RemoteException {
        super(); // Exports this object to the RMI runtime
        this.outboundQueue = new LinkedBlockingQueue<>();
        this.inboundQueue = new LinkedBlockingQueue<>();
        this.active = true;
    }

    /**
     * Sends a message to the client. Must be thread-safe:
     * multiple threads (e.g., ViewUpdater broadcasting) may call this concurrently.
     *
     * @param message the message to send
     */
    @Override
    public void sendMessage(Object message) {
        if (active) {
            outboundQueue.offer(message);
        }
    }

    /**
     * Blocking read: waits for and returns the next message from the client.
     *
     * @return the received message
     * @throws Exception if the connection is lost or an error occurs
     */
    @Override
    public Object receiveMessage() throws Exception {
        Object message = inboundQueue.take(); // Blocks here if queue is empty
        if (message instanceof PoisonPill) {
            throw new Exception("Connection closed: Poison Pill received.");
        }
        return message;
    }

    /**
     * Closes the connection and releases resources.
     */
    @Override
    public void close() {
        if (active) {
            active = false;
            inboundQueue.offer(new PoisonPill());
            outboundQueue.offer(new PoisonPill());
        }
    }

    /**
     * @return true if the connection is still active
     */
    @Override
    public boolean isConnected() {
        return active;
    }

    /**
     * Called by the Client (via RMI stub) to send an action/message to the Server.
     */
    public void pushMessageToServer(Object message) throws RemoteException {
        if (!active) throw new RemoteException("Connection is closed.");
        inboundQueue.offer(message);
    }

    /**
     * Called by the Client (via RMI stub) to fetch messages waiting on the Server.
     * This implements a "Long Polling" or "Blocking Pull" pattern.
     */
    public Object pullMessageFromServer() throws RemoteException {
        if (!active) return new PoisonPill();
        try {
            // Wait for the server to put something in the outbound queue
            return outboundQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Thread interrupted during RMI pull", e);
        }
    }

}
