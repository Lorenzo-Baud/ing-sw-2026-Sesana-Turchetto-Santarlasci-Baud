package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TurnOrderTrackTest {

    @Test
    void testConstructorException() throws IOException {

        Path originalPath = Paths.get("src/main/resources/offer_tiles.json");
        Path tempPath = Paths.get("src/main/resources/offer_tiles_HIDDEN.json");

        try {
            Files.move(originalPath, tempPath);

            assertThrows(RuntimeException.class, () -> {
                new TurnOrderTrack(1);
            });

        } finally {
            if (Files.exists(tempPath)) {
                Files.move(tempPath, originalPath);
            }
        }

    }

    @Test
    void testTrackFullException() {
        TurnOrderTrack test = new TurnOrderTrack(4);
        for(int i = 0; i < 4; i++) {
            test.placePlayer(new Player(0, "", TotemColor.RED));
        }
        assertThrows(IllegalStateException.class, () -> {
            test.placePlayer(new Player(0, "", TotemColor.RED));
        });
    }

    @Test
    void testVariousMethods() {

        TurnOrderTrack test = new TurnOrderTrack(4);
        Player p1 = new Player(0, "", TotemColor.RED);
        Player p2 = new Player(1, "", TotemColor.RED);
        test.placePlayer(p1);
        test.placePlayer(p2);

        // Test getOccupantAtSlot
        assertEquals(p1, test.getOccupantAtSlot(0));
        assertEquals(p2, test.getOccupantAtSlot(1));

        // Test getLastSlotPrestigePenalty
        assertEquals(-2, test.getLastSlotPrestigePenalty());

        // Test getFoodBonusesBySlot
        /*
            NOTE: not all the configuration are tested, this
            test only checks that the bonuses are correctly
            loaded from the file and returned by the method.
         */
        List<Integer> expectedBonuses = new ArrayList<>(List.of(2, 1, 0, 0));
        int j = 0;
        for(Integer i : test.getFoodBonusesBySlot()) {
            assertEquals(expectedBonuses.get(j), i);
            j++;
        }

        // Test getPlayerCount
        assertEquals(4, test.getPlayerCount());

        // Test DTO
        List<PlayerDTO> testDTO = test.toDto();
        for (int i = 0; i < testDTO.size(); i++) {
            if(testDTO.get(i) != null) {
                assertEquals(testDTO.get(i).playerId(), test.getOccupantAtSlot(i).getPlayerId());
            }
        }

    }



    @Test
    void testVariousExceptions() {

        TurnOrderTrack test = new TurnOrderTrack(4);
        for(int i = 0; i < 4; i++) {
            test.placePlayer(new Player(i, "", TotemColor.RED));
        }

        // Player not found exception
        assertThrows(IllegalArgumentException.class, () -> {
            test.returnTotem(new Player(4, "", TotemColor.RED));
        });

    }

}
