package it.polimi.ingws.gc8.Controller.events;

import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

/**
 * Listener interface for game-state notifications.
 * Each ClientHandler registers itself as a GameEventListener
 * to receive updates from ViewUpdater.
 */


public interface GameEventListener {

    public void onGameUpdate(NetworkMessage updateMessage);

    public int getPlayerId();
}
