package it.polimi.ingws.gc8.View;

import it.polimi.ingws.gc8.Client.ViewListener;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base class for every View (TUI /e GUI).
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Maintains the list of {@link ViewListener} (the network controller).</li>
 *   <li>Maintains a blocking queue of incoming {@link NetworkMessage}:
 *       the network thread inserts messages, the UI thread consumes them.</li>
 *   <li>Maintains the "soft" state of the UI: {@code nickname} chosen
 *       by the user and the last {@code gameView} snapshot received from the server.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * The {@code messages} queue is a {@code LinkedBlockingQueue} (thread-safe).
 * {@code gameView} and {@code nickname} are {@code volatile}: written by one
 * thread (update dispatcher / main loop), read by another (printing).
 *
 * <h2>Lifecycle</h2>
 * The subclass implements {@link #run()} with its own procedural UI logic
 * (for TUI: initial menu, game phases, etc.).
 */
public abstract class View {

    /** Registered listeners (typically a single ClientController). */
    protected final List<ViewListener> listeners = new ArrayList<>();

    /** Queue of messages from the server, consumed by the UI thread. */
    protected final LinkedBlockingQueue<NetworkMessage> messages = new LinkedBlockingQueue<>();

    /** Nickname chosen by the user, set during login. */
    protected volatile String nickname;

    /** PlayerId assigned by the server during login. */
    protected volatile int playerId = -1;

    /** Last authoritative game snapshot received from the server. */
    protected volatile GameDTO gameView;

    // ------------------------------------------------------------------
    // Listener registry
    // ------------------------------------------------------------------

    public void addListener(ViewListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(ViewListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all listeners that the user has produced input/action.
     * The {@code message} is typically a {@code PlayerAction}.
     */
    protected void notifyAllListeners(Object message) {
        for (ViewListener l : listeners) {
            l.updateFromView(message);
        }
    }

    // ------------------------------------------------------------------
    // Message queue: server -> view
    // ------------------------------------------------------------------

    /** Inserts a server message into the queue. Called by the network pump. */
    public void addMessage(NetworkMessage msg) {
        if (msg != null) messages.offer(msg);
    }

    /**
     * Waits (blocking) for the next message from the server.
     * Interruptible: returns {@code null} if the thread is interrupted.
     */
    protected NetworkMessage waitForMessage() {
        try {
            return messages.take();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Access to "soft" state
    // ------------------------------------------------------------------

    public String getNickname()          { return nickname; }
    public void   setNickname(String n)  { this.nickname = n; }
    public int    getPlayerId()          { return playerId; }
    public void   setPlayerId(int id)    { this.playerId = id; }
    public GameDTO getGameView()         { return gameView; }
    public void    setGameView(GameDTO v){ this.gameView = v; }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    /** Lifecycle of the UI: blocking, called from the client main. */
    public abstract void run();
}
