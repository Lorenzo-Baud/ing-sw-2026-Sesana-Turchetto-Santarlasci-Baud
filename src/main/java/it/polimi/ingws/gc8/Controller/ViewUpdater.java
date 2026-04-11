package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges the controller/model layer to the view layer.
 * Maintains a list of {@link GameEventListener} instances and broadcasts
 * game notifications to all of them.
 *
 * All notify methods build immutable data from the model and broadcast it.
 * ViewUpdater never holds a persistent reference to the model;
 * it receives it as a parameter when a full snapshot is needed.
 */
public class ViewUpdater {

    private final List<GameEventListener> listeners;

    public ViewUpdater() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void addListener(GameEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    //TODO: da fare dopo aver fatto i DTO. Struttura creata, mancano parametri d'ingresso e forse qualche metodo.

    //Game lifecycle notifications

    /**
     * Sends a game-started notification to all listeners.
     * Called at game start after setupGame().
     */
    public void notifyGameStarted(GameModel model) {
    }

    /**
     * Sends a full game snapshot to a single listener.
     * Used for reconnection.
     */
    public void sendFullSnapshotTo(GameEventListener listener, GameModel model) {}

    // Phase and turn notifications

    public void notifyPhaseChanged() {}

    public void notifyPlayerTurn() {}

    //Positioning phase

    public void notifyTotemPlaced() {}

    //Action resolution phase notifications

    public void notifyCardTaken() {}

    public void notifyFoodTaken() {}

    public void notifyTotemReturned() {}

    //End-of-round notifications

    public void notifyEventResolved() {}

    public void notifyRoundEnded() {}

    //Game over

    public void notifyGameOver() {}

    //Error notifications

    public void notifyError() {}

    //Lobby notifications

    public void notifyLobbyUpdate() {}

    //Connection notifications

    public void notifyPlayerDisconnected() {
        }

    public void notifyPlayerReconnected() {
    }
}
