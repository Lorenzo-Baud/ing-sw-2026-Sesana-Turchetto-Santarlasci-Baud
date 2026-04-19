package it.polimi.ingws.gc8.Network;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteConnectionInterface extends Remote {

    /**
     * Pushes messages to the server
     * @param message message that has to be sent
     * @throws RemoteException
     */
    void pushMessageToServer(Object message) throws RemoteException;

    /**
     * Pulls message from the server
     * @return received message
     * @throws RemoteException
     */
    Object pullMessageFromServer() throws RemoteException;
}