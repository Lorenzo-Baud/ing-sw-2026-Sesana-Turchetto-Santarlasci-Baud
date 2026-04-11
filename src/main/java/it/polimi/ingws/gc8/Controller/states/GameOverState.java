package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

/**
 * State active during {@link GamePhase#GAME_OVER}.
 * The game has ended. Final scores have been calculated.
 * No player actions are accepted.
 */
public class GameOverState implements GameState {

    @Override
    public void handleAction(GameController context, PlayerAction action) {
        context.getViewUpdater().notifyError(
                action.getPlayerId(),
                "GAME_OVER",
                "The game is over. No actions are accepted."
        );
    }

    @Override
    public void onEnter(GameController context) {
        // Notify all clients of the final result
        context.getViewUpdater().notifyGameOver(context.getModel());
    }

    @Override
    public void onExit(GameController context) {
        // No cleanup needed
    }
}
