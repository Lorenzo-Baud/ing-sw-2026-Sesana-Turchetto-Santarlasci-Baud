package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import it.polimi.ingws.gc8.Controller.actions.PlaceTotemAction;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

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

        // Validate it's this player's turn
        Player currentPlayer = model.getCurrentPlayer();
        if (currentPlayer == null || currentPlayer.getPlayerId() != player.getPlayerId()) {
            viewUpdater.notifyError(player.getPlayerId(), "NOT_YOUR_TURN",
                    "It is not your turn to place a totem.");
            return;
        }

        // Find the requested offer tile
        OfferTile targetTile = findOfferTile(model, placeAction.getOfferTileId());
        if (targetTile == null || !targetTile.isFree()) {
            viewUpdater.notifyError(player.getPlayerId(), "INVALID_TILE",
                    "The selected offer tile is not available.");
            return;
        }

        // Execute the action on the model
        model.placeTotem(player, targetTile);



        // Check if all totems are placed → transition
        if (model.allTotemsPlaced()) {
            context.transitionTo(GamePhase.ACTION_RESOLUTION);

        }
    }

    @Override
    public void onEnter(GameController context) {
        // Notify the first player in turn order that it's their turn
        Player firstPlayer = context.getModel().getCurrentPlayer();
        if (firstPlayer != null) {
            context.getViewUpdater().notifyPlayerTurn(firstPlayer);
        }
    }

    @Override
    public void onExit(GameController context) {
        // No cleanup needed
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
