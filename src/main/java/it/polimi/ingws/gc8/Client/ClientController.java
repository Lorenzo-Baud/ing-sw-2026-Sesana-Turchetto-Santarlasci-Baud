package it.polimi.ingws.gc8.Client;

import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.Network.Messages.PingMessage;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.View.View;

/**
 * Network adapter for the client-side infrastructure, serving as the bridge between
 * the networking layer and the UI.
 *
 * This component is stateless regarding game logic; the authoritative client-side
 * state resides within {@link View#getGameView()} and is managed exclusively by
 * the View's internal dispatcher.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Upstream:</b> Implements {@link ViewListener} to intercept UI-triggered
 *       actions (via {@code notifyAllListeners}) and dispatch them to the remote
 *       endpoint through {@link ServerConnection#sendAction(Object)}.</li>
 *   <li><b>Downstream:</b> Manages a dedicated daemon thread (Network Pump) that
 *       polls {@link ServerConnection#receive()} and enqueues incoming
 *       {@link NetworkMessage} instances into {@link View#addMessage(NetworkMessage)}.</li>
 * </ul>
 *
 * <h2>Concurrency Model</h2>
 * Networking is handled by a single background daemon thread. Since the View
 * utilizes a thread-safe message queue ({@code LinkedBlockingQueue}), no
 * additional synchronization or locking is required within this adapter.
 */
public class ClientController implements ViewListener {

    private final ServerConnection serverConnection;
    private final View view;

    private Thread networkPump;
    private volatile boolean running;

    public ClientController(ServerConnection serverConnection, View view) {
        this.serverConnection = serverConnection;
        this.view = view;
    }

    /** Registers this controller as listener of the view. */
    public void start() {
        if (running) return;
        running = true;
        view.addListener(this);
        networkPump = new Thread(this::pumpLoop, "client-network-pump");
        networkPump.setDaemon(true);
        networkPump.start();
    }

    public void stop() {
        running = false;
        view.removeListener(this);
        if (networkPump != null) networkPump.interrupt();
    }

    private void pumpLoop() {
        while (running) {
            try {
                Object raw = serverConnection.receive(); // Blocks until a message arrives
                if (raw instanceof PingMessage) {
                    // FA3: heartbeat — reply immediately, never forward to the UI.
                    try {
                        serverConnection.pong();
                    } catch (Exception ignored) {
                        // A failed pong means the link is down; the next receive()
                        // will surface the error through the normal path.
                    }
                } else if (raw instanceof NetworkMessage m) {
                    view.addMessage(m);                 // Queues the message into the queue
                }
                // Unknown messages are discarded
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (running) {
                    String detail = (e.getMessage() == null) ? "connection lost" : e.getMessage();
                    view.addMessage(new ErrorMessage("NETWORK", detail));
                }
                return;
            }
        }
    }

    /**
     * {@link ViewListener} callback invoked when the View captures user input.
     * Typically, the {@code message} argument encapsulates a
     * {@link it.polimi.ingws.gc8.Controller.actions.PlayerAction} to be
     * processed by the controller or dispatched across the network.
     */
    @Override
    public void updateFromView(Object message) {
        if (message == null) return;
        try {
            serverConnection.sendAction(message);
        } catch (Exception e) {
            String detail = (e.getMessage() == null) ? "send failed" : e.getMessage();
            view.addMessage(new ErrorMessage("NETWORK", detail));
        }
    }
}
