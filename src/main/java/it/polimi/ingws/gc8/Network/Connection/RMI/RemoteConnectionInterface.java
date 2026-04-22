package it.polimi.ingws.gc8.Network.Connection.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines the methods that the client can invoke on the server via RMI.
 * This interface is implemented by the server-side RMI connection handler
 * and used by the client to communicate with the server.
 */
public interface RemoteConnectionInterface extends Remote {

    /**
     * Sends a message (action, login, etc.) from the client to the server.
     * * @param message the object to be sent to the server
     * @throws RemoteException if a communication error occurs
     */
    void pushMessageToServer(Object message) throws RemoteException;

    /**
     * Fetches the next available message from the server's outbound queue.
     * This is a blocking call: the client will wait until a message is available.
     * * @return the message received from the server
     * @throws RemoteException if a communication error occurs
     */
    Object pullMessageFromServer() throws RemoteException;

}