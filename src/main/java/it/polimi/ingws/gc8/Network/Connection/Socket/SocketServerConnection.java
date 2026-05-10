package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketServerConnection implements ServerConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final BlockingQueue<LoginResult> loginQueue = new LinkedBlockingQueue<>();

    public SocketServerConnection() { }

    @Override
    public void connect(String host, int port) throws Exception {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public LoginResult login(String nickname) throws Exception {
        out.writeObject("LOGIN:" + nickname);
        out.reset();
        out.flush();
        return loginQueue.take();
    }

    @Override
    public void setExpectedPlayers(int n) throws Exception {
        out.writeObject("SET_EXPECTED_PLAYERS:" + n);
        out.reset();
        out.flush();
    }

    @Override
    public void requestStartGame() throws Exception {
        out.writeObject("REQUEST_START_GAME");
        out.reset();
        out.flush();
    }

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

    @Override
    public Object receive() throws Exception {
        Object obj = in.readObject();
        
        if (obj instanceof LoginResult) {
            loginQueue.offer((LoginResult) obj);
            return receive();
        }
        return obj;
    }

    @Override
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}