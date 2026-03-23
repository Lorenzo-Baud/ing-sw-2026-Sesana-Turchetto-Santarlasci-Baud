package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTrackTest_getters {

    private TurnOrderTrack turnOrderTrack;
    private Player player;

    @BeforeEach
    void setUp() {
        turnOrderTrack = new TurnOrderTrack(4);
        player = new Player(1, "TestPlayer", TotemColor.RED);
    }

    // --- getOccupantAtSlot ---

    @Test
    void getOccupantAtSlot_slotVuoto_restituisceNull() {
        assertNull(turnOrderTrack.getOccupantAtSlot(0));
    }

    @Test
    void getOccupantAtSlot_slotOccupato_restituiscePlayerCorretto() {
        turnOrderTrack.placePlayer(player);
        assertEquals(player, turnOrderTrack.getOccupantAtSlot(0));
    }

    // --- getCurrentOrder ---

    @Test
    void getCurrentOrder_trackVuoto_tuttiNull() {
        List<Player> order = turnOrderTrack.getCurrentOrder();
        assertEquals(4, order.size());
        order.forEach(p -> assertNull(p));
    }

    @Test
    void getCurrentOrder_rifletteLOrdineCorrente() {
        Player player2 = new Player(2, "Secondo", TotemColor.BLUE);
        turnOrderTrack.placePlayer(player);
        turnOrderTrack.placePlayer(player2);

        List<Player> order = turnOrderTrack.getCurrentOrder();
        assertEquals(player, order.get(0));
        assertEquals(player2, order.get(1));
        assertNull(order.get(2));
        assertNull(order.get(3));
    }

    @Test
    void getCurrentOrder_listaImmutabile() {
        assertThrows(UnsupportedOperationException.class,
                () -> turnOrderTrack.getCurrentOrder().add(player));
    }

    // --- isLastSlot ---

    @Test
    void isLastSlot_ultimoSlot_restituisceTrue() {
        assertTrue(turnOrderTrack.isLastSlot(3)); // playerCount 4 → ultimo slot è 3
    }

    @Test
    void isLastSlot_slotIntermedio_restituisceFalse() {
        assertFalse(turnOrderTrack.isLastSlot(0));
        assertFalse(turnOrderTrack.isLastSlot(1));
        assertFalse(turnOrderTrack.isLastSlot(2));
    }

    // --- getPlayerCount ---

    @Test
    void getPlayerCount_restituisceValoreCorretto() {
        assertEquals(4, turnOrderTrack.getPlayerCount());
    }

    // --- getFoodBonusForSlot e getFoodBonusesBySlot ---

    @Test
    void getFoodBonusForSlot_coerente_conFoodBonusesBySlot() {
        List<Integer> bonuses = turnOrderTrack.getFoodBonusesBySlot();
        for (int i = 0; i < bonuses.size(); i++) {
            assertEquals(bonuses.get(i), turnOrderTrack.getFoodBonusForSlot(i));
        }
    }

    // --- getLastSlotFoodPenalty e getLastSlotPrestigePenalty ---

    @Test
    void getLastSlotFoodPenalty_valoreCorretto() {
        assertEquals(1, turnOrderTrack.getLastSlotFoodPenalty());
    }

    @Test
    void getLastSlotPrestigePenalty_valoreCorretto() {
        assertEquals(-2, turnOrderTrack.getLastSlotPrestigePenalty());
    }
}