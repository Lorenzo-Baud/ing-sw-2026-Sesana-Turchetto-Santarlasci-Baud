package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Application entry point for the server side.
 *
 * <p>Manages the lobby (host + expected size + auto-start), creates the
 * {@link GameController} when the game starts, and routes
 * {@link PlayerAction} messages to it.</p>
 */
public class MainController {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    /** Connected clients waiting in the lobby. */
    private final List<ClientHandler> lobby;

    /** Maps playerId → ClientHandler for routing and reconnection. */
    private final Map<Integer, ClientHandler> clientMap;

    /** The ViewUpdater that bridges GameController events to ClientHandlers. */
    private ViewUpdater viewUpdater;

    /** Active game controller. */
    private GameController gameController;

    /** Tracks whether a game is in progress. */
    private volatile boolean gameInProgress;

    /** Lock for lobby operations, separate from GameController's gameLock. */
    private final Object lobbyLock;

    /** Counter for assigning player IDs. */
    private int nextPlayerId;

    /** Player id of the lobby creator (host); {@code -1} when no lobby exists. */
    private int hostPlayerId;

    /** Expected number of players for this match; {@code 0} until the host chooses. */
    private int expectedPlayers;

    public MainController() {
        this.lobby = new CopyOnWriteArrayList<>();
        this.clientMap = new ConcurrentHashMap<>();
        this.lobbyLock = new Object();
        this.gameInProgress = false;
        this.nextPlayerId = 1;
        this.hostPlayerId = -1;
        this.expectedPlayers = 0;
    }

    /**
     * Called when a new client connects.
     * Creates a {@link ClientHandler} entry, adds it to the lobby,
     * notifies all lobby members of the updated state and auto-starts
     * the game if the expected number of players has been reached.
     *
     * @param clientHandler the newly connected client
     */
    public void registerClient(ClientHandler clientHandler) {
        synchronized (lobbyLock) {
            if (gameInProgress) {
                clientHandler.sendError("Game already in progress. Cannot join.");
                return;
            }
            if (lobby.size() >= MAX_PLAYERS) {
                clientHandler.sendError("Lobby is full.");
                return;
            }

            // First registered player becomes the host (lobby creator).
            if (hostPlayerId == -1) {
                hostPlayerId = clientHandler.getPlayerId();
            }

            lobby.add(clientHandler);
            clientMap.put(clientHandler.getPlayerId(), clientHandler);
            broadcastLobbyUpdate();
            maybeAutoStart();
        }
    }

    /**
     * Called when a client disconnects.
     * If in lobby, removes them and updates the others.
     * If a game is in progress, notifies the others about the disconnection.
     *
     * @param clientHandler the disconnected client
     */
    public void unregisterClient(ClientHandler clientHandler) {
        synchronized (lobbyLock) {
            if (!gameInProgress) {
                lobby.remove(clientHandler);
                clientMap.remove(clientHandler.getPlayerId());
                if (viewUpdater != null) {
                    viewUpdater.removeListener(clientHandler);
                }
                if (clientHandler.getPlayerId() == hostPlayerId) {
                    // Transfer host to next remaining player, if any.
                    hostPlayerId = lobby.isEmpty() ? -1 : lobby.get(0).getPlayerId();
                }
                broadcastLobbyUpdate();
            } else {
                if (viewUpdater != null) {
                    viewUpdater.notifyPlayerDisconnected(clientHandler.getNickname());
                }
                // TODO: implementare logica pause/timeout
            }
        }
    }

    /**
     * Sets the expected number of players for the lobby. Allowed only to
     * the host while the game has not yet started; invalid invocations
     * are silently ignored.
     *
     * @param playerId id of the requesting player (must be the host)
     * @param n        expected number of players
     */
    public void setExpectedPlayers(int playerId, int n) {
        synchronized (lobbyLock) {
            if (gameInProgress) return;
            if (playerId != hostPlayerId) return;
            if (n < MIN_PLAYERS || n > MAX_PLAYERS) return;
            this.expectedPlayers = n;
            broadcastLobbyUpdate();
            maybeAutoStart();
        }
    }

    /**
     * Manually requests the start of the game. Normally not needed: the
     * server auto-starts the match as soon as the lobby reaches
     * {@link #expectedPlayers}.
     */
    public void requestStartGame() {
        synchronized (lobbyLock) {
            tryStartGame();
        }
    }

    /**
     * Routes an incoming {@link PlayerAction} to the {@link GameController}.
     * Called by {@link ClientHandler} when it receives a player's action.
     *
     * @param action the action to dispatch
     */
    public void dispatchAction(PlayerAction action) {
        if (!gameInProgress || gameController == null) {
            ClientHandler sender = clientMap.get(action.getPlayerId());
            if (sender != null) {
                sender.sendError("No game in progress.");
            }
            return;
        }
        gameController.handleAction(action);
    }

    /**
     * Handles reconnection of a previously connected client.
     * Re-associates them with their player slot and sends a full game snapshot.
     *
     * @param clientHandler the reconnecting client
     * @param playerId      the player ID to reclaim
     */
    public void handleReconnection(ClientHandler clientHandler, int playerId) {
        synchronized (lobbyLock) {
            clientMap.put(playerId, clientHandler);
            if (gameInProgress && viewUpdater != null) {
                viewUpdater.addListener(clientHandler);
                viewUpdater.sendFullSnapshotTo(clientHandler, gameController.getModel());
                viewUpdater.notifyPlayerReconnected(clientHandler.getNickname());
            }
        }
    }

    /**
     * @return the next available player ID
     */
    public int assignPlayerId() {
        synchronized (lobbyLock) {
            return nextPlayerId++;
        }
    }

    /** @return true if a game is currently in progress */
    public boolean isGameInProgress() {
        return gameInProgress;
    }

    /** @return number of players currently in the lobby */
    public int getLobbySize() {
        return lobby.size();
    }

    /** @return true if the given player is the lobby host */
    public boolean isHost(int playerId) {
        synchronized (lobbyLock) {
            return playerId == hostPlayerId;
        }
    }

    /** @return the expected number of players for this match (0 if not set yet) */
    public int getExpectedPlayers() {
        synchronized (lobbyLock) {
            return expectedPlayers;
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /** Caller MUST hold {@link #lobbyLock}. */
    private void maybeAutoStart() {
        if (expectedPlayers > 0 && lobby.size() == expectedPlayers) {
            tryStartGame();
        }
    }

    /** Caller MUST hold {@link #lobbyLock}. */
    private void tryStartGame() {
        if (gameInProgress) return;
        if (lobby.size() < MIN_PLAYERS) return;

        gameInProgress = true;

        viewUpdater = new ViewUpdater();
        for (ClientHandler ch : lobby) {
            viewUpdater.addListener(ch);
        }

        gameController = new GameController(viewUpdater);
        TotemColor[] colors = TotemColor.values();
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < lobby.size(); i++) {
            ClientHandler ch = lobby.get(i);
            players.add(new Player(ch.getPlayerId(), ch.getNickname(), colors[i]));
        }
        gameController.startGame(players);    }

    /**
     * Caller MUST hold {@link #lobbyLock}.
     * Broadcasts the raw expectedPlayers (0 = not yet chosen by the host);
     * the View is responsible for rendering 0 as "?" or similar.
     */
    private void broadcastLobbyUpdate() {
        List<String> names = new ArrayList<>();
        for (ClientHandler ch : lobby) {
            names.add(ch.getNickname());
        }
        LobbyUpdateMessage msg = new LobbyUpdateMessage(lobby.size(), expectedPlayers, names);
        for (ClientHandler ch : lobby) {
            ch.onGameUpdate(msg);
        }
    }
}
