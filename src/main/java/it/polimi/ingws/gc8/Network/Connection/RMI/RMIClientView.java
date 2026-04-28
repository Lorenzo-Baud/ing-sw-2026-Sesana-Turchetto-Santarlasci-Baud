package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Client-side implementation of {@link RemoteView}.
 * Receives {@link NetworkMessage} instances from the server (via RMI
 * callback) and enqueues them in a {@link BlockingQueue}; the client
 * pump drains them through {@link RMIServerConnection#receive()}.
 */
public class RMIClientView extends UnicastRemoteObject implements RemoteView {

    private final BlockingQueue<NetworkMessage> inboundQueue;

    public RMIClientView() throws RemoteException {
        super();
        this.inboundQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void onGameUpdate(NetworkMessage message) throws RemoteException {
        if (message != null) {
            inboundQueue.offer(message);
        }
    }

    /** Blocks until a message arrives from the server. */
    public NetworkMessage take() throws InterruptedException {
        return inboundQueue.take();
    }
}