package it.polimi.ingws.gc8.Controller.events;

import it.polimi.ingws.gc8.Model.Enum.GamePhase;

/**
 * Listener interface for game-state notifications.
 * Each ClientHandler registers itself as a GameEventListener
 * to receive updates from ViewUpdater.
 */


public interface GameEventListener {

    public void onGameUpdate(Object updateMessage);

    public int getPlayerId();
}
