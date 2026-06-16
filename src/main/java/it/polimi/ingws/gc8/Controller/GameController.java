package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.states.*;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.MesosGame;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Network.ViewUpdater;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntPredicate;

/**
 * Main game controller. Owns a single game instance and uses the State pattern
 * to delegate action handling to the current {@link GameState}.
 *
 * Thread safety: all model mutations are serialized through {@code gameLock}.
 * Multiple client threads may call {@link #handleAction} concurrently.
 */
public class GameController {

    private final GameModel model;
    private final ViewUpdater viewUpdater;
    private final ReentrantLock gameLock;  //più client inviano azioni contemporaneamente, ogni lock() deve avere il suo unlock()

    /** Pre-instantiated states, one per GamePhase. The "current" state is
     *  always derived from {@code model.getPhase()} via this map — we do not
     *  keep a separate field, to avoid two sources of truth. */
    private final Map<GamePhase, GameState> states;

    /** Fast lookup: playerId → Player model object. Set at game start. */
    private Map<Integer, Player> playerMap;

    /** Players in offer-track resolution order. Set by ActionResolutionState. */
    private List<Player> offerOrder;

    /** Index of the current player within offerOrder. */
    private int offerOrderIndex;

    /** FA3: tells whether a given playerId is currently disconnected.
     *  Set by {@link MainController}; defaults to "nobody disconnected". */
    private IntPredicate disconnectedCheck = id -> false;

    public GameController(ViewUpdater viewUpdater) {
        this.model = new MesosGame(viewUpdater);
        this.viewUpdater = viewUpdater;
        this.gameLock = new ReentrantLock();
        this.states = new EnumMap<>(GamePhase.class);
        this.playerMap = new HashMap<>();

        //creazione mappa
        states.put(GamePhase.SETUP, new SetupState());
        states.put(GamePhase.POSITIONING, new PositioningState());
        states.put(GamePhase.ACTION_RESOLUTION, new ActionResolutionState());
        states.put(GamePhase.END_ROUND, new EndRoundState());
        states.put(GamePhase.GAME_OVER, new GameOverState());
    }

    /**
     * Central action dispatch, called by {@link MainController}.
     * Acquires the game lock, then delegates to the current state
     * (derived from the model's current phase).
     */
    public void handleAction(PlayerAction action) {
        gameLock.lock();
        try {
            GameState current = states.get(model.getPhase());
            if (current != null) {
                current.handleAction(this, action);
            }
        } finally {
            gameLock.unlock();
        }
    }

    /**
     * Transitions the game to a new phase.
     * Called by {@link GameState} implementations when the phase should change.
     * Must be called while the gameLock is held.
     *
     * Calls onExit on the old state, asks the model to update its phase
     * (which notifies observers), then calls onEnter on the new state.
     */
    public void transitionTo(GamePhase newPhase) {
        GameState oldState = states.get(model.getPhase());
        if (oldState != null) {
            oldState.onExit(this);
        }

        model.setPhase(newPhase);

        GameState newState = states.get(newPhase);
        if (newState != null) {
            newState.onEnter(this);
        }
    }

    /**
     * Initializes and starts the game.
     * Called by {@link MainController} when enough players are in the lobby.
     *
     * @param players list of players (2-5)
     */
    public void startGame(List<Player> players) {
        gameLock.lock();
        try {
            model.setupGame(players.size(), players);
            playerMap.clear();
            for (Player p : model.getPlayers()) {
                playerMap.put(p.getPlayerId(), p);
            }
            transitionTo(GamePhase.POSITIONING);
        } finally {
            gameLock.unlock();
        }
    }

    /**
     * Forces the game to end immediately (FA3): used when the sole-survivor
     * timeout expires and the only connected player wins by default.
     * Transitions the model to {@link GamePhase#GAME_OVER} and broadcasts a
     * {@link it.polimi.ingws.gc8.Network.Messages.GameOverMessage}.
     *
     * @param endMessage human-readable summary of how the game ended
     */
    public void forceGameOver(String endMessage) {
        gameLock.lock();
        try {
            if (model.getPhase() != GamePhase.GAME_OVER) {
                transitionTo(GamePhase.GAME_OVER);
            }
            viewUpdater.notifyGameOver(model, endMessage);
        } finally {
            gameLock.unlock();
        }
    }

    public GameModel getModel() {
        return model;
    }

    public ViewUpdater getViewUpdater() {
        return viewUpdater;
    }

    public Player getPlayerById(int playerId) {
        return playerMap.get(playerId);
    }

    /**
     * FA3: registers the predicate used by game states to skip the turn of
     * disconnected players. Set by {@link MainController} after game start.
     */
    public void setDisconnectedCheck(IntPredicate check) {
        this.disconnectedCheck = (check == null) ? id -> false : check;
    }

    /** FA3: @return true if the given player is currently disconnected. */
    public boolean isDisconnected(int playerId) {
        return disconnectedCheck.test(playerId);
    }

    /**
     * FA3: a player disconnected while the game is running. Lets the current
     * state advance past them if they were the active player. Acquires the
     * game lock so it serializes with normal action handling.
     */
    public void onPlayerDisconnected() {
        gameLock.lock();
        try {
            GameState current = states.get(model.getPhase());
            if (current != null) {
                current.onCurrentPlayerDisconnected(this);
            }
        } finally {
            gameLock.unlock();
        }
    }

    public GameState getCurrentState() {
        return states.get(model.getPhase());
    }

    public Map<GamePhase, GameState> getStates() {
        return states;
    }

    //Offer order tracking (used by ActionResolutionState)

    public List<Player> getOfferOrder() {
        return offerOrder;
    }

    public void setOfferOrder(List<Player> offerOrder) {
        this.offerOrder = offerOrder;
    }

    public int getOfferOrderIndex() {
        return offerOrderIndex;
    }

    public void setOfferOrderIndex(int offerOrderIndex) {
        this.offerOrderIndex = offerOrderIndex;
    }
}
