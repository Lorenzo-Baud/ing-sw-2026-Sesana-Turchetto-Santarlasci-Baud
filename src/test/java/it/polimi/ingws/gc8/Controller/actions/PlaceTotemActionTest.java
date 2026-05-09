package it.polimi.ingws.gc8.Controller.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlaceTotemActionTest {

     @Test
     void testConstructorAndGetters() {
         int expectedPlayerId = 1;
         String expectedTileId = "B";

         PlaceTotemAction action = new PlaceTotemAction(expectedPlayerId, expectedTileId);

         assertEquals(expectedPlayerId, action.getPlayerId(), "player ID should be the one inserted in the constructor input arguments");
         assertEquals(expectedTileId, action.getOfferTileId(), "offer tile ID should be the one inserted in the constructor input arguments");
     }
}
