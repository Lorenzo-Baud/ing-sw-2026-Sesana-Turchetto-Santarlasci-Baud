package it.polimi.ingws.gc8.Persistence;

import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.MesosGame;
import it.polimi.ingws.gc8.Model.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FA2 — verifies the whole model graph is serializable end-to-end and that a
 * snapshot survives a save/load round-trip with its state intact.
 */
public class PersistenceManagerTest {

    private MesosGame newStartedGame() {
        MesosGame game = new MesosGame();
        List<Player> players = List.of(
                new Player(1, "Alice", TotemColor.RED),
                new Player(2, "Bob", TotemColor.BLUE),
                new Player(3, "Carol", TotemColor.YELLOW));
        game.setupGame(players.size(), players);
        // Place a totem so the graph holds OfferTile -> Player references too.
        Player first = game.getPlayersInTurnOrder().get(0);
        OfferTile tile = game.getAvailableOfferTiles().get(0);
        game.placeTotem(first, tile);
        return game;
    }

    private MatchSnapshot snapshotOf(GameModel model) {
        Map<String, Integer> nicks = new LinkedHashMap<>();
        nicks.put("Alice", 1);
        nicks.put("Bob", 2);
        nicks.put("Carol", 3);
        return new MatchSnapshot("partita-test", 3, "Alice", 1, 4,
                nicks, model, System.currentTimeMillis());
    }

    @Test
    public void wholeModelGraphIsSerializable() throws Exception {
        MatchSnapshot snap = snapshotOf(newStartedGame());
        // Throws NotSerializableException if any reachable field is not serializable.
        byte[] bytes = PersistenceManager.toBytes(snap);
        assertTrue(bytes.length > 0);
    }

    @Test
    public void saveLoadRoundTripPreservesState(@TempDir Path dir) {
        PersistenceManager pm = new PersistenceManager(dir);
        MesosGame original = newStartedGame();
        MatchSnapshot snap = snapshotOf(original);

        assertDoesNotThrow(() -> pm.save(snap));

        List<MatchSnapshot> loaded = pm.loadAll();
        assertEquals(1, loaded.size());

        MatchSnapshot restored = loaded.get(0);
        assertEquals("partita-test", restored.matchId());
        assertEquals(3, restored.expectedPlayers());
        assertEquals("Alice", restored.hostNickname());
        assertEquals(4, restored.nextPlayerId());
        assertEquals(Integer.valueOf(2), restored.nickToPlayerId().get("Bob"));

        GameModel rModel = restored.model();
        assertEquals(original.getPhase(), rModel.getPhase());
        assertEquals(original.getCurrentRound(), rModel.getCurrentRound());
        assertEquals(3, rModel.getPlayers().size());
        assertEquals("Alice", rModel.getPlayers().get(0).getNickname());
        // The placed totem must still be reflected after deserialization.
        assertEquals(GamePhase.POSITIONING, rModel.getPhase());
        assertTrue(rModel.getAvailableOfferTiles().size()
                < original.getBoard().getOfferTrack().getTiles().size());
    }

    @Test
    public void deleteRemovesSnapshot(@TempDir Path dir) {
        PersistenceManager pm = new PersistenceManager(dir);
        MatchSnapshot snap = snapshotOf(newStartedGame());
        assertDoesNotThrow(() -> pm.save(snap));
        assertEquals(1, pm.loadAll().size());

        pm.delete(snap.matchId());
        assertEquals(0, pm.loadAll().size());
    }
}
