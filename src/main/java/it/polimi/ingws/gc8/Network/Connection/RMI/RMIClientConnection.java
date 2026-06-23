package it.polimi.ingws.gc8.Network.Connection.RMI;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ClientConnection;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

import java.rmi.RemoteException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /** Remove view object */
    private final RemoteView remoteView;
    /** Inbound message queue */
    private final BlockingQueue<Object> inboundQueue;
    /** Flags that indicates wether or not the server is active*/
    private volatile boolean active;
    /** Ensures the {@link PoisonPill} is enqueued exactly once. */
    private final AtomicBoolean inboundClosed = new AtomicBoolean(false);

    /** RMIClientConnection constructor */
    public RMIClientConnection(RemoteView remoteView) {
        this.remoteView = remoteView;
        this.inboundQueue = new LinkedBlockingQueue<>();
        this.active = true;
    }

    /**
     * Sends a message through RMI
     * @param message the message to send
     */
    @Override
    public void sendMessage(Object message) {
        if (!active) return;
        if (!(message instanceof NetworkMessage networkMessage)) return;
        try {
            remoteView.onGameUpdate(networkMessage);
        } catch (RemoteException e) {
            active = false;
            releaseInbound();
        }
    }

    /**
     * Receives message
     * @return received message
     * @throws Exception if an error in the message reception occurs
     */
    @Override
    public Object receiveMessage() throws Exception {
        Object message = inboundQueue.take();
        if (message instanceof PoisonPill) {
            throw new Exception("Connection closed: PoisonPill received");
        }
        return message;
    }

    /** Closes RMI connection */
    @Override
    public void close() {
        active = false;
        releaseInbound();
    }

    /**
     * Unblocks {@link #receiveMessage()} by enqueuing the poison pill exactly
     * once, regardless of whether {@link #active} was already cleared by a
     * failed {@link #sendMessage}. This is the single place that guarantees the
     * handler thread wakes up and runs its cleanup.
     */
    private void releaseInbound() {
        if (inboundClosed.compareAndSet(false, true)) {
            inboundQueue.offer(new PoisonPill());
        }
    }

    /**
     * Returns whether the connection is still active or not
     * @return whether the connection is still active or not
     */
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