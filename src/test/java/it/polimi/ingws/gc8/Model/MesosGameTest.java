package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.CardRow;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
        MesosGame game = new MesosGame();

        List<Player> players = new ArrayList<>();
        players.add(new Player(1, "Player1", TotemColor.RED));
        players.add(new Player(2, "Player2", TotemColor.RED));
        players.add(new Player(3, "Player3", TotemColor.RED));

        // Game setup
        game.setupGame(numPlayers, players);


        // ROUND 1
        // -----------------------------------------------------------------------------
        System.out.println("ROUND 1");
        // Places totems on offer track randomly
        assertEquals(numPlayers, game.getPlayersInTurnOrder().size());
        for(Player p : game.getPlayersInTurnOrder()) {
            OfferTile t = game.getAvailableOfferTiles().get(ThreadLocalRandom.current().nextInt(game.getAvailableOfferTiles().size()));
            game.placeTotem(p, t);
            System.out.println("Player " + p.getNickname() + " placed a totem on tile " + t.getId());
        }
        assertTrue(game.allTotemsPlaced());
        assertEquals(2, game.getAvailableOfferTiles().size());
        assertEquals(0, game.getPlayersInTurnOrder().size());

        // Each player picks cards
        for(Player p : game.getPlayersInOfferOrder()) {

            if(game.getOfferTile(p).getFoodReward() > 0) {
                game.takeFood(p);
            }

            // Picks from upper row
            int pickedBuildingCards = 0;
            int pickedCharactersCards = 0;

            for(int i = 0; i < game.getOfferTile(p).getUpperRowPicks(); i++) {
                Card c = game.getAvailableCardsFromRow(CardRow.UPPER).get(ThreadLocalRandom.current().nextInt(game.getAvailableCardsFromRow(CardRow.UPPER).size()));
                if (c.getCardType().equals(CardType.CHARACTER)) {
                    game.takeCharacterCard(p, (CharacterCard) c);
                    pickedCharactersCards++;
                } else {
                    game.takeBuildingCard(p, (BuildingCard) c);
                    pickedBuildingCards++;
                }
            }

            // Picks from lower row
            for(int i = 0; i < game.getOfferTile(p).getLowerRowPicks(); i++) {
                Card c = game.getAvailableCardsFromRow(CardRow.LOWER).get(ThreadLocalRandom.current().nextInt(game.getAvailableCardsFromRow(CardRow.LOWER).size()));
                if (c.getCardType().equals(CardType.CHARACTER)) {
                    game.takeCharacterCard(p, (CharacterCard) c);
                    pickedCharactersCards++;
                } else {
                    game.takeBuildingCard(p, (BuildingCard) c);
                    pickedBuildingCards++;
                }
            }

            assertEquals(pickedCharactersCards, p.getTribe().getCharacters().size());
            assertEquals(pickedBuildingCards, p.getTribe().getBuildings().size());
            assertEquals(game.getOfferTile(p).getLowerRowPicks() + game.getOfferTile(p).getUpperRowPicks(), pickedCharactersCards + pickedBuildingCards);

            game.returnTotemToTurnOrder(p);

        }

        assertEquals(numPlayers, game.getPlayersInTurnOrder().size());

        // Discards all lower row cards
        game.discardLowerRow();
        assertEquals(0, game.getAvailableCardsFromRow(CardRow.LOWER).size());

        // Moves upper row cards into lower row
        game.moveUpperRowToLower();
        int availableCards = 0;
        game.refreshUpperRow();
        for(Card card : game.getBoard().getUpperRow()) {
            if(!card.getCardType().equals(CardType.BUILDING)) {
                availableCards++;
            }
        }
        assertEquals(numPlayers + 4, availableCards);

        assertEquals(2, game.getCurrentRound());
        // -----------------------------------------------------------------------------


        // ROUND 2
        // -----------------------------------------------------------------------------
        System.out.println("\n\nROUND 2");
        // Totem placing
        assertEquals(numPlayers, game.getPlayersInTurnOrder().size());

        for(Player p : game.getPlayersInTurnOrder()) {
            OfferTile t = game.getAvailableOfferTiles().get(ThreadLocalRandom.current().nextInt(game.getAvailableOfferTiles().size()));
            game.placeTotem(p, t);
            System.out.println("Player " + p.getNickname() + " placed a totem on tile " + t.getId());
        }
        assertEquals(0, game.getPlayersInTurnOrder().size());
        assertTrue(game.allTotemsPlaced());
        assertEquals(2, game.getAvailableOfferTiles().size());


        for(Player p : game.getPlayersInOfferOrder()) {

            if(game.getOfferTile(p).getFoodReward() > 0) {
                game.takeFood(p);
            }

            // Picks from upper row
            int pickedBuildingCards = 0;
            int pickedCharactersCards = 0;

            for(int i = 0; i < game.getOfferTile(p).getUpperRowPicks(); i++) {
                Card c = game.getAvailableCardsFromRow(CardRow.UPPER).get(ThreadLocalRandom.current().nextInt(game.getAvailableCardsFromRow(CardRow.UPPER).size()));
                if (c.getCardType().equals(CardType.CHARACTER)) {
                    game.takeCharacterCard(p, (CharacterCard) c);
                    pickedCharactersCards++;
                } else {
                    game.takeBuildingCard(p, (BuildingCard) c);
                    pickedBuildingCards++;
                }
            }

            // Picks from lower row
            for(int i = 0; i < game.getOfferTile(p).getLowerRowPicks(); i++) {
                Card c = game.getAvailableCardsFromRow(CardRow.LOWER).get(ThreadLocalRandom.current().nextInt(game.getAvailableCardsFromRow(CardRow.LOWER).size()));
                if (c.getCardType().equals(CardType.CHARACTER)) {
                    game.takeCharacterCard(p, (CharacterCard) c);
                    pickedCharactersCards++;
                } else {
                    game.takeBuildingCard(p, (BuildingCard) c);
                    pickedBuildingCards++;
                }
            }

            assertEquals(game.getOfferTile(p).getLowerRowPicks() + game.getOfferTile(p).getUpperRowPicks(), pickedCharactersCards + pickedBuildingCards);
            System.out.println(p.getNickname() + " picked " + pickedCharactersCards + " character cards and " + pickedBuildingCards + " building cards.");

        }

        // Event resolution

        // -----------------------------------------------------------------------------


    }

    /**
     * Tests if offer tracks is correct
     */
    @Test
    public void testOfferTrack() {

        int playerNum = ThreadLocalRandom.current().nextInt(2, 6);

        MesosGame game = new MesosGame();

        List<Player> players = new ArrayList<>();
        for(int i = 0; i < playerNum; i++) {
            players.add(new Player(i, "Player" + i, TotemColor.values()[i]));
        }

        game.setupGame(playerNum, players);

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
        MesosGame game = new MesosGame();
        assertThrows(IllegalArgumentException.class, () -> game.setupGame(1, new ArrayList<>()));
    }


    /**
     * Tests if the game has been correctly initialized
     */
    @Test
    public void testGameInitialization() {

        int playerNum = ThreadLocalRandom.current().nextInt(2, 6);

        MesosGame game = new MesosGame();

        List<Player> players = new ArrayList<>();
        for(int i = 0; i < playerNum; i++) {
            players.add(new Player(i, "Player" + i, TotemColor.values()[i]));
        }

        // Constructor test
        assertEquals(0, game.getPlayers().size());
        assertEquals(0, game.getCurrentRound());
        assertEquals(Era.ERA_1, game.getCurrentEra());

        // Game setup
        game.setupGame(playerNum, players);

        // Tests initial randomization
        int[] foodByPosition = {2, 3, 3, 4, 4};
        for(int i = 0; i < game.getPlayersInTurnOrder().size(); i++) {
            assertEquals(game.getPlayersInTurnOrder().get(i).getFood(), foodByPosition[i]);
        }

        // Checks card initialization
        // Lower row
        assertEquals(playerNum + 1, game.getAvailableCardsFromRow(CardRow.LOWER).size());
        for(Card c : game.getAvailableCardsFromRow(CardRow.LOWER)) {
            assertFalse(c instanceof BuildingCard);            //assertEquals(CardRow.UPPER, c.getRow());
        }

        // Upper row
        for(Card c : game.getAvailableCardsFromRow(CardRow.UPPER)) {
            if(c instanceof EventCard) {
                System.out.println("Event card in lowe row");
            }
        }
        if(playerNum == 2) {
            assertEquals(playerNum + 4 + 1, game.getBoard().getUpperRow().size());
        } else {
            assertEquals(playerNum + 4 + 2, game.getBoard().getUpperRow().size());
        }

        // Checks building decks
        // ERA_2
        if(playerNum <= 3) {
            assertEquals(2, game.getBoard().getEra2BuildingDeck().size());
        } else {
            assertEquals(3, game.getBoard().getEra2BuildingDeck().size());
        }

        // ERA_3
        if(playerNum == 2) {
            assertEquals(3, game.getBoard().getEra3BuildingDeck().size());
        } else if (playerNum == 3 || playerNum == 4) {
            assertEquals(4, game.getBoard().getEra3BuildingDeck().size());
        } else {
            assertEquals(5, game.getBoard().getEra3BuildingDeck().size());
        }

        for(Player p : game.getPlayersInTurnOrder()) {
            assertNull(game.getOfferTile(p));
        }

    }
}