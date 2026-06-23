package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.Network.DTO.MatchStatus;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.PingMessage;
import it.polimi.ingws.gc8.Persistence.MatchSnapshot;
import it.polimi.ingws.gc8.Persistence.PersistenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a single match: lobby, game start, and action routing.
 * One instance per match; coordinated by {@link MatchRegistry}.
 */
public class MainController {

    /** Minimum number of players */
    public static final int MIN_PLAYERS = 2;
    /** Maximum number of players */
    public static final int MAX_PLAYERS = 5;

    /** Minimum number of players to continue the game */
    public static final int MIN_TO_CONTINUE = 2;
    /** FA3: if only one player is left connected, they have this long to wait for others before winning by default. */
    public static final long SOLE_SURVIVOR_TIMEOUT_MS = 60_000L;
    /** FA3: ping interval and timeout for detecting silent disconnects. */
    public static final long PING_INTERVAL_MS = 10_000L;
    /** FA3: if a player does not reply to a ping within this time, they are considered disconnected. */
    public static final long PING_TIMEOUT_MS = 25_000L;

    /** Persistence: fallback save cadence if no action triggered a save in the meantime. */
    public static final long PERSIST_INTERVAL_MS = 30_000L;

    /** Logger for the MainController class */
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    /** Match id */
    private final String matchId;

    private final MatchRegistry registry;

    /** Persistence manager */
    private final PersistenceManager pm;
    /** Persistence executor */
    private ExecutorService persistExec;
    /** True if the game over has been handled */
    private final AtomicBoolean gameOverHandled = new AtomicBoolean(false);

    /** Clients connected to the lobby */
    private final List<ClientHandler> lobby;
    /** Maps containing clients and their id */
    private final Map<Integer, ClientHandler> clientMap;
    /** Map containing player id and nicknames */
    private final Map<String, Integer> nickToPlayerId;

    /** FA3: per-player connection state, populated when the game starts. */
    private final Map<Integer, ConnectionState> connState;

    /** FA3: last time (ms) a pong was received from each player. */
    private final Map<Integer, Long> lastPongMs;

    /** View updater object */
    private ViewUpdater viewUpdater;
    /** Game controller onject*/
    private GameController gameController;
    /** Match status */
    private volatile MatchStatus status;

    /** Lobby lock */
    private final Object lobbyLock;
    /** Next player id */
    private int nextPlayerId;
    /** Host player id */
    private int hostPlayerId;
    /** Expected number of players in the game */
    private int expectedPlayers;
    /** Host nickname */
    private String hostNickname;

    /** FA3: true while the match is suspended waiting for more players. */
    private volatile boolean suspended;
    /** FA3: per-match scheduler for the sole-survivor timeout. */
    private final ScheduledExecutorService scheduler;
    /** FA3: pending sole-survivor timeout task (guarded by lobbyLock). */
    private ScheduledFuture<?> soleSurvivorTask;

    /**
     * Constructor for MainController
     * @param matchId match id
     * @param registry registry of previous matches
     */
    public MainController(String matchId, MatchRegistry registry) {
        this(matchId, registry, null);
    }

    /**
     * Constructor for MainController with persistence manager
     * @param matchId match id
     * @param registry registry of previous matches
     * @param pm persistance manager object
     */
    public MainController(String matchId, MatchRegistry registry, PersistenceManager pm) {
        this.matchId = matchId;
        this.registry = registry;
        this.pm = pm;
        this.lobby = new CopyOnWriteArrayList<>();
        this.clientMap = new ConcurrentHashMap<>();
        this.nickToPlayerId = new ConcurrentHashMap<>();
        this.connState = new ConcurrentHashMap<>();
        this.lastPongMs = new ConcurrentHashMap<>();
        this.lobbyLock = new Object();
        this.status = MatchStatus.LOBBY;
        this.nextPlayerId = 1;
        this.hostPlayerId = -1;
        this.expectedPlayers = 0;
        this.suspended = false;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "match-scheduler-" + matchId);
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Joins a player to this match or reconnects them if already known.
     *
     * @param nickname      the player's nickname
     * @param clientHandler the handler for this client
     * @return the join result with matchId, playerId, and host flag
     * @throws IllegalStateException if nickname is taken, lobby full, or match in progress with unknown nick
     */
    public JoinResult joinOrReconnect(String nickname, ClientHandler clientHandler) {
        synchronized (lobbyLock) {
            Integer existingId = nickToPlayerId.get(nickname);

            if (existingId != null) {
                if (status == MatchStatus.LOBBY) {
                    throw new IllegalStateException("Nickname già in uso in questa lobby");
                }

                if (connState.get(existingId) == ConnectionState.CONNECTED) {
                    throw new IllegalStateException("Nickname già attivo in questa partita");
                }
                clientHandler.attachToMatch(this, existingId);
                clientMap.put(existingId, clientHandler);
                connState.put(existingId, ConnectionState.CONNECTED);
                lastPongMs.put(existingId, System.currentTimeMillis());
                if (viewUpdater != null) {
                    viewUpdater.notifyPlayerReconnected(nickname);
                }
                maybeResumeAfterReconnect();
                // reconnected = true: the client must skip lobby setup.
                return new JoinResult(matchId, existingId, isHost(existingId), true);
            }

            if (status != MatchStatus.LOBBY) {
                throw new IllegalStateException("Match in corso, nickname non riconosciuto");
            }
            if (lobby.size() >= MAX_PLAYERS) {
                throw new IllegalStateException("Lobby piena");
            }

            int playerId = nextPlayerId++;
            nickToPlayerId.put(nickname, playerId);
            clientHandler.attachToMatch(this, playerId);

            if (hostPlayerId == -1) {
                hostPlayerId = playerId;
                hostNickname = nickname;
            }

            lobby.add(clientHandler);
            clientMap.put(playerId, clientHandler);
            broadcastLobbyUpdate();
            maybeAutoStart();
            return new JoinResult(matchId, playerId, isHost(playerId), false);
        }
    }

    /**
     * Completes a reconnection by attaching the client to the broadcast and
     * sending it the full snapshot. MUST be called by the transport layer
     * <strong>after</strong> the {@link JoinResult} response has been delivered,
     * so on TCP the snapshot never races ahead of the lobby handshake on the
     * shared stream. No-op if the match is not in game (e.g. a lobby join).
     *
     * @param clientHandler the reconnected client's handler
     */
    public void completeReconnection(ClientHandler clientHandler) {
        synchronized (lobbyLock) {
            if (status != MatchStatus.IN_GAME || viewUpdater == null || gameController == null) {
                return;
            }
            viewUpdater.addListener(clientHandler);
            viewUpdater.sendFullSnapshotTo(clientHandler, gameController.getModel());
        }
    }

    /**
     * Unregisters client from lobby
     * @param clientHandler client handler object
     */
    public void unregisterClient(ClientHandler clientHandler) {
        synchronized (lobbyLock) {
            if (status == MatchStatus.LOBBY) {
                lobby.remove(clientHandler);
                clientMap.remove(clientHandler.getPlayerId());
                nickToPlayerId.values().remove(Integer.valueOf(clientHandler.getPlayerId()));
                if (viewUpdater != null) {
                    viewUpdater.removeListener(clientHandler);
                }
                if (clientHandler.getPlayerId() == hostPlayerId) {
                    hostPlayerId = lobby.isEmpty() ? -1 : lobby.get(0).getPlayerId();
                    hostNickname = lobby.isEmpty() ? null : lobby.get(0).getNickname();
                }
                broadcastLobbyUpdate();
                if (lobby.isEmpty()) {
                    registry.removeMatch(matchId);
                    shutdown();
                }
            } else if (status == MatchStatus.FINISHED) {
                if (viewUpdater != null) {
                    viewUpdater.removeListener(clientHandler);
                }
            } else {
                int pid = clientHandler.getPlayerId();
                connState.put(pid, ConnectionState.DISCONNECTED);
                if (viewUpdater != null) {
                    viewUpdater.removeListener(clientHandler);
                    viewUpdater.notifyPlayerDisconnected(clientHandler.getNickname());
                }
                evaluateSuspensionAfterDisconnect();
                if (!suspended && gameController != null) {
                    gameController.onPlayerDisconnected();
                }
            }
        }
    }

    /**
     * Sets expected number of players inside the lobby
     * @param playerId
     * @param n expected number of players
     */
    public void setExpectedPlayers(int playerId, int n) {
        synchronized (lobbyLock) {
            if (status != MatchStatus.LOBBY) return;
            if (playerId != hostPlayerId) return;
            if (n < MIN_PLAYERS || n > MAX_PLAYERS) return;
            this.expectedPlayers = n;
            broadcastLobbyUpdate();
            maybeAutoStart();
        }
    }

    /**
     * Requests to start the game, if the caller is the host and the lobby has enough players.
     */
    public void requestStartGame() {
        synchronized (lobbyLock) {
            tryStartGame();
        }
    }

    /**
     * Dispatches actions
     * @param action player action
     */
    public void dispatchAction(PlayerAction action) {
        if (status != MatchStatus.IN_GAME || gameController == null) {
            ClientHandler sender = clientMap.get(action.getPlayerId());
            if (sender != null) {
                sender.sendError("No game in progress.");
            }
            return;
        }
        if (suspended) {
            if (viewUpdater != null) {
                viewUpdater.notifyError(action.getPlayerId(), "SUSPENDED",
                        "Partita sospesa: in attesa di altri giocatori.");
            }
            return;
        }
        gameController.handleAction(action);
        persistAsync();
    }

    /**
     * Returns true if the given player id is host
     * @param playerId player id
     * @return true of the given player id is host
     */
    public boolean isHost(int playerId) {
        synchronized (lobbyLock) {
            return playerId == hostPlayerId;
        }
    }

    /**
     * Returns match id
     * @return match id
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * Returns game status
     * @return gane status
     */
    public MatchStatus getStatus() {
        return status;
    }

    /**
     * Returns match info
     * @return match info
     */
    public MatchInfo toMatchInfo() {
        synchronized (lobbyLock) {
            return new MatchInfo(matchId, hostNickname,
                    lobby.size(), expectedPlayers, status);
        }
    }

    /** Caller MUST hold {@link #lobbyLock}. */
    private void maybeAutoStart() {
        if (expectedPlayers > 0 && lobby.size() == expectedPlayers) {
            tryStartGame();
        }
    }

    /**
     * Tries to start the game
     */
    private void tryStartGame() {
        if (status != MatchStatus.LOBBY) return;
        if (lobby.size() < MIN_PLAYERS) return;

        status = MatchStatus.IN_GAME;

        viewUpdater = new ViewUpdater();
        for (ClientHandler ch : lobby) {
            viewUpdater.addListener(ch);
        }

        gameController = new GameController(viewUpdater);
        gameController.setDisconnectedCheck(this::isDisconnected);
        gameController.setOnGameOver(this::handleGameOver);
        TotemColor[] colors = TotemColor.values();
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < lobby.size(); i++) {
            ClientHandler ch = lobby.get(i);
            players.add(new Player(ch.getPlayerId(), ch.getNickname(), colors[i]));
            connState.put(ch.getPlayerId(), ConnectionState.CONNECTED);
            lastPongMs.put(ch.getPlayerId(), System.currentTimeMillis());
        }
        gameController.startGame(players);

        scheduler.scheduleAtFixedRate(this::heartbeatTick,
                PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);

        startPersistence();
        persistAsync();
    }

    /**
     * Persistence: starts the non-daemon persist executor and the fallback periodic
     * save. Created lazily (only for matches that actually start) so a
     * lobby-only match never holds a non-daemon thread that would block JVM exit.
     */
    private void startPersistence() {
        if (pm == null || persistExec != null) return;
        persistExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "persist-" + matchId);
            t.setDaemon(false); // must finish flushing before the JVM exits
            return t;
        });
        scheduler.scheduleAtFixedRate(this::persistAsync,
                PERSIST_INTERVAL_MS, PERSIST_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Persistence: serializes the current match state and queues the disk write.
     * The model graph is serialized while holding the game lock (so it is
     * consistent), then the resulting bytes are written to disk off the lock on
     * the persist executor, so saving never lags game actions.
     */
    private void persistAsync() {
        ExecutorService exec = persistExec;
        if (pm == null || exec == null || gameController == null) return;
        if (status != MatchStatus.IN_GAME) return;

        final Map<String, Integer> nickCopy = new HashMap<>(nickToPlayerId);
        final int exp = expectedPlayers;
        final int hpid = hostPlayerId;
        final int npid = nextPlayerId;
        final String hn = hostNickname;
        final GameModel model = gameController.getModel();

        final byte[] data;
        try {
            data = gameController.withGameLock(() -> {
                MatchSnapshot snap = new MatchSnapshot(matchId, exp, hn, hpid, npid,
                        nickCopy, model, System.currentTimeMillis());
                try {
                    return PersistenceManager.toBytes(snap);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not serialize snapshot for " + matchId, e);
            return;
        }

        try {
            exec.submit(() -> {
                try {
                    pm.saveBytes(matchId, data);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not write snapshot for " + matchId, e);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
        }
    }

    /**
     * Persistence: rebuilds a suspended match from a snapshot after a server restart.
     * The model is reattached to a fresh (empty) {@link ViewUpdater}; the match
     * starts with every player marked DISCONNECTED and suspended, waiting for
     * {@link #MIN_TO_CONTINUE} players to reconnect by nickname before resuming.
     *
     * @param snap     the loaded snapshot
     * @param registry the registry that will own the rebuilt controller
     * @param pm       the persistence manager for ongoing saves and cleanup
     * @return the reattached controller, in IN_GAME + suspended state
     */
    public static MainController restore(MatchSnapshot snap, MatchRegistry registry,
                                         PersistenceManager pm) {
        MainController mc = new MainController(snap.matchId(), registry, pm);
        mc.status = MatchStatus.IN_GAME;
        mc.suspended = true;
        mc.expectedPlayers = snap.expectedPlayers();
        mc.hostPlayerId = snap.hostPlayerId();
        mc.hostNickname = snap.hostNickname();
        mc.nextPlayerId = snap.nextPlayerId();
        mc.nickToPlayerId.putAll(snap.nickToPlayerId());

        mc.viewUpdater = new ViewUpdater();
        mc.gameController = new GameController(mc.viewUpdater, snap.model());
        mc.gameController.setDisconnectedCheck(mc::isDisconnected);
        mc.gameController.setOnGameOver(mc::handleGameOver);

        for (Integer pid : snap.nickToPlayerId().values()) {
            mc.connState.put(pid, ConnectionState.DISCONNECTED);
        }

        mc.scheduler.scheduleAtFixedRate(mc::heartbeatTick,
                PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        mc.startPersistence();
        return mc;
    }

    /**
     * FA2: game-over cleanup, run exactly once. Stops the persist executor
     * (dropping any queued writes), deletes the on-disk snapshot, and removes
     * the match from the registry.
     */
    private void handleGameOver() {
        if (!gameOverHandled.compareAndSet(false, true)) return;
        status = MatchStatus.FINISHED;
        shutdown();
        if (pm != null) {
            pm.delete(matchId);
        }
        registry.removeMatch(matchId);
    }

    /** @return true if the given player is currently disconnected (FA3). */
    public boolean isDisconnected(int playerId) {
        return connState.get(playerId) == ConnectionState.DISCONNECTED;
    }

    /** FA3: records a heartbeat reply from the given player. */
    public void onPong(int playerId) {
        lastPongMs.put(playerId, System.currentTimeMillis());
    }

    /**
     * FA3 heartbeat: pings every connected client and closes connections that
     * have not replied within {@link #PING_TIMEOUT_MS}, catching silent drops
     * (cable pull, NAT timeout) that produce no EOF. Runs on the per-match
     * scheduler thread; closing a connection triggers the normal
     * {@link #unregisterClient} path on the client-handler thread.
     */
    private void heartbeatTick() {
        if (status != MatchStatus.IN_GAME) return;
        long now = System.currentTimeMillis();
        for (Integer id : new ArrayList<>(clientMap.keySet())) {
            if (connState.get(id) != ConnectionState.CONNECTED) continue;
            ClientHandler ch = clientMap.get(id);
            if (ch == null) continue;
            Long last = lastPongMs.get(id);
            if (last != null && now - last > PING_TIMEOUT_MS) {
                ch.close(); // -> run() finally -> unregisterClient -> DISCONNECTED
            } else {
                ch.onGameUpdate(new PingMessage());
            }
        }
    }

    /** @return number of players currently marked CONNECTED (FA3). */
    public int countConnected() {
        int n = 0;
        for (ConnectionState s : connState.values()) {
            if (s == ConnectionState.CONNECTED) n++;
        }
        return n;
    }

    /**
     * Caller MUST hold {@link #lobbyLock}. Evaluates whether the match must be
     * suspended after a player disconnected: if exactly one player is left
     * connected, suspend and arm the sole-survivor timeout.
     */
    private void evaluateSuspensionAfterDisconnect() {
        if (status != MatchStatus.IN_GAME) return;
        int connected = countConnected();
        if (connected == 1 && !suspended) {
            suspended = true;
            int survivor = onlyConnectedPlayerId();
            if (viewUpdater != null) {
                ClientHandler ch = clientMap.get(survivor);
                viewUpdater.notifyMatchSuspended(ch == null ? null : ch.getNickname());
            }
            scheduleSoleSurvivorTimeout();
        }
    }

    /**
     * Caller MUST hold {@link #lobbyLock}. Resumes a suspended match once enough
     * players are connected again, cancelling the sole-survivor timeout.
     */
    private void maybeResumeAfterReconnect() {
        if (!suspended) return;
        cancelSoleSurvivorTimeout();
        if (countConnected() >= MIN_TO_CONTINUE) {
            suspended = false;
            if (viewUpdater != null) {
                viewUpdater.notifyMatchResumed();
            }
        }
    }

    /** Caller MUST hold {@link #lobbyLock}. */
    private int onlyConnectedPlayerId() {
        for (Map.Entry<Integer, ConnectionState> e : connState.entrySet()) {
            if (e.getValue() == ConnectionState.CONNECTED) return e.getKey();
        }
        return -1;
    }

    /** Caller MUST hold {@link #lobbyLock}. */
    private void scheduleSoleSurvivorTimeout() {
        cancelSoleSurvivorTimeout();
        soleSurvivorTask = scheduler.schedule(this::declareSoleSurvivorWinner,
                SOLE_SURVIVOR_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /** Caller MUST hold {@link #lobbyLock}. */
    private void cancelSoleSurvivorTimeout() {
        if (soleSurvivorTask != null) {
            soleSurvivorTask.cancel(false);
            soleSurvivorTask = null;
        }
    }

    /**
     * Sole-survivor timeout fired: the only connected player wins by default.
     * Re-checks the condition under the lock to avoid a race with a last-moment
     * reconnection.
     */
    private void declareSoleSurvivorWinner() {
        GameController gc;
        String winner;
        synchronized (lobbyLock) {
            if (!suspended || status != MatchStatus.IN_GAME) return;
            if (countConnected() != 1) return;
            int survivor = onlyConnectedPlayerId();
            ClientHandler ch = clientMap.get(survivor);
            winner = ch == null ? "?" : ch.getNickname();
            gc = gameController;
            status = MatchStatus.FINISHED;
            soleSurvivorTask = null;
        }
        if (gc != null) {
            gc.forceGameOver("Vittoria di " + winner + " (unico giocatore rimasto connesso).");
        }
        shutdown();
        registry.removeMatch(matchId);
    }

    /**
     * Releases per-match resources (FA3 scheduler, FA2 persist executor).
     * Used on game over: queued snapshot writes are dropped because the
     * snapshot is about to be deleted anyway.
     */
    public void shutdown() {
        scheduler.shutdownNow();
        if (persistExec != null) {
            persistExec.shutdownNow();
        }
    }

    /**
     * Persistence: ordered shutdown for a still-running match (server stopping). Stops
     * the scheduler and lets the persist executor drain its queue so the last
     * snapshot reaches disk, then waits briefly for it.
     */
    public void flush() {
        scheduler.shutdownNow();
        ExecutorService exec = persistExec;
        if (exec == null) return;
        exec.shutdown();
        try {
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                exec.shutdownNow();
            }
        } catch (InterruptedException e) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sends lobby update to all the connected clients
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
