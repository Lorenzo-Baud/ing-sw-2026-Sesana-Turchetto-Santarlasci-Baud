package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ClientConnection;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

import java.rmi.RemoteException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Server-side adapter for a single RMI client connection.
 * Implements {@link ClientConnection} keeping it transport-agnostic:
 * <ul>
 *   <li><b>Outbound</b>: directly invokes the client's {@link RemoteView}
 *       stub (callback). No outbound queue on the server side.</li>
 *   <li><b>Inbound</b>: {@link PlayerAction}s received from
 *       {@link RMIServer#submitAction(PlayerAction)} are enqueued here
 *       and consumed by the {@code ClientHandler.run()} thread.</li>
 * </ul>
 *
 * <p>This class is NOT a remote object: the remote object is
 * {@link RMIServer}, shared among all clients. The reverse direction
 * (server → client) goes through the {@code RemoteView} stub provided
 * at login time.</p>
 */
public class RMIClientConnection implements ClientConnection {

    /** Server-internal marker used to unblock {@link #receiveMessage()} on close. */
    public static final class PoisonPill {}

    private final RemoteView remoteView;
    private final BlockingQueue<Object> inboundQueue;
    private volatile boolean active;

    public RMIClientConnection(RemoteView remoteView) {
        this.remoteView = remoteView;
        this.inboundQueue = new LinkedBlockingQueue<>();
        this.active = true;
    }

    @Override
    public void sendMessage(Object message) {
        if (!active) return;
        if (!(message instanceof NetworkMessage networkMessage)) return;
        try {
            remoteView.onGameUpdate(networkMessage);
        } catch (RemoteException e) {
            active = false;
        }
    }

    @Override
    public Object receiveMessage() throws Exception {
        Object message = inboundQueue.take();
        if (message instanceof PoisonPill) {
            throw new Exception("Connection closed: PoisonPill received");
        }
        return message;
    }

    @Override
    public void close() {
        if (active) {
            active = false;
            inboundQueue.offer(new PoisonPill());
        }
    }

    @Override
    public boolean isConnected() {
        return active;
    }

    /**
     * Enqueues an action received from the client through {@link RMIServer}.
     * Package-private: only {@code RMIServer} is expected to call it.
     */
    void enqueueAction(PlayerAction action) {
        if (active && action != null) {
            inboundQueue.offer(action);
        }
    }
}