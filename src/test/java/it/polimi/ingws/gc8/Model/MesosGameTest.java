package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MesosGameTest {

    /**
     * This test class simulates an entire mesos game
     * to ensure all the classes involved work properly
     */
    @Test
    public void testGame() {

        MesosGame game = new MesosGame();

        List<Player> players = new ArrayList<>();
        players.add(new Player(1, "Player1", TotemColor.RED));
        players.add(new Player(2, "Player2", TotemColor.RED));
        players.add(new Player(3, "Player3", TotemColor.RED));

        // Constructor test
        assertEquals(0, game.getPlayers().size());
        assertEquals(0, game.getCurrentRound());
        assertEquals(Era.ERA_1, game.getCurrentEra());

        // Game setup
        game.setupGame(3, players);

        // Tests initial randomization
        int[] foodByPosition = {2, 3, 3, 4, 4};
        for(int i = 0; i < game.getPlayersInTurnOrder().size(); i++) {
            assertEquals(game.getPlayersInTurnOrder().get(i).getFood(), foodByPosition[i]);
        }

        // Places totems


    }

    @Test
    public void testSetupGameException()
    {
        MesosGame game = new MesosGame();
        assertThrows(IllegalArgumentException.class, () -> game.setupGame(1, new ArrayList<>()));
    }

}
