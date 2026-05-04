package it.polimi.ingws.gc8.Model;

import it.polimi.ingws.gc8.Model.Board.Board;
import it.polimi.ingws.gc8.Model.Board.OfferTile;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingEffect;
import it.polimi.ingws.gc8.Model.Cards.BuildingCard.SetCompletionEffect;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.*;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.HuntEventCard;
import it.polimi.ingws.gc8.Model.Enum.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

    @Test

    public void testThreeRoundsAndFinalDeterministic() {
        // Setup
        MesosGame game = new MesosGame();
        Player p1 = new Player(1, "Player1", TotemColor.RED);
        Player p2 = new Player(2, "Player2", TotemColor.BLUE);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        game.setupGame(2, players);

        p1.addFood(10);
        p2.addFood(10);

        Board board = game.getBoard();

        // Creating deterministic cards for the initial board
        Builder initialLowerBuilder = new Builder("builder_front.png", "builder_back.png", Era.ERA_1, 1, 2, 2);
        Inventor initialUpperInventor = new Inventor("inventor_front.png", "inventor_back.png", Era.ERA_1, 2, 1);

        // Changing the initial rows (that were randomly generated)
        board.setLowerRow(new ArrayList<>(List.of(initialLowerBuilder)));
        board.setUpperRow(new ArrayList<>(List.of(initialUpperInventor)));

        // Creating a deterministic deck for the three rounds
        Deque<Card> deterministicDeck = new ArrayDeque<>();
        Hunter round1Hunter = new Hunter("hunter_front.png", "hunter_back.png", Era.ERA_1, 2, false);
        Shaman round1Shaman = new Shaman("shaman_front.png", "shaman_back.png", Era.ERA_1, 1, 2);
        Artist round2Artist = new Artist("artist_front.png", "artist_back.png", Era.ERA_1, 2);
        Inventor round2Inventor = new Inventor("inventor_front.png", "inventor_back.png", Era.ERA_1, 2, 1);

        // adding an hunt event in order to test event resolution
        HuntEventCard huntEvent = new HuntEventCard("hunt_front.png", "hunt_back.png", Era.ERA_1, false, EventType.hunt, 3);

        deterministicDeck.add(round2Inventor);
        deterministicDeck.add(round1Hunter);
        deterministicDeck.add(huntEvent);
        deterministicDeck.add(round1Shaman);
        deterministicDeck.add(round2Artist);

        //adding uselsess cards in order to prevent a too early game end
        for (int i = 0; i < 15; i++) {
            deterministicDeck.add(new Artist("artist_front.png", "artist_back.png", Era.ERA_1, 2));
        }

        board.setTribeDeck(deterministicDeck);


        // Round 1

        game.placeTotem(p1, board.getOfferTrack().getTileById(OffertileId.B));
        game.placeTotem(p2, board.getOfferTrack().getTileById(OffertileId.C));
        game.takeCharacterCard(p1, initialLowerBuilder);
        game.returnTotemToTurnOrder(p1);
        game.takeCharacterCard(p2, initialUpperInventor);
        game.returnTotemToTurnOrder(p2);

         List<EventCard> events = game.getEventsToResolve();
        for (EventCard event : events) {
            event.resolveEvent(game.getPlayers());
        }

        // 2. Shift and clean the board rows
        game.discardLowerRow();
        game.moveUpperRowToLower();
        game.refreshUpperRow();
        game.checkNewEra();

        // Round 2
        game.placeTotem(p1, board.getOfferTrack().getTileById(OffertileId.C));
        game.placeTotem(p2, board.getOfferTrack().getTileById(OffertileId.B));

        game.takeCharacterCard(p1, round1Hunter);
        game.returnTotemToTurnOrder(p1);
        game.returnTotemToTurnOrder(p2);

        events = game.getEventsToResolve();
        for (EventCard event : events) {
            event.resolveEvent(game.getPlayers());
        }

        // 2. Shift and clean the board rows
        game.discardLowerRow();
        game.moveUpperRowToLower();
        game.refreshUpperRow();
        game.checkNewEra();


        // Round 3
        game.placeTotem(p1, board.getOfferTrack().getTileById(OffertileId.B));
        game.placeTotem(p2, board.getOfferTrack().getTileById(OffertileId.C));
        game.takeCharacterCard(p1, round1Shaman);
        game.returnTotemToTurnOrder(p1);
        game.takeCharacterCard(p2, round2Inventor);
        game.returnTotemToTurnOrder(p2);

        int p1FoodBeforeEvent = p1.getFood();
        int p1PPBeforeEvent = p1.getPrestigePoints();
        int p2FoodBeforeEvent = p2.getFood();
        int p2PPBeforeEvent = p2.getPrestigePoints();


         events = game.getEventsToResolve();
        for (EventCard event : events) {
            event.resolveEvent(game.getPlayers());
        }

        // 2. Shift and clean the board rows
        game.discardLowerRow();
        game.moveUpperRowToLower();
        game.refreshUpperRow();
        game.checkNewEra();


        //checks on the event resolution success and on the card management
        assertEquals(p1FoodBeforeEvent + 1, p1.getFood(), "Player 1 should have gained 1 food point from hunt event");
        assertEquals(p1PPBeforeEvent + 3, p1.getPrestigePoints(), "Player 1 should have gained 1 prestige point from hunt event");

        assertEquals(p2FoodBeforeEvent, p2.getFood(), " food points should not change for player 2");
        assertEquals(p2PPBeforeEvent, p2.getPrestigePoints(), "prestige points should not change for player 2");

        assertEquals(3, p1.getTribe().getCharacters().size(), "Player 1 should have exactly 3 drafted cards.");
        assertEquals(2, p2.getTribe().getCharacters().size(), "Player 2 should have exactly 2 drafted cards.");


        // final phase
        // clearing the  deck to trigger the end game
        board.getTribeDeck().clear();
        assertTrue(game.isGameOver(), "end game should be recognized when the deck is empty");

        // Calculating scores
        game.calculateFinalScores();

        assertEquals(5, p1.getPrestigePoints(), "Plaeyr1 should have 5 prestige points (2 from Builder, 3 from Hunt Event).");
        assertEquals(2, p2.getPrestigePoints(), "Player2 should have 2 prestige points from their Inventors.");


        assertEquals(p1, game.getWinner(), "Player1 should be the undisputed winner thanks to the Hunt Event.");
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

    @Test
    void otherMethodsTest() {

        int numPlayers = 3;
        MesosGame game = new MesosGame();

        // getCurrentPlayer
        assertNull(game.getCurrentPlayer());

        // getOfferTile
        assertNull(game.getOfferTile(null));

        List<Player> players = new ArrayList<>();
        players.add(new Player(1, "Player1", TotemColor.RED));
        players.add(new Player(2, "Player2", TotemColor.RED));
        players.add(new Player(3, "Player3", TotemColor.RED));

        // Game setup
        game.setupGame(numPlayers, players);

        // canPlayerAffordBuilding test
        assertFalse(game.canPlayerAffordBuilding(players.getFirst(), new BuildingCard("", 4, 0, Era.ERA_1, "", "", new SetCompletionEffect(3)) ));
        assertTrue(game.canPlayerAffordBuilding(players.getFirst(), new BuildingCard("", 0, 0, Era.ERA_1, "", "", new SetCompletionEffect(3)) ));

        // takeFood
        int previousFood = players.getFirst().getFood();
        game.takeFood(players.getFirst());
        assertEquals(previousFood + 3, players.getFirst().getFood());

        // returnTotemToTurnOrder
        assertThrows(IllegalStateException.class, () -> {
            game.returnTotemToTurnOrder(players.getFirst());
        });


        // canPickExtraCard
        assertFalse(game.canPickExtraCard(players.getFirst()));

        // takeExtraCard
        game.takeExtraCard(players.getFirst(), new Hunter(
                "", "", Era.ERA_1, 3, false
        ));

        game.takeExtraCard(players.getFirst(), new BuildingCard("", 4, 0, Era.ERA_1, "", "", new SetCompletionEffect(3)));
        assertEquals( 1, players.getFirst().getTribe().getCharacters().size());
        assertEquals( 1, players.getFirst().getTribe().getBuildings().size());
        assertEquals(1, players.getFirst().getTotalCharacters());
        assertEquals(0, players.getFirst().getTotalShamans());
        assertEquals(0, players.getFirst().getTotalBuildingDiscount());



        // setCurrentPlayer
        game.setCurrentPlayer(players.getFirst());
        assertEquals(players.getFirst(), game.getCurrentPlayer());

        // setupGame Exception
        assertThrows(IllegalArgumentException.class, () -> game.setupGame(1, new ArrayList<>()));

        // getNumPlayers
        assertEquals(3, game.getNumPlayers());

        // getPhase
        assertEquals(GamePhase.POSITIONING, game.getPhase());

        // setPhase
        game.setPhase(GamePhase.SETUP);
        assertEquals(GamePhase.SETUP, game.getPhase());

    }

    @Test
    void initializeTestMethodException() {
        int numPlayers = 3;
        MesosGame game = new MesosGame();


        List<Player> players = new ArrayList<>();
        players.add(new Player(1, "Player1", TotemColor.RED));
        players.add(new Player(2, "Player2", TotemColor.RED));
        players.add(null);

        // Game setup
        assertThrows(IllegalStateException.class, () -> game.setupGame(numPlayers, players));


    }

}