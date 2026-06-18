package it.polimi.ingws.gc8.Network.Connection;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Controller.MatchRegistry;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.CreateMatchRequest;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.JoinMatchRequest;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.ListMatchesRequest;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.ListMatchesResponse;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.RequestStartGameCommand;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.SetExpectedPlayersCommand;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.Network.Messages.PongMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the bidirectional communication with a single connected player.
 * Transport-agnostic: delegates all I/O to a {@link ClientConnection}.
 *
 * <p>Has two phases: pre-match (match == null, handles list/create/join)
 * and in-match (handles PlayerAction and lobby commands).</p>
 */
public class ClientHandler implements Runnable, GameEventListener {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final ClientConnection connection;
    private final MatchRegistry registry;
    private volatile MainController match;
    private volatile int playerId;
    // Not final: for TCP the handler is created at handshake (nickname still
    // unknown) and the nickname only arrives later with the create/join
    // request. RMI instead knows it at construction time.
    private volatile String nickname;

    public ClientHandler(ClientConnection connection, MatchRegistry registry, String nickname) {
        this.connection = connection;
        this.registry = registry;
        this.nickname = nickname;
        this.playerId = -1;
    }

    public void attachToMatch(MainController match, int playerId) {
        this.match = match;
        this.playerId = playerId;
    }

    @Override
    public void run() {
        try {
            while (connection.isConnected()) {
                Object message = connection.receiveMessage();

                if (match == null) {
                    handlePreMatchMessage(message);
                } else {
                    handleInMatchMessage(message);
                }
            }
        } catch (Exception e) {
            // Connection lost (EOF when the client quits) or protocol error.
            // Logged at FINE so it is available when debugging but does not
            // spam the console on normal disconnections.
            LOGGER.log(Level.FINE, "Client handler terminated", e);
        } finally {
            connection.close();
            if (match != null) {
                match.unregisterClient(this);
            }
        }
    }

    private void handlePreMatchMessage(Object message) {
        if (message instanceof ListMatchesRequest) {
            connection.sendMessage(new ListMatchesResponse(registry.listOpenMatches()));

        } else if (message instanceof CreateMatchRequest req) {
            try {
                this.nickname = req.nickname();
                MainController newMatch = registry.createMatch(req.matchName());
                JoinResult result = newMatch.joinOrReconnect(req.nickname(), this);
                connection.sendMessage(result);
            } catch (IllegalStateException e) {
                sendError(e.getMessage());
            }

        } else if (message instanceof JoinMatchRequest req) {
            try {
                MainController target = registry.getMatch(req.matchId());
                if (target == null) {
                    sendError("Match non trovato: " + req.matchId());
                    return;
                }
                this.nickname = req.nickname();
                JoinResult result = target.joinOrReconnect(req.nickname(), this);
                connection.sendMessage(result);
                // FA2/FA3: deliver the snapshot only AFTER the JoinResult is on the
                // wire, so it does not desync the synchronous lobby handshake.
                if (result.reconnected()) {
                    target.completeReconnection(this);
                }
            } catch (IllegalStateException e) {
                sendError(e.getMessage());
            }

        } else if (message != null) {
            sendError("Devi prima creare o unirti a una partita.");
        }
    }

    private void handleInMatchMessage(Object message) {
        if (message instanceof PlayerAction action) {
            if (action.getPlayerId() != this.playerId) {
                sendError("Cannot submit actions for another player.");
                return;
            }
            match.dispatchAction(action);

        } else if (message instanceof PongMessage) {
            // FA3: heartbeat reply (TCP path); refresh this player's liveness.
            match.onPong(this.playerId);

        } else if (message instanceof SetExpectedPlayersCommand cmd) {
            match.setExpectedPlayers(this.playerId, cmd.n());

        } else if (message instanceof RequestStartGameCommand) {
            match.requestStartGame();

        } else if (message != null) {
            sendError("Unsupported message: " + message.getClass().getSimpleName());
        }
    }

    @Override
    public void onGameUpdate(NetworkMessage updateMessage) {
        // Se la connessione è attiva, spediamo il messaggio (es. un GameDTO) al client.
        if (connection.isConnected()) {
            connection.sendMessage(updateMessage);
        }
    }

    //Helpers

    public void sendError(String message) {
        connection.sendMessage(new ErrorMessage("GENERIC", message));
    }

    //Getters
    @Override
    public int getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void close() {
        connection.close();
    }
}
