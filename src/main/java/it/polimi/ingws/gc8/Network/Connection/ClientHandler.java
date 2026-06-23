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
 *
 * <p>The handler is transport-agnostic: it delegates all I/O to a
 * {@link ClientConnection}, so the same logic can be reused by both socket
 * and RMI implementations. It is responsible for routing incoming messages to
 * the correct phase of the client lifecycle:</p>
 *
 * <ul>
 *   <li><b>Pre-match</b>: list/create/join lobby requests.</li>
 *   <li><b>In-match</b>: player actions, lobby setup commands, and heartbeat
 *       messages.</li>
 * </ul>
 *
 * <p>The class also implements {@link GameEventListener} so it can receive
 * server-side model updates and forward them to the connected client.</p>
 */
public class ClientHandler implements Runnable, GameEventListener {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    /** Transport-specific connection used to receive and send messages. */
    private final ClientConnection connection;

    /** Registry used to list, create, and resolve matches during the lobby phase. */
    private final MatchRegistry registry;

    /** Current match assigned to this client, or {@code null} before join. */
    private volatile MainController match;

    /** Player identifier assigned when the client joins a match. */
    private volatile int playerId;
    // Not final: for TCP the handler is created at handshake (nickname still
    // unknown) and the nickname only arrives later with the create/join
    // request. RMI instead knows it at construction time.

    /** Player nickname associated with this connection. */
    private volatile String nickname;

    /**
     * Creates a handler bound to a specific connection and match registry.
     *
     * @param connection transport-specific client connection
     * @param registry registry used to discover and create matches
     * @param nickname initial nickname, if already known by the transport
     */
    public ClientHandler(ClientConnection connection, MatchRegistry registry, String nickname) {
        this.connection = connection;
        this.registry = registry;
        this.nickname = nickname;
        this.playerId = -1;
    }

    /**
     * Associates this client with an active match and assigns the player id.
     *
     * @param match active match controller
     * @param playerId id assigned to the connected player inside the match
     */
    public void attachToMatch(MainController match, int playerId) {
        this.match = match;
        this.playerId = playerId;
    }

    /**
     * Main receive loop for the connection.
     *
     * <p>Until the connection closes, this method continuously receives
     * messages from the transport and dispatches them to the pre-match or
     * in-match handler depending on whether the client has already joined a
     * match.</p>
     */
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

    /**
     * Handles messages received before the client is attached to a match.
     *
     * <p>Supported requests are match listing, match creation, and match
     * joining. Any other non-null message is rejected with an error.</p>
     *
     * @param message decoded message received from the client transport
     */
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

    /**
     * Handles messages received once the client has joined a match.
     *
     * <p>This phase accepts player actions, lobby setup commands, start-game
     * requests, and heartbeat messages. Messages that do not match the active
     * protocol are rejected with an error.</p>
     *
     * @param message decoded message received from the client transport
     */
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

    /**
     * Forwards a server-generated network update to the connected client.
     *
     * <p>The message is sent only if the connection is still alive.</p>
     *
     * @param updateMessage network message produced by the match controller
     */
    @Override
    public void onGameUpdate(NetworkMessage updateMessage) {
        // Se la connessione è attiva, spediamo il messaggio (es. un GameDTO) al client.
        if (connection.isConnected()) {
            connection.sendMessage(updateMessage);
        }
    }

    //Helpers

    /**
     * Sends a generic error response to the client.
     *
     * @param message human-readable error detail
     */
    public void sendError(String message) {
        connection.sendMessage(new ErrorMessage("GENERIC", message));
    }

    //Getters
    /**
     * Returns the player id assigned to this client.
     *
     * @return the current player id, or {@code -1} before join
     */
    @Override
    public int getPlayerId() {
        return playerId;
    }

    /**
     * Returns the nickname associated with this client connection.
     *
     * @return player nickname, possibly {@code null} before handshake
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Checks whether the underlying transport is still connected.
     *
     * @return {@code true} if the connection is still alive
     */
    public boolean isConnected() {
        return connection.isConnected();
    }

    /**
     * Closes the underlying client connection.
     */
    public void close() {
        connection.close();
    }
}
