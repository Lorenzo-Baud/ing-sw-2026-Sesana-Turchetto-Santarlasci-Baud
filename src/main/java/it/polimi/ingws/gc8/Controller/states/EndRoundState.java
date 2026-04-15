package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Model.ViewUpdater;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

import java.util.List;

/**
 * State active during {@link GamePhase#END_ROUND}.
 * Resolves event cards, manages row transitions, checks for era changes,
 * and advances to the next round.
 *
 * This state is fully automated: no player actions are needed.
 * All logic runs in {@link #onEnter}, which transitions to
 * {@link GamePhase#POSITIONING} or {@link GamePhase#GAME_OVER}.
 */
public class EndRoundState implements GameState {

    @Override
    public void handleAction(GameController context, PlayerAction action) {
        context.getViewUpdater().notifyError(
                action.getPlayerId(),
                "INVALID_ACTION",
                "No actions allowed during end of round."
        );
    }

    @Override
    public void onEnter(GameController context) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();

        // 1. Resolve event cards
        List<EventCard> events = model.getEventsToResolve();
        for (EventCard event : events) {
            event.resolveEvent(model.getPlayers());
        }

        // 2. Check if the game is over (tribe deck exhausted)
        if (model.isGameOver()) {
            model.calculateFinalScores();
            context.transitionTo(GamePhase.GAME_OVER);
            return;
        }

        // 3. Discard lower row tribe cards, move upper to lower
        model.discardLowerRow();
        model.moveUpperRowToLower();

        // 4. Refill upper row and advance round counter
        model.refreshUpperRow();

        // 5. Check for era transition
        boolean eraChanged = model.checkNewEra();

        // 6. Transition to next positioning phase
        context.transitionTo(GamePhase.POSITIONING);
    }

    @Override
    public void onExit(GameController context) {
        // No cleanup needed
    }
}
