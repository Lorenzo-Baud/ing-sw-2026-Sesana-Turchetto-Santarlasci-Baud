package it.polimi.ingws.gc8.Controller.actions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;

public class TakeFoodActionTest {
    @Test
    public void takeFoodActionTesting() {
        int playerId = 3;
        TakeFoodAction action = new TakeFoodAction(playerId);
        Assertions.assertEquals(playerId, action.getPlayerId(), "playerId should be the one inserted in the constructor input arguments");
    }
}
