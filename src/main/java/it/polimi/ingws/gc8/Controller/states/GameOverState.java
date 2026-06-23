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

    /**
     * Handles a player action during the game over state.
     * No actions are accepted, and an error message is sent to the player.
     *
     * @param context The game controller context.
     * @param action  The player action to handle.
     */
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
        return;
    }

    @Override
    public void onExit(GameController context) {
    }
}
