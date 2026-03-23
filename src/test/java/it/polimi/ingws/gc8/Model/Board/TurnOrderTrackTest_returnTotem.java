package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTrackTest_returnTotem {

    private TurnOrderTrack turnOrderTrack;
    private Player player;

    @BeforeEach
    void setUp() {
        turnOrderTrack = new TurnOrderTrack(4);
        player = new Player(1, "TestPlayer", TotemColor.RED);
    }

    @Test
    void returnTotem_restituisceIndiceCorretto() {
        turnOrderTrack.placePlayer(player);
        int index = turnOrderTrack.returnTotem(player);
        assertEquals(0, index);
    }

    @Test
    void returnTotem_slotDiventaNull_dopoRimozione() {
        turnOrderTrack.placePlayer(player);
        turnOrderTrack.returnTotem(player);
        assertNull(turnOrderTrack.getOccupantAtSlot(0));
    }

    @Test
    void returnTotem_playerNonNelTracciato_lanciaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> turnOrderTrack.returnTotem(player));
    }

    @Test
    void returnTotem_messaggioEccezioneCorretto() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> turnOrderTrack.returnTotem(player)
        );
        assertEquals("Giocatore non trovato nel tracciato", ex.getMessage());
    }

    @Test
    void returnTotem_rimuoveSoloIlPlayerCorretto() {
        Player player2 = new Player(2, "Secondo", TotemColor.BLUE);
        turnOrderTrack.placePlayer(player);   // slot 0
        turnOrderTrack.placePlayer(player2);  // slot 1

        turnOrderTrack.returnTotem(player);

        assertNull(turnOrderTrack.getOccupantAtSlot(0));         // rimosso
        assertEquals(player2, turnOrderTrack.getOccupantAtSlot(1)); // intatto
    }
}