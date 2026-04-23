package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import it.polimi.ingws.gc8.Controller.actions.*;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

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

        // Validate it's this player's turn in the offer order
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

    @Override
    public void onEnter(GameController context) {
        // Build the offer order and initialize the index
        List<Player> offerOrder = context.getModel().getPlayersInOfferOrder();
        context.setOfferOrder(offerOrder);
        context.setOfferOrderIndex(0);
    }

    @Override
    public void onExit(GameController context) {
        // Clear offer order tracking
        context.setOfferOrder(null);
        context.setOfferOrderIndex(0);
    }

    private void handleTakeCard(GameController context, Player player, TakeCardAction action) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();

        // Find the card in the specified row
        List<Card> availableCards = model.getAvailableCardsFromRow(action.getRow());
        Card targetCard = null;
        for (Card card : availableCards) {
            if (card.getFront().equals(action.getCardName())) {
                targetCard = card;
                break;
            }
        }

        if (targetCard == null) {
            viewUpdater.notifyError(player.getPlayerId(), "INVALID_CARD",
                    "Card not found or not available in the specified row.");
            return;
        }

        // Determine card type and execute
        String cardType;
        if (targetCard instanceof BuildingCard) {
            BuildingCard building = (BuildingCard) targetCard;
            if (!model.canPlayerAffordBuilding(player, building)) {
                viewUpdater.notifyError(player.getPlayerId(), "CANNOT_AFFORD",
                        "Not enough food to buy this building.");
                return;
            }
            model.pickBuildingCard(player, building);
            cardType = "BUILDING";
        } else if (targetCard instanceof CharacterCard) {
            model.pickCharacterCard(player, (CharacterCard) targetCard);
            cardType = "CHARACTER";
        } else {
            viewUpdater.notifyError(player.getPlayerId(), "INVALID_CARD",
                    "Cannot take this type of card.");
            return;
        }

    }

    private void handleTakeFood(GameController context, Player player) {
        GameModel model = context.getModel();
        ViewUpdater viewUpdater = context.getViewUpdater();
        model.takeFood(player);

    }

    private void handleReturnTotem(GameController context, Player player) {
        GameModel model = context.getModel();

        model.returnTotemToTurnOrder(player);

        int nextIndex = context.getOfferOrderIndex() + 1;
        context.setOfferOrderIndex(nextIndex);
        List<Player> offerOrder = context.getOfferOrder();

        if (nextIndex >= offerOrder.size()) {
            context.transitionTo(GamePhase.END_ROUND);
        }
    }

    private Player getCurrentOfferPlayer(GameController context) {
        List<Player> offerOrder = context.getOfferOrder();
        int index = context.getOfferOrderIndex();
        if (offerOrder == null || index < 0 || index >= offerOrder.size()) {
            return null;
        }
        return offerOrder.get(index);
    }
}
