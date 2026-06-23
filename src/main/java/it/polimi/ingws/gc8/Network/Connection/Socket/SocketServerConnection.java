package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.CreateMatchRequest;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.JoinMatchRequest;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.ListMatchesRequest;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.ListMatchesResponse;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.RequestStartGameCommand;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.SetExpectedPlayersCommand;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.PongMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TCP-based implementation of {@link ServerConnection} used by the client.
 *
 * <p>This class owns the socket, the object streams, and a background reader
 * thread that continuously drains messages from the server into an internal
 * queue. The public API exposes synchronous request/response operations for
 * lobby commands and fire-and-forget methods for in-game actions.</p>
 *
 * <h2>Threading model</h2>
 * <ul>
 *   <li>The calling thread performs writes through a synchronized lock so
 *       {@link ObjectOutputStream} access remains serialized.</li>
 *   <li>A dedicated reader thread is started after a match is created or
 *       joined; it forwards every incoming object to {@code responseQueue}.</li>
 *   <li>{@link #receive()} blocks on that queue and returns the next server
 *       message to the controller.</li>
 * </ul>
 */
public class SocketServerConnection implements ServerConnection {

    /** Underlying TCP socket connected to the game server. */
    private Socket socket;

    /** Stream used to serialize and send requests to the server. */
    private ObjectOutputStream out;

    /** Stream used to read responses and asynchronous updates from the server. */
    private ObjectInputStream in;

    /** Mutual exclusion guard for writes on the output stream. */
    private final Object writeLock = new Object();

    /** Buffer of objects received from the server but not yet consumed. */
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

    /** True while the socket is considered usable. */
    private volatile boolean connected;

    /** Ensures the background reader thread is started only once. */
    private volatile boolean readerStarted;

    /** Background thread that reads objects from the server. */
    private Thread readerThread;

    /** Creates a disconnected connection adapter. */
    public SocketServerConnection() { }

    /**
     * Opens a TCP connection to the server and initializes the object streams.
     *
     * @param host server host name or address
     * @param port server TCP port
     * @throws Exception if the socket or streams cannot be created
     */
    @Override
    public void connect(String host, int port) throws Exception {
        this.socket = new Socket(host, port);
        this.socket.setTcpNoDelay(true);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
    }

    /**
     * Requests the list of available matches from the server.
     *
     * <p>This method sends a request and blocks until the matching
     * {@link ListMatchesResponse} is received.</p>
     *
     * @return the current matches advertised by the server
     * @throws Exception if the connection is unavailable or the response cannot
     *                   be read
     */
    @Override
    public List<MatchInfo> listMatches() throws Exception {
        ensureConnected();
        writeOnLock(new ListMatchesRequest());
        Object resp = waitForResponse(ListMatchesResponse.class);
        return ((ListMatchesResponse) resp).matches();
    }

    /**
     * Creates a new match and waits for the server join result.
     *
     * <p>If the server replies with an {@link ErrorMessage}, it is translated
     * to an {@link IOException}. On success, the background reader thread is
     * started so asynchronous game updates can be received.</p>
     *
     * @param matchName name of the new match to create
     * @param nickname player's nickname to register in the match
     * @return the join result produced by the server
     * @throws Exception if the connection fails or the server rejects the
     *                   request with an error message
     */
    @Override
    public JoinResult createMatch(String matchName, String nickname) throws Exception {
        ensureConnected();
        writeOnLock(new CreateMatchRequest(matchName, nickname));
        Object resp = waitForResponse(JoinResult.class);
        if (resp instanceof ErrorMessage err) {
            throw new IOException(err.getError());
        }
        startReaderThread();
        return (JoinResult) resp;
    }

    /**
     * Joins an existing match and waits for the server join result.
     *
     * <p>If the server replies with an {@link ErrorMessage}, it is translated
     * to an {@link IOException}. On success, the background reader thread is
     * started so asynchronous game updates can be received.</p>
     *
     * @param matchId identifier of the match to join
     * @param nickname player's nickname to register in the match
     * @return the join result produced by the server
     * @throws Exception if the connection fails or the server rejects the
     *                   request with an error message
     */
    @Override
    public JoinResult joinMatch(String matchId, String nickname) throws Exception {
        ensureConnected();
        writeOnLock(new JoinMatchRequest(matchId, nickname));
        Object resp = waitForResponse(JoinResult.class);
        if (resp instanceof ErrorMessage err) {
            throw new IOException(err.getError());
        }
        startReaderThread();
        return (JoinResult) resp;
    }

    /**
     * Sends the expected player count for the lobby setup.
     *
     * @param n number of players that should join the match
     * @throws Exception if the connection is not available or the write fails
     */
    @Override
    public void setExpectedPlayers(int n) throws Exception {
        ensureConnected();
        writeOnLock(new SetExpectedPlayersCommand(n));
    }

    /**
     * Requests the server to start the game.
     *
     * @throws Exception if the connection is not available or the write fails
     */
    @Override
    public void requestStartGame() throws Exception {
        ensureConnected();
        writeOnLock(new RequestStartGameCommand());
    }

    /**
     * Sends a player action to the server without waiting for an immediate
     * response.
     *
     * <p>Any write failure closes the connection silently, since this method
     * is used by the controller as part of the game loop.</p>
     *
     * @param action serialized game action to send
     */
    @Override
    public void sendAction(Object action) {
        if (!connected) return;
        try {
            writeOnLock(action);
        } catch (IOException e) {
            disconnect();
        }
    }

    /**
     * Sends a pong message to keep the connection alive.
     *
     * @throws Exception if the connection is not available or the write fails
     */
    @Override
    public void pong() throws Exception {
        if (!connected) return;
        try {
            writeOnLock(new PongMessage());
        } catch (IOException e) {
            disconnect();
        }
    }

    /**
     * Blocks until the next object received from the server is available.
     *
     * <p>The returned object is produced by the background reader thread and
     * may be either a game update or another network message.</p>
     *
     * @return the next queued server object
     * @throws Exception if the connection is not available
     */
    @Override
    public Object receive() throws Exception {
        ensureConnected();
        return responseQueue.take();
    }

    /**
     * Closes the socket and stops the background reader thread.
     *
     * <p>This method is idempotent and ignores socket-close failures.</p>
     */
    @Override
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        if (readerThread != null) readerThread.interrupt();
    }

    /**
     * Returns whether the underlying socket is still open and usable.
     *
     * @return {@code true} if the connection is currently alive
     */
    @Override
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Reads objects from the socket until one of the expected type is found.
     *
     * <p>Unexpected objects are preserved by placing them into
     * {@code responseQueue} so they can be consumed later by the controller.
     * Server-side {@link ErrorMessage}s are returned immediately, regardless of
     * the expected type.</p>
     *
     * @param expectedType type used to identify the synchronous response
     * @return the first matching response or an error message
     * @throws Exception if the socket read fails
     */
    private Object waitForResponse(Class<?> expectedType) throws Exception {
        while (true) {
            Object obj = in.readObject();
            if (expectedType.isInstance(obj) || obj instanceof ErrorMessage) {
                return obj;
            }
            responseQueue.put(obj);
        }
    }

    /**
     * Starts the asynchronous reader thread if it has not been created yet.
     *
     * <p>The thread continuously reads from the socket and pushes every object
     * into {@code responseQueue}. If reading fails, the connection is marked as
     * closed.</p>
     */
    private void startReaderThread() {
        if (readerStarted) return;
        readerStarted = true;
        readerThread = new Thread(() -> {
            try {
                while (connected) {
                    Object obj = in.readObject();
                    responseQueue.put(obj);
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "tcp-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Serializes and writes an object to the socket while holding the write lock.
     *
     * @param obj object to send to the server
     * @throws IOException if the write or flush operation fails
     */
    private void writeOnLock(Object obj) throws IOException {
        synchronized (writeLock) {
            out.writeObject(obj);
            out.flush();
            out.reset();
        }
    }

    /**
     * Verifies that the connection is available before performing a blocking
     * network operation.
     *
     * @throws IllegalStateException if the connection is already closed
     */
    private void ensureConnected() {
        if (!connected) throw new IllegalStateException("Not connected to server");
    }
}
