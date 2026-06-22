package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;

/**
 * State active during {@link it.polimi.ingws.gc8.Model.Enum.GamePhase#SETUP}.
 * The game is being initialized. No player actions are accepted..
 */
public class SetupState implements GameState {

    @Override
    public void handleAction(GameController context, PlayerAction action) {
        context.getViewUpdater().notifyError(
                action.getPlayerId(),
                "INVALID_ACTION",
                "Game is not started yet."
        );
    }

    @Override
    public void onEnter(GameController context) {
    }

    @Override
    public void onExit(GameController context) {
    }
}
