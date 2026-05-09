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
        game = new MesosGame();
        p1 = new Player(1, "Player1", TotemColor.RED);
        p2 = new Player(2, "Player2", TotemColor.BLUE);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        game.setupGame(2, players);


        p1.addFood(10);
        p2.addFood(10);

        Board board = game.getBoard();
        board.setUpperRow(new ArrayList<>(List.of(new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 2, 2),
                new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 2, 2), new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 2, 2))));
        board.setLowerRow(new ArrayList<>(List.of(new BuildingCard("Building1", 3, 2, Era.ERA_1, "building_front.png", "building_back.png", null),
                new BuildingCard("Building1", 3, 2, Era.ERA_1, "building_front.png", "building_back.png", null))));
        List<OfferTile> availableTiles = game.getAvailableOfferTiles();
        OfferTile Player1Tile = availableTiles.get(0);
        OfferTile Player2Tile = availableTiles.get(1);

        viewUpdater = new ViewUpdaterForTesting();
        controller = new GameController(viewUpdater);
        controller.startGame(players);
        state = new ActionResolutionState();

        game.placeTotem(p1, Player1Tile);
        game.placeTotem(p2, Player2Tile);

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
    void takeFoodTesting() {
        int initialFood = p2.getFood();
        TakeFoodAction action = new TakeFoodAction(2);

        state.handleAction(controller, action);
        assertTrue(p2.getFood() > initialFood, "Player's food should have increased.");
    }
}

