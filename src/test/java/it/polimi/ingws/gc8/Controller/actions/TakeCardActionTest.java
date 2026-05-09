package it.polimi.ingws.gc8.Controller.actions;

import it.polimi.ingws.gc8.Model.Enum.CardRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TakeCardActionTest {
    @Test
    public void TakeCardActionTestnig()
    {
        int playerId = 1;
        CardRow row  = CardRow.UPPER;
        int cardIndex = 2;

        TakeCardAction action = new TakeCardAction(playerId, cardIndex, row);
        assertEquals( cardIndex,action.getCardIndex(), "cardIndex should be the one inserted in the constructor input arguments");
        assertEquals(row, action.getRow(), "row should be the one inserted in the constructor input arguments");
        assertEquals( playerId, action.getPlayerId(), "playerId should be the one inserted in the constructor input arguments");

    }


}
