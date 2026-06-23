package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.*;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Network.ViewUpdater;


import java.util.List;

/**
 * State active during {@link GamePhase#ACTION_RESOLUTION}.
 * Players resolve their offer tiles in order.
 * Each player picks cards from the lower/upper rows, optionally takes food,
 * and then returns their totem to the turn order track.
 *
 * Accepts {@link TakeCardAction}, {@link TakeFoodAction}, and {@link ReturnTotemAction}.
 *
 * The offer order and current acting player index are tracked in
 * {@link GameController#getOfferOrder()} and {@link GameController#getOfferOrderIndex()}.
 */
public class ActionResolutionState implements GameState {

    /**
     * Handles a player action during the action resolution phase.
     * @param context the GameController providing access to model and view updater
     * @param action  the action requested by a player
     */
    @Override
    public void handleAction(GameController context, PlayerAction action) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();
        Player player = context.getPlayerById(action.getPlayerId());

        if (player == null) {
            viewUpdater.notifyError(action.getPlayerId(), "INVALID_PLAYER",
                    "Player not found.");
            return;
        }

        Player currentPlayer = getCurrentOfferPlayer(context);
        if (currentPlayer == null || currentPlayer.getPlayerId() != player.getPlayerId()) {
            viewUpdater.notifyError(player.getPlayerId(), "NOT_YOUR_TURN",
                    "It is not your turn to act.");
            return;
        }

        if (action instanceof TakeCardAction) {
            handleTakeCard(context, player, (TakeCardAction) action);
        } else if (action instanceof TakeFoodAction) {
            handleTakeFood(context, player);
        } else if (action instanceof ReturnTotemAction) {
            handleReturnTotem(context, player);
        } else {
            viewUpdater.notifyError(player.getPlayerId(), "INVALID_ACTION",
                    "Action not allowed during action resolution phase.");
        }
    }

    /**
     * Initializes the offer order and sets the first player as the current player.
     * Resets the per-turn pick counters in the model.
     * @param context the GameController providing access to model and view updater
     */
    @Override
    public void onEnter(GameController context) {
        // Build the offer order and initialize the index
        List<Player> offerOrder = context.getModel().getPlayersInOfferOrder();
        context.setOfferOrder(offerOrder);
        context.setOfferOrderIndex(0);
        context.getModel().resetCurrentTurnPicks();

        if (!offerOrder.isEmpty()) {
            Player firstPlayer = offerOrder.get(0);
            context.getModel().setCurrentPlayer(firstPlayer); // Questo scatena notifyUpdate!
        }

        // FA3: the first offer player(s) may be disconnected; skip them.
        skipDisconnectedOfferPlayers(context);
    }

    /**
     *
     * @param context the GameController
     */
    @Override
    public void onExit(GameController context) {
        context.setOfferOrder(null);
        context.setOfferOrderIndex(0);
    }

    /**
     * Handles the disconnection of the current player
     * @param context the GameController
     */
    @Override
    public void onCurrentPlayerDisconnected(GameController context) {
        skipDisconnectedOfferPlayers(context);
    }

    /**
     * Handles the take card action
     * @param context game controller
     * @param player player performing the action
     * @param action action performed
     */
    private void handleTakeCard(GameController context, Player player, TakeCardAction action) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();

        OfferTile tile = model.getOfferTile(player);
        if (tile == null) {
            viewUpdater.notifyError(player.getPlayerId(), "NO_OFFER_TILE",
                    "You are not standing on any offer tile.");
            return;
        }
        CardRow row = action.getRow();
        if (row == CardRow.UPPER) {
            if (model.getCurrentTurnUpperTaken() >= tile.getUpperRowPicks()) {
                viewUpdater.notifyError(player.getPlayerId(), "PICK_LIMIT_EXCEEDED",
                        "No more upper-row picks allowed on offer tile "
                                + tile.getId() + ".");
                return;
            }
        } else if (row == CardRow.LOWER) {
            if (model.getCurrentTurnLowerTaken() >= tile.getLowerRowPicks()) {
                viewUpdater.notifyError(player.getPlayerId(), "PICK_LIMIT_EXCEEDED",
                        "No more lower-row picks allowed on offer tile "
                                + tile.getId() + ".");
                return;
            }
        }

        List<Card> rowCards = (row == CardRow.UPPER)
                ? model.getBoard().getUpperRow()
                : model.getBoard().getLowerRow();
        int idx = action.getCardIndex();
        if (idx < 0 || idx >= rowCards.size()) {
            viewUpdater.notifyError(player.getPlayerId(), "INVALID_CARD",
                    "Card index out of range for the specified row.");
            return;
        }
        Card targetCard = rowCards.get(idx);

        if (targetCard instanceof BuildingCard building) {
            if (!model.canPlayerAffordBuilding(player, building)) {
                viewUpdater.notifyError(player.getPlayerId(), "CANNOT_AFFORD",
                        "Not enough food to buy this building.");
                return;
            }
            bumpPickCounter(model, row);
            model.takeBuildingCard(player, building);
        } else if (targetCard instanceof CharacterCard character) {
            bumpPickCounter(model, row);
            model.takeCharacterCard(player, character);
        } else {
            viewUpdater.notifyError(player.getPlayerId(), "INVALID_CARD",
                    "Cannot take this type of card.");
            return;
        }
    }

    /** Bumps the authoritative per-turn pick counter for the given row. */
    private void bumpPickCounter(GameModel model, CardRow row) {
        if (row == CardRow.UPPER) {
            model.setCurrentTurnUpperTaken(model.getCurrentTurnUpperTaken() + 1);
        } else if (row == CardRow.LOWER) {
            model.setCurrentTurnLowerTaken(model.getCurrentTurnLowerTaken() + 1);
        }
    }

    /**
     * Handles take food action
     * @param context game controller
     * @param player player performing the action
     */
    private void handleTakeFood(GameController context, Player player) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();

        OfferTile tile = model.getOfferTile(player);
        if (tile == null) {
            viewUpdater.notifyError(player.getPlayerId(), "NO_OFFER_TILE",
                    "You are not standing on any offer tile.");
            return;
        }
        if (tile.getFoodReward() <= 0) {
            viewUpdater.notifyError(player.getPlayerId(), "FOOD_NOT_ALLOWED",
                    "Offer tile " + tile.getId() + " does not grant food.");
            return;
        }
        if (model.isCurrentTurnFoodTaken()) {
            viewUpdater.notifyError(player.getPlayerId(), "FOOD_ALREADY_TAKEN",
                    "You have already taken food this turn.");
            return;
        }

        model.setCurrentTurnFoodTaken(true);
        model.takeFood(player);
    }

    /**
     * Handles totem return to the turn order track, advancing to the next offer player or ending the round.
     * @param context game controller
     * @param player player performing the action
     */
    private void handleReturnTotem(GameController context, Player player) {
        GameModel model = context.getModel();

        if (hasMandatoryCharacterPick(model, player)) {
            context.getViewUpdater().notifyError(player.getPlayerId(), "MUST_PICK_CHARACTER",
                    "You must take the available Character card(s) before returning your totem.");
            return;
        }

        advanceToNextOfferPlayer(context, player);

        skipDisconnectedOfferPlayers(context);
    }

    /**
     * Whether the player is still obliged to pick a Character: a row from which
     * picks remain (per the offer tile quota) still contains a Character card.
     */
    private boolean hasMandatoryCharacterPick(GameModel model, Player player) {
        OfferTile tile = model.getOfferTile(player);
        if (tile == null) {
            return false;
        }
        if (model.getCurrentTurnUpperTaken() < tile.getUpperRowPicks()
                && rowHasCharacter(model.getBoard().getUpperRow())) {
            return true;
        }
        return model.getCurrentTurnLowerTaken() < tile.getLowerRowPicks()
                && rowHasCharacter(model.getBoard().getLowerRow());
    }

    private boolean rowHasCharacter(List<Card> row) {
        return row != null && row.stream().anyMatch(c -> c instanceof CharacterCard);
    }

    /**
     * Returns the given player's totem and advances the offer order to the next
     * player, transitioning to {@link GamePhase#END_ROUND} when the order is
     * exhausted.
     */
    private void advanceToNextOfferPlayer(GameController context, Player player) {
        GameModel model = context.getModel();

        model.resetCurrentTurnPicks();

        model.returnTotemToTurnOrder(player);

        int nextIndex = context.getOfferOrderIndex() + 1;
        context.setOfferOrderIndex(nextIndex);
        List<Player> offerOrder = context.getOfferOrder();

        if (nextIndex >= offerOrder.size()) {
            context.transitionTo(GamePhase.END_ROUND);
        } else {
            Player nextPlayer = offerOrder.get(nextIndex);
            context.getModel().setCurrentPlayer(nextPlayer);
        }
    }

    /**
     * FA3: while the current offer player is disconnected, auto-return their
     * totem with no picks (no gains) and advance, so an offline player cannot
     * stall the action-resolution phase. Bounded by the offer order size.
     */
    private void skipDisconnectedOfferPlayers(GameController context) {
        while (context.getModel().getPhase() == GamePhase.ACTION_RESOLUTION) {
            Player current = getCurrentOfferPlayer(context);
            if (current == null || !context.isDisconnected(current.getPlayerId())) {
                break;
            }
            advanceToNextOfferPlayer(context, current);
        }
    }

    /**
     * Gets the current offer player from the offer order and index.
     * @param context game controller
     * @return player index
     */
    private Player getCurrentOfferPlayer(GameController context) {
        List<Player> offerOrder = context.getOfferOrder();
        int index = context.getOfferOrderIndex();
        if (offerOrder == null || index < 0 || index >= offerOrder.size()) {
            return null;
        }
        return offerOrder.get(index);
    }
}
