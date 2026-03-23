package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTrackTest_getFirstFreeSlotIndex {

    private TurnOrderTrack turnOrderTrack;
    private Player player;

    @BeforeEach
    void setUp() {
        turnOrderTrack = new TurnOrderTrack(4);
        player = new Player(1, "TestPlayer", TotemColor.RED);
    }

    @Test
    void getFirstFreeSlotIndex_trackVuoto_restituisceZero() {
        assertEquals(0, turnOrderTrack.getFirstFreeSlotIndex());
    }

    @Test
    void getFirstFreeSlotIndex_unSlotOccupato_restituisceIlSuccessivo() {
        turnOrderTrack.placePlayer(player);
        assertEquals(1, turnOrderTrack.getFirstFreeSlotIndex());
    }

    @Test
    void getFirstFreeSlotIndex_trackPieno_restituisceMinonoUno() {
        for (int i = 0; i < 4; i++) {
            turnOrderTrack.placePlayer(new Player(i, "Filler" + i, TotemColor.RED));
        }
        assertEquals(-1, turnOrderTrack.getFirstFreeSlotIndex());
    }

    @Test
    void getFirstFreeSlotIndex_restituisceIlPrimoSlotLibero_nonUnoCasuale() {
        // occupa slot 0 e 1, il primo libero deve essere 2
        turnOrderTrack.placePlayer(new Player(1, "Primo", TotemColor.RED));
        turnOrderTrack.placePlayer(new Player(2, "Secondo", TotemColor.BLUE));
        assertEquals(2, turnOrderTrack.getFirstFreeSlotIndex());
    }
}