package it.polimi.ingws.gc8.Controller.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReturnTotemActionTest {
    @Test
    public void testConstructor() {
        int playerId = 1;
        ReturnTotemAction action = new ReturnTotemAction(playerId);
        assertEquals(action.getPlayerId(), playerId,"player ID should be the one inserted in the constructor input arguments");
    }
}
