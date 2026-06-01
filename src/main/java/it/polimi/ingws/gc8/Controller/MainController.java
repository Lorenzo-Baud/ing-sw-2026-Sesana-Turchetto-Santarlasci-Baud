package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.Network.DTO.MatchStatus;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a single match: lobby, game start, and action routing.
 * One instance per match; coordinated by {@link MatchRegistry}.
 */
public class MainController {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    private final String matchId;
    private final MatchRegistry registry;

    private final List<ClientHandler> lobby;
    private final Map<Integer, ClientHandler> clientMap;
    private final Map<String, Integer> nickToPlayerId;

    private ViewUpdater viewUpdater;
    private GameController gameController;
    private volatile MatchStatus status;

    private final Object lobbyLock;
    private int nextPlayerId;
    private int hostPlayerId;
    private int expectedPlayers;
    private String hostNickname;

    public MainController(String matchId, MatchRegistry registry) {
        this.matchId = matchId;
        this.registry = registry;
        this.lobby = new CopyOnWriteArrayList<>();
        this.clientMap = new ConcurrentHashMap<>();
        this.nickToPlayerId = new ConcurrentHashMap<>();
        this.lobbyLock = new Object();
        this.status = MatchStatus.LOBBY;
        this.nextPlayerId = 1;
        this.hostPlayerId = -1;
        this.expectedPlayers = 0;
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
                // Reconnection to in-progress match
                clientHandler.attachToMatch(this, existingId);
                clientMap.put(existingId, clientHandler);
                if (viewUpdater != null) {
                    viewUpdater.addListener(clientHandler);
                    viewUpdater.sendFullSnapshotTo(clientHandler, gameController.getModel());
                    viewUpdater.notifyPlayerReconnected(nickname);
                }
                return new JoinResult(matchId, existingId, isHost(existingId));
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
            return new JoinResult(matchId, playerId, isHost(playerId));
        }
    }

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
                }
            } else {
                if (viewUpdater != null) {
                    viewUpdater.notifyPlayerDisconnected(clientHandler.getNickname());
                }
                // TODO: FA3 — logica pause/timeout
            }
        }
    }

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

    public void requestStartGame() {
        synchronized (lobbyLock) {
            tryStartGame();
        }
    }

    public void dispatchAction(PlayerAction action) {
        if (status != MatchStatus.IN_GAME || gameController == null) {
            ClientHandler sender = clientMap.get(action.getPlayerId());
            if (sender != null) {
                sender.sendError("No game in progress.");
            }
            return;
        }
        gameController.handleAction(action);
    }

    public boolean isHost(int playerId) {
        synchronized (lobbyLock) {
            return playerId == hostPlayerId;
        }
    }

    public String getMatchId() {
        return matchId;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public MatchInfo toMatchInfo() {
        synchronized (lobbyLock) {
            return new MatchInfo(matchId, hostNickname,
                    lobby.size(), expectedPlayers, status);
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

    private void tryStartGame() {
        if (status != MatchStatus.LOBBY) return;
        if (lobby.size() < MIN_PLAYERS) return;

        status = MatchStatus.IN_GAME;

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
        gameController.startGame(players);
    }

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
