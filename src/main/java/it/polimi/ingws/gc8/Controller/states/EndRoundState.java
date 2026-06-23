package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import it.polimi.ingws.gc8.Network.Messages.EventResolvedMessage;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Handles action
     * @param context the GameController providing access to model and view updater
     * @param action  the action requested by a player
     */
    @Override
    public void handleAction(GameController context, PlayerAction action) {
        context.getViewUpdater().notifyError(
                action.getPlayerId(),
                "INVALID_ACTION",
                "No actions allowed during end of round."
        );
    }

    /**
     *
     * @param context the GameController
     */
    @Override
    public void onEnter(GameController context) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();

        // 1. Resolve event cards, capturing the prestige variation each one
        //    produces so the views can report what happened (and to whom).
        List<Player> players = model.getPlayers();
        List<EventResolvedMessage.ResolvedEvent> resolvedOutcomes = new ArrayList<>();
        List<EventCard> events = model.getEventsToResolve();
        for (EventCard event : events) {
            Map<Integer, Integer> before = new HashMap<>();
            for (Player p : players) before.put(p.getPlayerId(), p.getPrestigePoints());

            event.resolveEvent(players);

            List<EventResolvedMessage.PlayerDelta> deltas = new ArrayList<>();
            for (Player p : players) {
                int delta = p.getPrestigePoints() - before.get(p.getPlayerId());
                if (delta != 0) {
                    deltas.add(new EventResolvedMessage.PlayerDelta(p.getNickname(), delta));
                }
            }
            resolvedOutcomes.add(new EventResolvedMessage.ResolvedEvent(
                    event.getEventType().name(), event.isFinal(), deltas));
        }
        viewUpdater.notifyEventsResolved(resolvedOutcomes);

        // 2. Check if the game is over (tribe deck exhausted)
        if (model.isGameOver()) {
            model.calculateFinalScores();
            context.transitionTo(GamePhase.GAME_OVER);
            Player winner = model.getWinner();
            String endMessage = (winner != null)
                    ? "Partita terminata: vince " + winner.getNickname() + "!"
                    : "Partita terminata in parità.";
            viewUpdater.notifyGameOver(model, endMessage);
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
    }
}
