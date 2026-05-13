package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.LoginRequest;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.RequestStartGameCommand;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.SetExpectedPlayersCommand;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Client-side implementation of {@link ServerConnection} using Java Sockets.
 * Handles network communication with the server, including initial connection,
 * authentication, and asynchronous message dispatching.
 *
 * <p>All write operations on the underlying {@link ObjectOutputStream} are
 * serialized via a dedicated {@code writeLock}, so callbacks from the network
 * pump (e.g. {@code sendAction}) cannot interleave with lobby commands
 * invoked from the main thread.</p>
 */
public class SocketServerConnection implements ServerConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /** Serializes every write on {@link #out} regardless of the calling thread. */
    private final Object writeLock = new Object();

    /** Queue used to synchronize the blocking login request with the asynchronous reception loop. */
    private final BlockingQueue<LoginResult> loginQueue = new LinkedBlockingQueue<>();

    private volatile boolean connected;

    /**
     * Initializes an unconfigured server connection instance.
     */
    public SocketServerConnection() { }

    /**
     * Establishes a TCP connection with the server and initializes object streams.
     * Output stream is created and flushed before the input stream to avoid the
     * classic {@code ObjectStream} handshake deadlock.
     *
     * @param host the server's hostname or IP address.
     * @param port the server's listening port.
     * @throws Exception if connection or stream initialization fails.
     */
    @Override
    public void connect(String host, int port) throws Exception {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
    }

    /**
     * Sends a {@link LoginRequest} to the server and blocks until a
     * {@link LoginResult} is received via the asynchronous reception loop.
     *
     * @param nickname the chosen player alias.
     * @return the result of the login attempt containing player ID and lobby status.
     * @throws Exception if transmission fails or the thread is interrupted while waiting.
     */
    @Override
    public LoginResult login(String nickname) throws Exception {
        ensureConnected();
        writeOnLock(new LoginRequest(nickname));
        return loginQueue.take();
    }

    /**
     * Notifies the server of the required number of players to start the game.
     *
     * @param n the number of players.
     * @throws Exception if a network error occurs during transmission.
     */
    @Override
    public void setExpectedPlayers(int n) throws Exception {
        ensureConnected();
        writeOnLock(new SetExpectedPlayersCommand(n));
    }

    /**
     * Sends a request to the server to move the lobby into the game phase.
     *
     * @throws Exception if a network error occurs during transmission.
     */
    @Override
    public void requestStartGame() throws Exception {
        ensureConnected();
        writeOnLock(new RequestStartGameCommand());
    }

    /**
     * Transmits an in-game action to the server. Thread-safe.
     *
     * @param action the game action to be sent.
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
     * Listens for objects from the input stream.
     * Intercepts {@link LoginResult} instances to populate the local sync queue;
     * everything else is returned to the caller (the network pump).
     *
     * @return the next non-{@link LoginResult} object received.
     * @throws Exception if a read error or deserialization failure occurs.
     */
    @Override
    public Object receive() throws Exception {
        while (true) {
            Object obj = in.readObject();
            if (obj instanceof LoginResult lr) {
                loginQueue.offer(lr);
                continue;
            }
            return obj;
        }
    }

    /**
     * Safely closes the socket and all associated streams, terminating the connection.
     */
    @Override
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    /**
     * Verifies the current operational status of the socket connection.
     *
     * @return {@code true} if the socket is initialized and currently open.
     */
    @Override
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // Helpers

    /**
     * Writes an object on the shared output stream under {@link #writeLock},
     * then flushes and resets to avoid stale-snapshot caching.
     */
    private void writeOnLock(Object obj) throws IOException {
        synchronized (writeLock) {
            out.writeObject(obj);
            out.flush();
            out.reset();
        }
    }

    private void ensureConnected() {
        if (!connected) throw new IllegalStateException("Not connected to server");
    }
}
