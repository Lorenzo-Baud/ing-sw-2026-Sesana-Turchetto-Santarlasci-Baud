package it.polimi.ingws.gc8.View;

import it.polimi.ingws.gc8.Client.ViewListener;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
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
    /** Outcome of {@link #sendAndAwaitUpdate(Object)}. */
    public enum SendResult {
        /** Server replied with a fresh {@code gameView} snapshot. */
        OK,
        /** Server replied with an {@code ErrorMessage}. */
        ERROR,
        /** No reply within the timeout window. */
        TIMEOUT
    }

    /**
     * Sends an action to the server and waits for a new gameView snapshot
     * (i.e., confirmation that the server has processed it).
     *
     * <p>Without this wait, after {@code notifyAllListeners} the main loop
     * would see the old {@code gameView} (with the turn still belonging to the
     * local player) and redraw the prompt immediately before the server's response
     * arrives — typical race condition when sending and then reading shared state
     * without a barrier.</p>
     *
     * <p>Reference identity comparison: the dispatcher replaces
     * {@code gameView} with a new {@code GameDTO} on each
     * {@code ModelUpdateMessage}, so {@code gameView != before} signals
     * that the server has responded.</p>
     *
     * <p>The {@code before}/{@code errorBefore} snapshots are taken
     * <em>after</em> {@code notifyAllListeners} returns: this minimises the
     * window in which a stale (previous-action) error message could be
     * mis-attributed to the current send.</p>
     *
     * @return {@link SendResult#OK} on snapshot update,
     *         {@link SendResult#ERROR} on server-side error,
     *         {@link SendResult#TIMEOUT} if the server does not reply in time.
     */
    protected abstract SendResult sendAndAwaitUpdate(Object action);


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


    // ------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------
    /**
     * Checks if it's the user's turn, based on the last {@code gameView} snapshot.
     * @return true if it's the user's turn, false otherwise
     */
    public boolean isMyTurn() {
        GameDTO g = gameView;
        if (g == null || g.currentPlayer() == null || getNickname() == null) return false;
        return getNickname().equals(g.currentPlayer().nickname());
    }

    public static int maxUpperPicks(OffertileId id) {
        return switch (id) {
            case C -> 1;
            case E -> 1;
            case F -> 2;
            case G -> 2;
            default -> 0;
        };
    }

    public static int maxLowerPicks(OffertileId id) {
        return switch (id) {
            case B -> 1;
            case D -> 2;
            case E -> 1;
            case G -> 1;
            default -> 0;
        };
    }

    // Offer tile rules (cfr. offer_tiles.json):
    //   A → food+3 (no picks)
    //   B → 1 lower           D → 2 lower
    //   C → 1 upper           F → 2 upper
    //   E → 1 lower + 1 upper G → 1 lower + 2 upper
    private static boolean allowsUpperPick(OffertileId id) {
        return id == OffertileId.C || id == OffertileId.E
                || id == OffertileId.F || id == OffertileId.G;
    }
    private static boolean allowsLowerPick(OffertileId id) {
        return id == OffertileId.B || id == OffertileId.D
                || id == OffertileId.E || id == OffertileId.G;
    }
    public static boolean allowsFood(OffertileId id) {
        return id == OffertileId.A;
    }

}
