package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTrackTest_placePlayer {

    private TurnOrderTrack turnOrderTrack;
    private Player player;

    @BeforeEach
    void setUp() {
        turnOrderTrack = new TurnOrderTrack(4);
        player = new Player(1, "TestPlayer", TotemColor.RED); // adatta il colore a quelli disponibili nell'enum
    }

    @Test
    void placePlayer_returnsPrimoSlotLibero() {
        int index = turnOrderTrack.placePlayer(player);
        assertEquals(0, index);
    }

    @Test
    void placePlayer_inseriscePlayerNelloSlotCorretto() {
        int index = turnOrderTrack.placePlayer(player);
        assertEquals(player, turnOrderTrack.getOccupantAtSlot(index));
    }

    @Test
    void placePlayer_piuPlayer_occupanoSlotProgressivi() {
        Player player2 = new Player(2, "Secondo", TotemColor.BLUE);
        Player player3 = new Player(3, "Terzo", TotemColor.WHITE);

        int i1 = turnOrderTrack.placePlayer(player);
        int i2 = turnOrderTrack.placePlayer(player2);
        int i3 = turnOrderTrack.placePlayer(player3);

        assertEquals(0, i1);
        assertEquals(1, i2);
        assertEquals(2, i3);
    }

    @Test
    void placePlayer_nessunoSlotLibero_lanciaIllegalStateException() {
        for (int i = 0; i < 4; i++) {
            turnOrderTrack.placePlayer(new Player(i, "Filler" + i, TotemColor.RED));
        }

        assertThrows(IllegalStateException.class,
                () -> turnOrderTrack.placePlayer(player));
    }

    @Test
    void placePlayer_nessunoSlotLibero_messaggioEccezioneCorretto() {
        for (int i = 0; i < 4; i++) {
            turnOrderTrack.placePlayer(new Player(i, "Filler" + i, TotemColor.RED));
        }

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> turnOrderTrack.placePlayer(player)
        );
        assertEquals("Nessuno slot libero disponibile", ex.getMessage());
    }
}