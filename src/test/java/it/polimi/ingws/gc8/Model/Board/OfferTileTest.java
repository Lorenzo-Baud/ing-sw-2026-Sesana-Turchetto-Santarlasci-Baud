package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OfferTileTest {

    private OfferTile tileB;  // minPlayers null, sempre disponibile
    private OfferTile tileA;  // minPlayers 5
    private OfferTile tileG;  // minPlayers 4
    private Player player;

    @BeforeEach
    void setUp() {
        tileB = new OfferTile(OffertileId.B, null, 0, 1, 0);
        tileA = new OfferTile(OffertileId.A, 5, 3, 0, 0);
        tileG = new OfferTile(OffertileId.G, 4, 0, 1, 2);
        player = new Player(1, "TestPlayer", TotemColor.RED);
    }

    // --- isFree ---

    @Test
    void isFree_tesseraAppenaCreata_restituisceTrue() {
        assertTrue(tileB.isFree());
    }

    @Test
    void isFree_dopoPlaceTotem_restituisceFalse() {
        tileB.placeTotem(player);
        assertFalse(tileB.isFree());
    }

    // --- placeTotem ---

    @Test
    void placeTotem_impostaOccupanteCorrettamente() {
        tileB.placeTotem(player);
        assertEquals(player, tileB.getOccupant());
    }

    // --- removeTotem ---

    @Test
    void removeTotem_restituisceIlPlayerRimosso() {
        tileB.placeTotem(player);
        Player removed = tileB.removeTotem();
        assertEquals(player, removed);
    }

    @Test
    void removeTotem_svuotaLaTessera() {
        tileB.placeTotem(player);
        tileB.removeTotem();
        assertNull(tileB.getOccupant());
        assertTrue(tileB.isFree());
    }

    @Test
    void removeTotem_tesseraVuota_restituisceNull() {
        assertNull(tileB.removeTotem());
    }

    // --- setOccupant ---

    @Test
    void setOccupant_impostaOccupante() {
        tileB.setOccupant(player);
        assertEquals(player, tileB.getOccupant());
    }

    @Test
    void setOccupant_null_svuotaLaTessera() {
        tileB.placeTotem(player);
        tileB.setOccupant(null);
        assertNull(tileB.getOccupant());
    }

    // --- isAvailableFor ---

    @Test
    void isAvailableFor_minPlayersNull_sempreDisponibile() {
        // tessera B: minPlayers null → disponibile per qualsiasi numero
        assertTrue(tileB.isAvailableFor(2));
        assertTrue(tileB.isAvailableFor(5));
    }

    @Test
    void isAvailableFor_tileA_soloConCinqueOPiuGiocatori() {
        // tessera A: minPlayers 5
        assertTrue(tileA.isAvailableFor(5));
        assertFalse(tileA.isAvailableFor(4));
        assertFalse(tileA.isAvailableFor(3));
    }

    @Test
    void isAvailableFor_tileG_soloConQuattroOPiuGiocatori() {
        // tessera G: minPlayers 4
        assertTrue(tileG.isAvailableFor(4));
        assertTrue(tileG.isAvailableFor(5));
        assertFalse(tileG.isAvailableFor(3));
    }

    // --- getters con valori reali dal JSON ---

    @Test
    void getters_tileA_valoriCorretti() {
        assertEquals(OffertileId.A, tileA.getId());
        assertEquals(5, tileA.getMinPlayers());
        assertEquals(3, tileA.getFoodReward());
        assertEquals(0, tileA.getLowerRowPicks());
        assertEquals(0, tileA.getUpperRowPicks());
    }

    @Test
    void getters_tileB_valoriCorretti() {
        assertEquals(OffertileId.B, tileB.getId());
        assertNull(tileB.getMinPlayers());
        assertEquals(0, tileB.getFoodReward());
        assertEquals(1, tileB.getLowerRowPicks());
        assertEquals(0, tileB.getUpperRowPicks());
    }

    @Test
    void getters_tileG_valoriCorretti() {
        assertEquals(OffertileId.G, tileG.getId());
        assertEquals(4, tileG.getMinPlayers());
        assertEquals(0, tileG.getFoodReward());
        assertEquals(1, tileG.getLowerRowPicks());
        assertEquals(2, tileG.getUpperRowPicks());
    }

    @Test
    void testToDTO() {

        OfferTile tile = new OfferTile(OffertileId.A, 0, 0, 0, 0);

        OfferTileDTO dto = tile.toDTO();

        assertNotNull(dto);
        assertEquals(-1, dto.playerIndexOccupant());
    }

}