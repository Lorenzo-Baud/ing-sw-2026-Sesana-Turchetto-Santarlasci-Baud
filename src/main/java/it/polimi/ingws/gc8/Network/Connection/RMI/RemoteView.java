package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface exposed by the client. The server invokes it through
 * a stub to deliver {@link NetworkMessage} instances (callback pattern).
 */
public interface RemoteView extends Remote {

    /**
     * Receives a notification or update from the server.
     *
     * @param message the message to deliver to the view
     */
    void onGameUpdate(NetworkMessage message) throws RemoteException;
}