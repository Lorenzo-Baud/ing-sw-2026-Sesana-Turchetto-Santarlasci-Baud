package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.*;
import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Model.Board.Board;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Board.OfferTrack;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Shaman;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.MesosGame;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewUpdaterForTesting extends ViewUpdater {
    private String lastError;
    private List<GameEventListener> listeners;


    public void viewUpdaterForTesting() {
        this.listeners = new CopyOnWriteArrayList<>();
    }
    public String getLastError() {
        return lastError;
    }

    public void notifyError(int playerId, String error, String errorDetail ) {
        lastError = error;

    }
}

public class ActionResolutionStateTest {



    MesosGame game;
    Player p1;
    Player p2;
    GameController controller;
    ViewUpdaterForTesting  viewUpdater;
    ActionResolutionState state;



    /*using  @BeforeEach in order to execute the setup before every test
    *   the setup phase initializes the model, the controller, the board rows, brings the fsm
    * into action resolution state places totems and triggers the onEnter method of the controller */
    @BeforeEach
    void setUp() {
        p1 = new Player(1, "Player1", TotemColor.RED);
        p2 = new Player(2, "Player2", TotemColor.BLUE);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);

        p1.addFood(10);
        p2.addFood(10);

        viewUpdater = new ViewUpdaterForTesting();
        controller = new GameController(viewUpdater);
        controller.startGame(players);
        // Replace the controller's freshly-built rows with a deterministic set
        // so tests can pick known cards. The model owned by the controller is
        // the authoritative one (the local `game` field below is only kept
        // for backward compatibility with assertions; the state under test
        // reads exclusively from controller.getModel()).
        game = (MesosGame) controller.getModel();
        Board board = game.getBoard();
        board.setUpperRow(new ArrayList<>(List.of(
                new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 2, 2),
                new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 2, 2),
                new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 2, 2))));
        board.setLowerRow(new ArrayList<>(List.of(
                new BuildingCard("Building1", 3, 2, Era.ERA_1, "building_front.png", "building_back.png", null),
                new BuildingCard("Building1", 3, 2, Era.ERA_1, "building_front.png", "building_back.png", null))));

        // Place totems on the controller's model so that the state under
        // test can resolve each player to their offer tile.
        List<OfferTile> availableTiles = game.getAvailableOfferTiles();
        game.placeTotem(p1, availableTiles.get(0));
        // Re-read available tiles: the first call exposes all unoccupied
        // ones, after p1's placement the list is smaller by one.
        game.placeTotem(p2, game.getAvailableOfferTiles().get(0));

        state = new ActionResolutionState();
        state.onEnter(controller);

        controller.setOfferOrder(players);
        controller.setOfferOrderIndex(1);
    }

    @Test
    void invalidPlayerIdTesting() {
        PlayerAction action = new TakeFoodAction(32); // Unknown ID
        state.handleAction(controller, action);
        assertEquals("INVALID_PLAYER", viewUpdater.getLastError());
    }

    @Test
    void NotPlayersTurnTesting() {
        PlayerAction action = new TakeFoodAction(1);
        state.handleAction(controller, action);
        assertEquals("NOT_YOUR_TURN", viewUpdater.getLastError());
    }

    @Test
    void invalidActionTesting() {
        PlayerAction action = new PlaceTotemAction(2, "A");
        state.handleAction(controller, action);

        assertEquals("INVALID_ACTION", viewUpdater.getLastError());
    }

    @Test
    void takeInvalidCardTesting() {

        TakeCardAction action = new TakeCardAction(2, 8, CardRow.UPPER);
        state.handleAction(controller, action);

        assertEquals("INVALID_CARD", viewUpdater.getLastError());
    }

    @Test
    void takeBuildingTesting() {
        p1.addFood(5);
        int initialFood =  p1.getFood();
        TakeCardAction action = new TakeCardAction(1,  0, CardRow.LOWER);
        state.handleAction(controller, action);
        assertTrue( p1.getFood() <= initialFood );
    }

    @Test
    void returnAllowedWhenOnlyBuildingsAndNoAffordOnBothRowTile() {
        // Repro of the live bug: totem on tile E (1 upper + 1 lower pick), both
        // rows contain ONLY buildings the player cannot afford, NO Character
        // anywhere. The player must be allowed to end the turn (no block).
        Board board = game.getBoard();
        board.setUpperRow(new ArrayList<>(List.of(
                new BuildingCard("U", 10, 2, Era.ERA_1, "f.png", "b.png", null),
                new BuildingCard("U", 9, 2, Era.ERA_1, "f.png", "b.png", null))));
        board.setLowerRow(new ArrayList<>(List.of(
                new BuildingCard("L", 5, 2, Era.ERA_1, "f.png", "b.png", null),
                new BuildingCard("L", 7, 2, Era.ERA_1, "f.png", "b.png", null))));

        // Force p1 onto tile E deterministically.
        OfferTrack track = board.getOfferTrack();
        for (OfferTile t : track.getTiles()) {
            if (p1.equals(t.getOccupant()) || p2.equals(t.getOccupant())) t.removeTotem();
        }
        track.placeTotem(OffertileId.E, p1);

        // Make it p1's turn in the offer order.
        controller.setOfferOrder(List.of(p1, p2));
        controller.setOfferOrderIndex(0);

        state.handleAction(controller, new ReturnTotemAction(1));

        assertNull("Return must be accepted (no character to pick on tile E)",
                viewUpdater.getLastError());
    }

    @Test
    void returnAllowedWhenCharacterOnlyInUnpickableRow() {
        // Tile C is upper-only (lowerRowPicks=0). A Character sits in the LOWER
        // row (not pickable from C) and the upper row has only buildings. The
        // player must be allowed to end the turn.
        Board board = game.getBoard();
        board.setUpperRow(new ArrayList<>(List.of(
                new BuildingCard("U", 10, 2, Era.ERA_1, "f.png", "b.png", null))));
        board.setLowerRow(new ArrayList<>(List.of(
                new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 2, 2))));

        OfferTrack track = board.getOfferTrack();
        for (OfferTile t : track.getTiles()) {
            if (p1.equals(t.getOccupant()) || p2.equals(t.getOccupant())) t.removeTotem();
        }
        track.placeTotem(OffertileId.C, p1);

        controller.setOfferOrder(List.of(p1, p2));
        controller.setOfferOrderIndex(0);

        state.handleAction(controller, new ReturnTotemAction(1));

        assertNull("Return must be accepted: the character is in a row tile C cannot pick from",
                viewUpdater.getLastError());
    }

    @Test
    void takeFoodRejectedOnNonFoodTileTesting() {
        // In a 2-player game tile A (the only foodReward>0 tile) is excluded
        // by its minPlayers constraint, so p2's tile cannot grant food. The
        // server must reject the action authoritatively.
        int initialFood = p2.getFood();
        TakeFoodAction action = new TakeFoodAction(2);

        state.handleAction(controller, action);

        assertEquals("FOOD_NOT_ALLOWED", viewUpdater.getLastError());
        assertEquals(initialFood, p2.getFood());
    }
}

