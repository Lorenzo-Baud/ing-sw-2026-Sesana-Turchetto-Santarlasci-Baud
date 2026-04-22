package it.polimi.ingws.gc8.Network.Connection.Socket;

import it.polimi.ingws.gc8.Network.Connection.ClientConnection;

public class SocketClientConnection implements ClientConnection {
    @Override
    public void sendMessage(Object message) {

    }

    @Override
    public Object receiveMessage(){
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }
}

