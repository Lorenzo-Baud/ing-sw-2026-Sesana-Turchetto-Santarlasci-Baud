package it.polimi.ingws.gc8.Client;

import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.View.View;

/**
 * Collante tra rete e UI
 * Adattatore di rete lato client. Non mantiene più stato di gioco: lo stato
 * vive dentro {@link View#getGameView()} ed è scritto dal dispatcher della
 * View stessa.
 *
 * <h2>Ruolo</h2>
 * <ul>
 *   <li>Implementa {@link ViewListener}: riceve le azioni prodotte dalla
 *       View (via {@code notifyAllListeners}) e le inoltra al server tramite
 *       {@link ServerConnection#sendAction(Object)}.</li>
 *   <li>Fa girare un thread "network pump" che fa polling su
 *       {@link ServerConnection#receive()} e inserisce i {@link NetworkMessage}
 *       in {@link View#addMessage(NetworkMessage)}.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * Un solo thread dedicato al pump (daemon). La View è già thread-safe rispetto
 * alla sua coda ({@code LinkedBlockingQueue}), quindi non servono lock qui.
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

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Registra questo controller come listener della View e avvia il pump. */
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

    // ------------------------------------------------------------------
    // Incoming: server -> view queue (in ingresso)
    // ------------------------------------------------------------------

    private void pumpLoop() {
        while (running) {
            try {
                Object raw = serverConnection.receive(); //blocca finché arriva qualcosa
                if (raw instanceof NetworkMessage m) {
                    view.addMessage(m);                 //lo mette in coda alla View
                }
                // Tipi sconosciuti vengono silenziosamente scartati.
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

    // ------------------------------------------------------------------
    // Outgoing: view -> server (in uscita)
    // ------------------------------------------------------------------

    /**
     * Callback {@link ViewListener}: la View ha prodotto un input dell'utente.
     * Tipicamente {@code message} è un
     * {@link it.polimi.ingws.gc8.Controller.actions.PlayerAction}.
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
