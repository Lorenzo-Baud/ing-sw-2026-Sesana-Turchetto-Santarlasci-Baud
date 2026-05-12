package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main server process that listens for incoming TCP connections.
 * This class implements {@link Runnable} to manage the server lifecycle and
 * handles the initial handshake protocol for client registration.
 */
public class SocketServer implements Runnable {

    private final int port;
    private final MainController mainController;
    private ServerSocket serverSocket;
    private boolean running;

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
     * Continuously accepts new client connections and delegates the handshake
     * process to separate threads to ensure non-blocking operation.
     */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("[Server] Socket Server listening on port: " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    SocketClientConnection connection = new SocketClientConnection(clientSocket);

                    // Offload handshake to prevent blocking the accept loop
                    new Thread(() -> handleHandshake(connection)).start();

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
     * Validates the login protocol, assigns unique IDs, determines player status,
     * and initializes a {@link ClientHandler} upon successful registration.
     *
     * @param connection the established client connection to validate.
     */
    private void handleHandshake(SocketClientConnection connection) {
        try {
            Object req = connection.receiveMessage();

            if (req instanceof String str && str.startsWith("LOGIN:")) {
                String nickname = str.substring(6);

                int playerId = mainController.assignPlayerId();
                boolean isFirst = (mainController.getLobbySize() == 0 && !mainController.isGameInProgress());

                LoginResult result = new LoginResult(playerId, isFirst);
                connection.sendMessage(result);

                ClientHandler handler = new ClientHandler(connection, mainController, playerId, nickname);
                mainController.registerClient(handler);

                new Thread(handler).start();
            } else {
                connection.close();
            }
        } catch (Exception e) {
            connection.close();
        }
    }

    /**
     * Signals the server to stop and releases the {@link ServerSocket} resource.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
    }
}