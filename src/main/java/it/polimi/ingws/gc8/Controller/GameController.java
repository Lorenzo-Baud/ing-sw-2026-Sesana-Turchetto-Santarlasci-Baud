package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.states.*;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.MesosGame;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

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

    /** Pre-instantiated states, one per GamePhase. */
    private final Map<GamePhase, GameState> states;

    /** Current state corresponding to the current GamePhase. */
    private GameState currentState;

    /** Fast lookup: playerId → Player model object. Set at game start. */
    private Map<Integer, Player> playerMap;

    /** Players in offer-track resolution order. Set by ActionResolutionState. */
    private List<Player> offerOrder;

    /** Index of the current player within offerOrder. */
    private int offerOrderIndex;

    public GameController(ViewUpdater viewUpdater) {
        this.model = new MesosGame();
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

        this.currentState = states.get(GamePhase.SETUP);
    }

    /**
     * Central action dispatch, called by {@link MainController}.
     * Acquires the game lock, then delegates to the current state.
     */
    public void handleAction(PlayerAction action) {
        gameLock.lock();
        try {
            if (currentState != null) {
                currentState.handleAction(this, action);
            }
        } finally {
            gameLock.unlock();
        }
    }

    /**
     * Transitions the game to a new phase.
     * Called by {@link GameState} implementations when the phase should change.
     * Must be called while the gameLock is held.
     */
    //chiama onExit() sullo stato vecchio, cambia stato, chiama onEnter() sul nuovo, notifica il cambio fase via viewUpdater
    public void transitionTo(GamePhase newPhase) {
        GamePhase oldPhase = model.getPhase();

        if (currentState != null) {
            currentState.onExit(this);
        }

        currentState = states.get(newPhase);

        if (currentState != null) {
            currentState.onEnter(this);
        }

        int currentPlayerId = model.getCurrentPlayer() != null
                ? model.getCurrentPlayer().getPlayerId() : -1;
        viewUpdater.notifyPhaseChanged(oldPhase, newPhase, currentPlayerId);
    }

    /**
     * Initializes and starts the game.
     * Called by {@link MainController} when enough players are in the lobby.
     *
     * @param numPlayers number of players (2-5)
     */
    public void startGame(int numPlayers) {
        gameLock.lock();
        try {
            model.setupGame(numPlayers);

            playerMap.clear();
            for (Player p : model.getPlayers()) {
                playerMap.put(p.getPlayerId(), p);
            }

            viewUpdater.notifyGameStarted(model);
            transitionTo(GamePhase.POSITIONING);
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

    public GameState getCurrentState() {
        return currentState;
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
