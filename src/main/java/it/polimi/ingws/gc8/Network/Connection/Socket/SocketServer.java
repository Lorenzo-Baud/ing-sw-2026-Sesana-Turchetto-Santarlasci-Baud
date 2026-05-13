package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.LoginRequest;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main server process that listens for incoming TCP connections.
 *
 * <p>Mirrors {@code RMIServer}: accepts a connection, performs the
 * {@link LoginRequest} handshake, registers the client into the shared
 * {@link MainController}, then hands the socket over to a
 * {@link ClientHandler} thread for in-game traffic.</p>
 */
public class SocketServer implements Runnable {

    /** Maximum time (ms) the handshake may take before the connection is dropped. */
    private static final int HANDSHAKE_TIMEOUT_MS = 5000;

    private final int port;
    private final MainController mainController;
    private final AtomicInteger handshakeCounter = new AtomicInteger();
    private final List<ClientHandler> activeHandlers = new CopyOnWriteArrayList<>();

    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    /**
     * Initializes the server with a specified port and a reference to the main controller.
     *
     * @param port           the TCP port to listen on.
     * @param mainController the game controller responsible for player and lobby management.
     */
    public SocketServer(int port, MainController mainController) {
        this.port = port;
        this.mainController = mainController;
    }

    /**
     * Starts the server loop.
     * Continuously accepts new client connections and offloads the handshake
     * to dedicated daemon threads so the accept loop never blocks on slow
     * or malicious clients.
     */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            // The "ready" banner is printed by ServerMain so it sits next to the
            // RMI one. Don't duplicate it here.

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int hsId = handshakeCounter.incrementAndGet();
                    Thread handshakeThread = new Thread(
                            () -> handleHandshake(clientSocket),
                            "tcp-handshake-" + hsId);
                    handshakeThread.setDaemon(true);
                    handshakeThread.start();
                } catch (IOException e) {
                    if (running) System.err.println("Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start Socket Server on port " + port);
        } finally {
            stop();
        }
    }

    /**
     * Processes the initial communication from a new connection.
     * Reads the mandatory {@link LoginRequest}, registers the player and
     * spawns the in-game {@link ClientHandler} thread.
     *
     * @param clientSocket the freshly accepted socket.
     */
    private void handleHandshake(Socket clientSocket) {
        SocketClientConnection connection = null;
        try {
            // Bound the handshake so a silent peer cannot leak this thread forever.
            clientSocket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            connection = new SocketClientConnection(clientSocket);

            Object first = connection.receiveMessage();
            if (!(first instanceof LoginRequest req) || req.nickname() == null || req.nickname().isBlank()) {
                connection.sendMessage(new ErrorMessage("LOGIN", "Expected LoginRequest with non-empty nickname"));
                connection.close();
                return;
            }

            // After the handshake the connection must be free to block on in-game reads.
            clientSocket.setSoTimeout(0);

            int playerId = mainController.assignPlayerId();
            ClientHandler handler = new ClientHandler(connection, mainController, playerId, req.nickname());

            // Send LoginResult BEFORE registerClient so the LobbyUpdate broadcast
            // triggered inside registerClient cannot reach this client before its
            // own LoginResult. On TCP both messages share the stream, so the order
            // must be enforced here; RMI gets this for free because login() returns
            // synchronously to the caller before any async callback fires.
            // The "isFirst" check reads MainController state directly: it inherits
            // the same host-election race already present in the RMI path, where
            // assignPlayerId + registerClient are not atomic either.
            boolean isFirst = mainController.getLobbySize() == 0 && !mainController.isGameInProgress();
            connection.sendMessage(new LoginResult(playerId, isFirst));

            mainController.registerClient(handler);
            activeHandlers.add(handler);

            Thread t = new Thread(handler, "client-handler-tcp-" + playerId);
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            if (connection != null) {
                connection.close();
            } else {
                try { clientSocket.close(); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Signals the server to stop, releases the {@link ServerSocket} resource
     * and closes every active {@link ClientHandler} so its blocked reader
     * thread can exit. {@link ClientHandler#close()} is idempotent, so it is
     * safe to call on handlers that already terminated on their own.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
        for (ClientHandler h : activeHandlers) {
            h.close();
        }
        activeHandlers.clear();
    }
}
