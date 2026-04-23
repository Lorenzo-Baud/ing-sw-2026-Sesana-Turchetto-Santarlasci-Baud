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

    public GameController(ViewUpdater viewUpdater) {
        this.model = new MesosGame();
        this.model.addObserver(viewUpdater);
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
     * @param numPlayers number of players (2-5)
     */
    public void startGame(int numPlayers) {
        gameLock.lock();
        try {
            model.setupGame(numPlayers, playerMap.values().stream().toList());

            playerMap.clear();
            for (Player p : model.getPlayers()) {
                playerMap.put(p.getPlayerId(), p);
            }


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
