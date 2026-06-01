package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Controller.MatchRegistry;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketServer implements Runnable {

    private static final int HANDSHAKE_TIMEOUT_MS = 5000;

    private final int port;
    private final MatchRegistry registry;
    private final AtomicInteger handshakeCounter = new AtomicInteger();
    private final List<ClientHandler> activeHandlers = new CopyOnWriteArrayList<>();

    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public SocketServer(int port, MatchRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

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

    private void handleHandshake(Socket clientSocket) {
        SocketClientConnection connection = null;
        try {
            connection = new SocketClientConnection(clientSocket);
            clientSocket.setSoTimeout(0);

            ClientHandler handler = new ClientHandler(connection, registry, null);
            activeHandlers.add(handler);

            Thread t = new Thread(handler, "client-handler-tcp-" + handshakeCounter.get());
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
