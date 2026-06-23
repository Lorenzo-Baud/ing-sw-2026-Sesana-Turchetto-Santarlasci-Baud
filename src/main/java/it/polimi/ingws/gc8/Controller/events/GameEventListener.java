package it.polimi.ingws.gc8.Controller.events;

import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

/**
 * Listener interface for game-state notifications.
 * Each ClientHandler registers itself as a GameEventListener
 * to receive updates from ViewUpdater.
 */
public interface GameEventListener {

    /**
     * Method triggered on game update
     * @param updateMessage received message
     */
    public void onGameUpdate(NetworkMessage updateMessage);

    /**
     * Returns player id
     * @return player id
     */
    public int getPlayerId();
}
