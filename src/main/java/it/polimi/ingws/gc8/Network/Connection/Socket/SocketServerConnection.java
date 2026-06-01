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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketServerConnection implements ServerConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Object writeLock = new Object();
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();

    private volatile boolean connected;
    private volatile boolean readerStarted;
    private Thread readerThread;

    public SocketServerConnection() { }

    @Override
    public void connect(String host, int port) throws Exception {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.connected = true;
    }

    @Override
    public List<MatchInfo> listMatches() throws Exception {
        ensureConnected();
        writeOnLock(new ListMatchesRequest());
        Object resp = waitForResponse(ListMatchesResponse.class);
        return ((ListMatchesResponse) resp).matches();
    }

    @Override
    public JoinResult createMatch(String nickname) throws Exception {
        ensureConnected();
        writeOnLock(new CreateMatchRequest(nickname));
        Object resp = waitForResponse(JoinResult.class);
        if (resp instanceof ErrorMessage err) {
            throw new IOException(err.getError());
        }
        startReaderThread();
        return (JoinResult) resp;
    }

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

    @Override
    public void setExpectedPlayers(int n) throws Exception {
        ensureConnected();
        writeOnLock(new SetExpectedPlayersCommand(n));
    }

    @Override
    public void requestStartGame() throws Exception {
        ensureConnected();
        writeOnLock(new RequestStartGameCommand());
    }

    @Override
    public void sendAction(Object action) {
        if (!connected) return;
        try {
            writeOnLock(action);
        } catch (IOException e) {
            disconnect();
        }
    }

    @Override
    public Object receive() throws Exception {
        ensureConnected();
        return responseQueue.take();
    }

    @Override
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        if (readerThread != null) readerThread.interrupt();
    }

    @Override
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    private Object waitForResponse(Class<?> expectedType) throws Exception {
        while (true) {
            Object obj = in.readObject();
            if (expectedType.isInstance(obj) || obj instanceof ErrorMessage) {
                return obj;
            }
            responseQueue.offer(obj);
        }
    }

    private void startReaderThread() {
        if (readerStarted) return;
        readerStarted = true;
        readerThread = new Thread(() -> {
            try {
                while (connected) {
                    Object obj = in.readObject();
                    responseQueue.offer(obj);
                }
            } catch (Exception e) {
                connected = false;
            }
        }, "tcp-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

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
