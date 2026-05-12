package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
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
 */
public class SocketServerConnection implements ServerConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /** Queue used to synchronize the blocking login request with the asynchronous reception loop. */
    private final BlockingQueue<LoginResult> loginQueue = new LinkedBlockingQueue<>();

    /**
     * Initializes an unconfigured server connection instance.
     */
    public SocketServerConnection() { }

    /**
     * Establishes a TCP connection with the server and initializes object streams.
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
    }

    /**
     * Sends a login request to the server and blocks until a {@link LoginResult} is received.
     *
     * @param nickname the chosen player alias.
     * @return the result of the login attempt containing player ID and lobby status.
     * @throws Exception if transmission fails or the thread is interrupted while waiting.
     */
    @Override
    public LoginResult login(String nickname) throws Exception {
        out.writeObject("LOGIN:" + nickname);
        out.reset();
        out.flush();
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
        out.writeObject("SET_EXPECTED_PLAYERS:" + n);
        out.reset();
        out.flush();
    }

    /**
     * Sends a request to the server to move the lobby into the game phase.
     *
     * @throws Exception if a network error occurs during transmission.
     */
    @Override
    public void requestStartGame() throws Exception {
        out.writeObject("REQUEST_START_GAME");
        out.reset();
        out.flush();
    }

    /**
     * Transmits a game action to the server.
     * This method is thread-safe and ensures object stream integrity via synchronization.
     *
     * @param action the game action or command to be sent.
     */
    @Override
    public void sendAction(Object action) {
        if (!isConnected()) return;
        try {
            synchronized (out) {
                out.writeObject(action);
                out.reset();
                out.flush();
            }
        } catch (IOException e) {
            disconnect();
        }
    }

    /**
     * Listens for objects from the input stream.
     * Intercepts {@link LoginResult} instances to populate the local sync queue;
     * otherwise, returns the received object to the caller.
     *
     * @return the received object.
     * @throws Exception if a read error or deserialization failure occurs.
     */
    @Override
    public Object receive() throws Exception {
        Object obj = in.readObject();

        if (obj instanceof LoginResult) {
            loginQueue.offer((LoginResult) obj);
            return receive();
        }
        return obj;
    }

    /**
     * Safely closes the socket and all associated streams, terminating the connection.
     */
    @Override
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
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
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}