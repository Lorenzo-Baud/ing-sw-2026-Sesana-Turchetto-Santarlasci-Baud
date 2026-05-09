package it.polimi.ingws.gc8.Controller.states;

import it.polimi.ingws.gc8.Controller.GameController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.actions.TakeFoodAction;

import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.MesosGame;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.ViewUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;


class EndRoundStateTest {

    private MesosGame game;
    private GameController controller;
    private ViewUpdaterForTesting viewUpdater;
    private EndRoundState state;
    private Player p1;
    private Player p2;
    private GamePhase lastTransitionedPhase;

    @BeforeEach
    void setUp() {
        game = new MesosGame();
        p1 = new Player(1, "Player1", TotemColor.RED);
        p2 = new Player(2, "Player2", TotemColor.BLUE);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        game.setupGame(2, players);


        viewUpdater = new ViewUpdaterForTesting();
        state = new EndRoundState();
        controller = new GameController(viewUpdater) {
            @Override
            public void transitionTo(GamePhase phase) {
                lastTransitionedPhase = phase;
                super.transitionTo(phase);
            }
        };
    }

    @Test
    void actionRejectsInputTesting() {
        PlayerAction action= new TakeFoodAction(1);

        state.handleAction(controller, action);

        assertEquals("INVALID_ACTION", viewUpdater.getLastError());
    }

    @Test
    void OnExitTesting() {
        assertDoesNotThrow(() -> state.onExit(controller));
    }
}