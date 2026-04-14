package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.ClientHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Application entry point for the server side.
 *
 * Manages the lobby,
 * creates the {@link GameController} when the game starts,
 * and routes {@link PlayerAction} messages to it.
 */
public class MainController {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 5;

    /** Connected clients waiting in the lobby. */
    private final List<ClientHandler> lobby;

    /** Maps playerId → ClientHandler for routing and reconnection.
     *
     */
    private final Map<Integer, ClientHandler> clientMap;

    /** The ViewUpdater that bridges GameController events to ClientHandlers. */
    private ViewUpdater viewUpdater;

    /** Active game controller */
    private GameController gameController;

    /** Tracks whether a game is in progress. */
    private volatile boolean gameInProgress;

    /** Lock for lobby operations, separate from GameController's gameLock. */
    private final Object lobbyLock;

    /** Counter for assigning player IDs. */
    private int nextPlayerId;

    public MainController() {
        this.lobby = new CopyOnWriteArrayList<>();
        this.clientMap = new ConcurrentHashMap<>();
        this.lobbyLock = new Object();
        this.gameInProgress = false;
        this.nextPlayerId = 1;
    }

    /**
     * Called when a new client connects.
     * Creates a {@link ClientHandler} entry, adds it to the lobby,
     * and notifies all lobby members of the updated lobby state.
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

            lobby.add(clientHandler);
            clientMap.put(clientHandler.getPlayerId(), clientHandler);
            broadcastLobbyUpdate();
        }
    }

    /**
     * Called when a client disconnects.
     * If in lobby, removes them and updates others.
     * If game is in progress, notifies other players.
     *
     * @param clientHandler the disconnected client
     */
    public void unregisterClient(ClientHandler clientHandler) {
        synchronized (lobbyLock) {
            if (!gameInProgress) {
                lobby.remove(clientHandler);
                clientMap.remove(clientHandler.getPlayerId());
                broadcastLobbyUpdate();
            } else {
                // Game in progress: notify others about disconnection
                if (viewUpdater != null) {
                    viewUpdater.notifyPlayerDisconnected(clientHandler.getNickname());
                }
                // TODO: implementare logica pause/timeout
            }
        }
    }

    /**
     * Called when a client requests to start the game.
     * Validates player count, creates the ViewUpdater and GameController,
     * registers all ClientHandlers as listeners, and starts the game.
     */
    public void requestStartGame() {
        synchronized (lobbyLock) {
            if (gameInProgress) return;
            if (lobby.size() < MIN_PLAYERS) {
                // Not enough players
                return;
            }

            gameInProgress = true;

            // Create ViewUpdater and register all current clients as listeners
            viewUpdater = new ViewUpdater();
            for (ClientHandler ch : lobby) {
                viewUpdater.addListener(ch);
            }

            // Create GameController and start the game
            gameController = new GameController(viewUpdater);
            gameController.startGame(lobby.size());
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

    /**
     * @return true if a game is currently in progress
     */
    public boolean isGameInProgress() {
        return gameInProgress;
    }

    /**
     * @return number of players currently in the lobby
     */
    public int getLobbySize() {
        return lobby.size();
    }

    private void broadcastLobbyUpdate() {
        // TODO: fare dopo aver creato i DTO
    }
}
