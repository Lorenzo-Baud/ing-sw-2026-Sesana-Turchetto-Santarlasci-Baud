package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Network.Connection.ClientConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Implementation of {@link ClientConnection} using Java Sockets for network communication.
 * Manages serialized object exchange through dedicated input and output streams.
 */
public class SocketClientConnection implements ClientConnection {

    private final Socket socket;
    private final ObjectOutputStream outStream;
    private final ObjectInputStream inStream;
    private volatile boolean connected;

    /**
     * Constructs a new connection and initializes object streams.
     * The output stream is flushed immediately to prevent header blocking.
     *
     * @param socket the established network socket.
     * @throws IOException if an error occurs during stream initialization.
     */
    public SocketClientConnection(Socket socket) throws IOException {
        this.socket = socket;

        this.outStream = new ObjectOutputStream(socket.getOutputStream());
        this.outStream.flush();
        this.inStream = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
    }

    /**
     * Serializes and sends an object to the remote endpoint.
     * Resets the stream after sending to ensure subsequent modifications to the object
     * are correctly transmitted.
     *
     * @param message the object to be transmitted.
     */
    @Override
    public synchronized void sendMessage(Object message) {
        if (!connected) return;
        try {
            outStream.writeObject(message);
            outStream.reset();
            outStream.flush();
        } catch (IOException e) {
            close();
        }
    }

    /**
     * Blocks until an object is received from the input stream.
     * Automatically triggers connection closure if a read error occurs.
     *
     * @return the deserialized object received from the socket.
     * @throws Exception if a network error or deserialization failure occurs.
     */
    @Override
    public Object receiveMessage() throws Exception {
        try {
            return inStream.readObject();
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    /**
     * Terminates the connection and safely closes all associated streams and the socket.
     */
    @Override
    public void close() {
        if (!connected) return;
        connected = false;
        try { if (inStream != null) inStream.close(); } catch (IOException ignored) {}
        try { if (outStream != null) outStream.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Evaluates the current state of the connection based on the internal flag
     * and the underlying socket status.
     *
     * @return {@code true} if the connection is active and the socket is open.
     */
    @Override
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }
}