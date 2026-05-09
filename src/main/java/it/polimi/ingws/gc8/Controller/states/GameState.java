package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;

/**
 * State pattern interface: each game phase validates and handles
 * actions differently. GameController delegates to the current state.
 */
public interface   GameState {

    /**
     * Processes a player action in the context of this state.
     * May call controller methods to update the model and notify views.
     *
     * @param context the GameController providing access to model and view updater
     * @param action  the action requested by a player
     */
    void handleAction(GameController context, PlayerAction action);

    /**
     * Called when the game transitions into this state.
     *
     * @param context the GameController
     */
    void onEnter(GameController context);

    /**
     * Called when the game transitions out of this state.
     *
     * @param context the GameController
     */
    void onExit(GameController context);
}
