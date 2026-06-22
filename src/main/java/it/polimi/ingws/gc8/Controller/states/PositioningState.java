package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import it.polimi.ingws.gc8.Controller.actions.PlaceTotemAction;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

import java.util.List;

/**
 * State active during {@link GamePhase#POSITIONING}.
 * Players place their totems on offer tiles, one at a time,
 * following the turn order.
 *
 * Accepts only {@link PlaceTotemAction}.
 * When all totems are placed, transitions to {@link GamePhase#ACTION_RESOLUTION}.
 */
public class PositioningState implements GameState {

    @Override
    public void handleAction(GameController context, PlayerAction action) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();

        if (!(action instanceof PlaceTotemAction)) {
            viewUpdater.notifyError(action.getPlayerId(), "INVALID_ACTION",
                    "Only totem placement is allowed during positioning phase.");
            return;
        }

        PlaceTotemAction placeAction = (PlaceTotemAction) action;
        Player player = context.getPlayerById(placeAction.getPlayerId());

        if (player == null) {
            viewUpdater.notifyError(placeAction.getPlayerId(), "INVALID_PLAYER",
                    "Player not found.");
            return;
        }

        Player currentPlayer = model.getCurrentPlayer();
        if (currentPlayer == null || currentPlayer.getPlayerId() != player.getPlayerId()) {
            viewUpdater.notifyError(player.getPlayerId(), "NOT_YOUR_TURN",
                    "It is not your turn to place a totem.");
            return;
        }

        OfferTile targetTile = findOfferTile(model, placeAction.getOfferTileId());
        if (targetTile == null || !targetTile.isFree()) {
            viewUpdater.notifyError(player.getPlayerId(), "INVALID_TILE",
                    "The selected offer tile is not available.");
            return;
        }

        placeAndCheckTransition(context, player, targetTile);

        skipDisconnectedPlayers(context);
    }

    @Override
    public void onEnter(GameController context) {
        GameModel model = context.getModel();

        List<Player> turnOrder = model.getPlayersInTurnOrder();
        if (!turnOrder.isEmpty()) {
            model.setCurrentPlayer(turnOrder.get(0));
        }

        skipDisconnectedPlayers(context);

        if (model.getPhase() == GamePhase.POSITIONING) {
            Player firstPlayer = model.getCurrentPlayer();
            if (firstPlayer != null) {
                context.getViewUpdater().notifyPlayerTurn(firstPlayer);
            }
        }
    }

    /**
     * FA3: while the current player is disconnected, auto-place their totem on
     * the first available offer tile (no gains for them) so the phase can
     * progress. The loop terminates when a connected player is reached or the
     * phase transitions; each iteration consumes one tile, so it is bounded.
     */
    private void skipDisconnectedPlayers(GameController context) {
        GameModel model = context.getModel();
        while (model.getPhase() == GamePhase.POSITIONING) {
            Player current = model.getCurrentPlayer();
            if (current == null || !context.isDisconnected(current.getPlayerId())) {
                break;
            }
            List<OfferTile> available = model.getAvailableOfferTiles();
            if (available.isEmpty()) break; // safety: nothing to place on
            placeAndCheckTransition(context, current, available.get(0));
        }
    }

    /** Places a totem and transitions to action resolution if it was the last. */
    private void placeAndCheckTransition(GameController context, Player player, OfferTile tile) {
        context.getModel().placeTotem(player, tile);
        if (context.getModel().allTotemsPlaced()) {
            context.transitionTo(GamePhase.ACTION_RESOLUTION);
        }
    }

    @Override
    public void onExit(GameController context) {
        // No cleanup needed
    }

    @Override
    public void onCurrentPlayerDisconnected(GameController context) {
        // FA3: if the player who just dropped is the one to act, skip them.
        skipDisconnectedPlayers(context);
    }

    private OfferTile findOfferTile(GameModel model, String tileId) {
        for (OfferTile tile : model.getAvailableOfferTiles()) {
            if (tile.getId().name().equals(tileId)) {
                return tile;
            }
        }
        return null;
    }
}
