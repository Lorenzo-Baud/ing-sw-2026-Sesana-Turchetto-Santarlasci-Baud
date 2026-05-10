package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Network.Connection.ClientConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClientConnection implements ClientConnection {

    private final Socket socket;
    private final ObjectOutputStream outStream;
    private final ObjectInputStream inStream;
    private volatile boolean connected;

    public SocketClientConnection(Socket socket) throws IOException {
        this.socket = socket;

        this.outStream = new ObjectOutputStream(socket.getOutputStream());
        this.outStream.flush();
        this.inStream = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
    }

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

    @Override
    public Object receiveMessage() throws Exception {
        try {
            return inStream.readObject();
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        if (!connected) return;
        connected = false;
        try { if (inStream != null) inStream.close(); } catch (IOException ignored) {}
        try { if (outStream != null) outStream.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }
}