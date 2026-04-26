package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class MesosGameTest {

    /**
     * This test class simulates an entire mesos game
     * to ensure all the classes involved work properly
     */
    @Test
    public void testGame() {

        int numPlayers = 3;
        MesosGame game = new MesosGame(null);

        // Game setup
        game.setupGame(numPlayers);

        // Places totems on offer track randomly
        for (Player p : game.getPlayersInTurnOrder()) {
            List<OfferTile> free = game.getAvailableOfferTiles();
            OfferTile t = free.get(ThreadLocalRandom.current().nextInt(free.size()));
            game.placeTotem(p, t);
            System.out.println("Player " + p.getNickname() + " placed a totem on tile " + t.getId());
        }
        assertTrue(game.allTotemsPlaced());
        assertEquals(2, game.getAvailableOfferTiles().size());

        // Each player picks cards
        for (Player p : game.getPlayersInOfferOrder()) {

            OfferTile myTile = game.getOfferTile(p);
            assertNotNull(myTile);

            if (myTile.getFoodReward() > 0) {
                game.takeFood(p);
            }

            int pickedBuildingCards = 0;
            int pickedCharactersCards = 0;

            // Picks from upper row
            for (int i = 0; i < myTile.getUpperRowPicks(); i++) {
                int[] picked = pickRandomCardFromRow(game, p, CardRow.UPPER);
                pickedCharactersCards += picked[0];
                pickedBuildingCards   += picked[1];
            }

            // Picks from lower row
            for (int i = 0; i < myTile.getLowerRowPicks(); i++) {
                int[] picked = pickRandomCardFromRow(game, p, CardRow.LOWER);
                pickedCharactersCards += picked[0];
                pickedBuildingCards   += picked[1];
            }

            assertEquals(pickedCharactersCards, p.getTribe().getCharacters().size());
            assertEquals(pickedBuildingCards, p.getTribe().getBuildings().size());
            assertEquals(myTile.getLowerRowPicks() + myTile.getUpperRowPicks(),
                    pickedCharactersCards + pickedBuildingCards);
            System.out.println(p.getNickname() + " picked " + pickedCharactersCards
                    + " character cards and " + pickedBuildingCards + " building cards.");
        }

        // Discards all lower row cards
        game.discardLowerRow();
        assertEquals(0, game.getAvailableCardsFromRow(CardRow.LOWER).size());

        // Moves upper row cards into lower row
        game.moveUpperRowToLower();
        game.refreshUpperRow();
        int availableCards = 0;
        for (Card card : game.getBoard().getUpperRow()) {
            System.out.println(card.getCardType());
            if (!card.getCardType().equals(CardType.BUILDING)) {
                availableCards++;
            }
        }
        assertEquals(numPlayers + 4, availableCards);

        assertEquals(2, game.getCurrentRound());
    }

    /**
     * Tests if offer tracks is correct
     */
    @Test
    public void testOfferTrack() {

        int playerNum = ThreadLocalRandom.current().nextInt(2, 6);

        MesosGame game = new MesosGame(null);
        game.setupGame(playerNum);

        // Checks that all offer tiles are free at the beginning of the game
        game.getAvailableOfferTiles().forEach(t -> {
            assertTrue(t.isFree());
            assertTrue(t.isAvailableFor(playerNum));
        });

        // Checks that each offer tile bonus is right
        game.getAvailableOfferTiles().forEach(t -> {
            switch (t.getId()) {
                case A:
                    assertEquals(3, t.getFoodReward());
                    assertEquals(0, t.getUpperRowPicks());
                    assertEquals(0, t.getLowerRowPicks());
                    break;
                case B:
                    assertEquals(0, t.getFoodReward());
                    assertEquals(0, t.getUpperRowPicks());
                    assertEquals(1, t.getLowerRowPicks());
                    break;
                case C:
                    assertEquals(0, t.getFoodReward());
                    assertEquals(1, t.getUpperRowPicks());
                    assertEquals(0, t.getLowerRowPicks());
                    break;
                case D:
                    assertEquals(0, t.getFoodReward());
                    assertEquals(0, t.getUpperRowPicks());
                    assertEquals(2, t.getLowerRowPicks());
                    break;
                case E:
                    assertEquals(0, t.getFoodReward());
                    assertEquals(1, t.getUpperRowPicks());
                    assertEquals(1, t.getLowerRowPicks());
                    break;
                case F:
                    assertEquals(0, t.getFoodReward());
                    assertEquals(2, t.getUpperRowPicks());
                    assertEquals(0, t.getLowerRowPicks());
                    break;
                case G:
                    assertEquals(0, t.getFoodReward());
                    assertEquals(2, t.getUpperRowPicks());
                    assertEquals(1, t.getLowerRowPicks());
                    break;
            }
        });
    }

    /**
     * Tests that setupGame throws an exception when given invalid input
     */
    @Test
    public void testSetupGameException() {
        MesosGame game = new MesosGame(null);
        assertThrows(IllegalArgumentException.class, () -> game.setupGame(1));
        assertThrows(IllegalArgumentException.class, () -> game.setupGame(6));
    }


    /**
     * Tests if the game has been correctly initialized
     */
    @Test
    public void testGameInitialization() {

        int playerNum = ThreadLocalRandom.current().nextInt(2, 6);

        MesosGame game = new MesosGame(null);

        // Constructor test
        assertEquals(0, game.getPlayers().size());
        assertEquals(0, game.getCurrentRound());
        assertEquals(Era.ERA_1, game.getCurrentEra());

        // Game setup
        game.setupGame(playerNum);

        // Tests initial randomization
        int[] foodByPosition = {2, 3, 3, 4, 4};
        for (int i = 0; i < game.getPlayersInTurnOrder().size(); i++) {
            assertEquals(game.getPlayersInTurnOrder().get(i).getFood(), foodByPosition[i]);
        }

        // Checks card initialization
        // Lower row
        assertEquals(playerNum + 1, game.getAvailableCardsFromRow(CardRow.LOWER).size());
        for (Card c : game.getAvailableCardsFromRow(CardRow.LOWER)) {
            assertFalse(c instanceof BuildingCard);
        }

        // Upper row
        for (Card c : game.getAvailableCardsFromRow(CardRow.UPPER)) {
            if (c instanceof EventCard) {
                System.out.println("Event card in lower row");
            }
        }
        if (playerNum == 2) {
            assertEquals(playerNum + 4 + 1, game.getBoard().getUpperRow().size());
        } else {
            assertEquals(playerNum + 4 + 2, game.getBoard().getUpperRow().size());
        }

        // Checks building decks
        // ERA_2
        if (playerNum <= 3) {
            assertEquals(2, game.getBoard().getEra2BuildingDeck().size());
        } else {
            assertEquals(3, game.getBoard().getEra2BuildingDeck().size());
        }

        // ERA_3
        if (playerNum == 2) {
            assertEquals(3, game.getBoard().getEra3BuildingDeck().size());
        } else if (playerNum == 3 || playerNum == 4) {
            assertEquals(4, game.getBoard().getEra3BuildingDeck().size());
        } else {
            assertEquals(5, game.getBoard().getEra3BuildingDeck().size());
        }

        for (Player p : game.getPlayersInTurnOrder()) {
            assertNull(game.getOfferTile(p));
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Picks a random card from the given row, retrying if a building cannot
     * be afforded. Returns int[]{ pickedCharacters, pickedBuildings } so the
     * caller can update its counters.
     */
    private static int[] pickRandomCardFromRow(MesosGame game, Player p, CardRow row) {
        while (true) {
            List<Card> available = game.getAvailableCardsFromRow(row);
            Card c = available.get(ThreadLocalRandom.current().nextInt(available.size()));
            if (c.getCardType().equals(CardType.CHARACTER)) {
                game.takeCharacterCard(p, (CharacterCard) c);
                return new int[]{1, 0};
            } else {
                BuildingCard b = (BuildingCard) c;
                if (game.canPlayerAffordBuilding(p, b)) {
                    game.takeBuildingCard(p, b);
                    return new int[]{0, 1};
                }
                // not affordable: retry with another card
            }
        }
    }
}
