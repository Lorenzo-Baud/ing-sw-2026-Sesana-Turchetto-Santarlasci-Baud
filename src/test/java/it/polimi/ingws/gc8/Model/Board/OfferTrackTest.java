package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfferTrackTest {

    private OfferTrack offerTrack;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        offerTrack = new OfferTrack(4);
        player1 = new Player(1, "Primo", TotemColor.RED);
        player2 = new Player(2, "Secondo", TotemColor.BLUE);
    }

    // --- getTiles ---

    @Test
    void getTiles_nonRestituisceNull() {
        assertNotNull(offerTrack.getTiles());
    }

    @Test
    void getTiles_conQuattroGiocatori_restituisceNumeroCorrettoDiTessere() {
        // verifica che le tessere caricate siano quelle disponibili per 4 giocatori
        assertTrue(offerTrack.getTiles().size() > 0);
    }

    // --- getTileById ---

    @Test
    void getTileById_idEsistente_restituisceTessera() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        assertNotNull(offerTrack.getTileById(id));
    }


    // --- isTileFree ---

    @Test
    void isTileFree_tesseraLibera_restituisceTrue() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        assertTrue(offerTrack.isTileFree(id));
    }

    @Test
    void isTileFree_tesseraOccupata_restituisceFalse() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        assertFalse(offerTrack.isTileFree(id));
    }

    // --- placeTotem ---

    @Test
    void placeTotem_tesseraLibera_piazzaPlayerCorrettamente() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        assertEquals(player1, offerTrack.getTileById(id).getOccupant());
    }

    @Test
    void placeTotem_tesseraGiaOccupata_lanciaIllegalStateException() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        assertThrows(IllegalStateException.class,
                () -> offerTrack.placeTotem(id, player2));
    }

    @Test
    void placeTotem_messaggioEccezioneCorretto() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> offerTrack.placeTotem(id, player2)
        );
        assertEquals("Tile not found or already occupied.", ex.getMessage());
    }

    // --- getAvailableTiles ---

    @Test
    void getAvailableTiles_trackVuoto_restituisceTutteLeTessere() {
        assertEquals(offerTrack.getTiles().size(), offerTrack.getAvailableTiles().size());
    }

    @Test
    void getAvailableTiles_dopoUnPiazzamento_restituisceUnaTesseraMeno() {
        int totaleBefore = offerTrack.getAvailableTiles().size();
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        assertEquals(totaleBefore - 1, offerTrack.getAvailableTiles().size());
    }

    // --- getResolutionOrder ---

    @Test
    void getResolutionOrder_trackVuoto_restituisceListaVuota() {
        assertTrue(offerTrack.getResolutionOrder().isEmpty());
    }

    @Test
    void getResolutionOrder_dopoUnPiazzamento_restituisceUnaTessera() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        assertEquals(1, offerTrack.getResolutionOrder().size());
        assertEquals(player1, offerTrack.getResolutionOrder().get(0).getOccupant());
    }

    // --- clear ---

    @Test
    void clear_svuotaTuttiGliSlot() {
        offerTrack.getTiles().forEach(t ->
                offerTrack.placeTotem(t.getId(), player1));
        offerTrack.clear();
        offerTrack.getTiles().forEach(t -> assertNull(t.getOccupant()));
    }

    // --- containsPlayer ---

    @Test
    void containsPlayer_playerPresente_restituisceTrue() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        assertTrue(offerTrack.containsPlayer(player1));
    }

    @Test
    void containsPlayer_playerAssente_restituisceFalse() {
        assertFalse(offerTrack.containsPlayer(player1));
    }

    // --- isFull ---

    @Test
    void isFull_trackVuoto_restituisceFalse() {
        assertFalse(offerTrack.isFull(4));
    }

    @Test
    void isFull_tuttiIPlayerPiazzati_restituisceTrue() {
        List<OfferTile> tiles = offerTrack.getTiles();
        Player[] players = {
                new Player(1, "P1", TotemColor.RED),
                new Player(2, "P2", TotemColor.BLUE),
                new Player(3, "P3", TotemColor.WHITE),
                new Player(4, "P4", TotemColor.BLACK)
        };
        for (int i = 0; i < 4; i++) {
            offerTrack.placeTotem(tiles.get(i).getId(), players[i]);
        }
        assertTrue(offerTrack.isFull(4));
    }

    @Test
    void isFull_parzialmentePieno_restituisceFalse() {
        OffertileId id = offerTrack.getTiles().get(0).getId();
        offerTrack.placeTotem(id, player1);
        assertFalse(offerTrack.isFull(4));
    }

    @Test
    void testOfferTileGetTile() {
        OfferTrack test = new OfferTrack(2);
        assertNull(test.getTileById(OffertileId.A));
        assertNotNull(test.getTileById(OffertileId.B));
        assertNotNull(test.getTileById(OffertileId.C));
        assertNull(test.getTileById(OffertileId.D));
        assertNotNull(test.getTileById(OffertileId.E));
        assertNotNull(test.getTileById(OffertileId.F));
        assertNull(test.getTileById(OffertileId.G));
    }

}