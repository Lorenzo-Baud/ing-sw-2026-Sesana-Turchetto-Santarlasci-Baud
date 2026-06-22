package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

/**
 * State active when the game is paused (e.g., a player disconnected).
 * All player actions are rejected until the game resumes.
 *
 * The previous state is stored in {@link GameController} so the game
 * can resume from where it left off.
 */
public class PausedState implements GameState {

    private GamePhase previousPhase;

    @Override
    public void handleAction(GameController context, PlayerAction action) {
        context.getViewUpdater().notifyError(
                action.getPlayerId(),
                "GAME_PAUSED",
                "The game is paused. Please wait for all players to reconnect."
        );
    }

    @Override
    public void onEnter(GameController context) {
        this.previousPhase = context.getModel().getPhase();
    }

    @Override
    public void onExit(GameController context) {
    }
}