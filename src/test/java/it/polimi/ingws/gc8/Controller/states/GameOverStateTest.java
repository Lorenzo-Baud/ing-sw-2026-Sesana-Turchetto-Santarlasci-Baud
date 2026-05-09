package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.actions.TakeFoodAction;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameOverStateTest {

    private GameController controller;
    private ViewUpdaterForTesting viewUpdater;
    private GameOverState state;



    @BeforeEach
    void setUp() {
        viewUpdater = new ViewUpdaterForTesting();
        controller = new GameController(viewUpdater);
        state = new GameOverState();
    }

    @Test
    void actionRejectsInputTesting() {
        int testPlayerId = 1;
        PlayerAction dummyAction = new TakeFoodAction(testPlayerId);

        state.handleAction(controller, dummyAction);

        assertEquals("GAME_OVER", viewUpdater.getLastError());
    }


    @Test
    void onEnterTesting() {
        assertDoesNotThrow(() -> state.onEnter(controller));
    }

    @Test
    void onExitTesting() {
        assertDoesNotThrow(() -> state.onExit(controller));
    }
}