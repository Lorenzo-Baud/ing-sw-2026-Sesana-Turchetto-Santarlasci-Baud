package it.polimi.ingws.gc8.Model.Board;

import it.polimi.ingws.gc8.Model.Cards.BuildingCard.BuildingCard;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Artist;
import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.Cards.EventCards.HuntEventCard;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;
    private BuildingCard b1;
    private BuildingCard b2;
    private BuildingCard b3;
    private BuildingCard b4;
    private BuildingCard b5;
    private Artist a1;
    private Artist a2;
    private EventCard e1;
    private EventCard e2;

    @BeforeEach
    void setUp() {
        b1 = new BuildingCard("B1", 1, 1, Era.ERA_1, "f1", "bk1", null);
        b2 = new BuildingCard("B2", 2, 2, Era.ERA_1, "f2", "bk2", null);
        b3 = new BuildingCard("B3", 3, 3, Era.ERA_2, "f3", "bk3", null);
        b4 = new BuildingCard("B4", 4, 4, Era.ERA_2, "f4", "bk4", null);
        b5 = new BuildingCard("B5", 5, 5, Era.ERA_3, "f5", "bk5", null);

        a1 = new Artist("ca1", "cab1", Era.ERA_1, 0);
        a2 = new Artist("ca2", "cab2", Era.ERA_2, 0);

        e1 = new HuntEventCard("ev1", "evb1", Era.ERA_1, false, EventType.hunt, 2);
        e2 = new HuntEventCard("ev2", "evb2", Era.ERA_2, false, EventType.hunt, 1);

        Deque<Card> tribeDeck = new ArrayDeque<>(List.of(a2, e2));
        List<Card> lowerRow = new ArrayList<>(List.of(b1, a1, e1));
        List<Card> upperRow = new ArrayList<>(List.of(a2, b2, e2));
        Deque<Card> era2Deck = new ArrayDeque<>(List.of(b3, b4));
        Deque<Card> era3Deck = new ArrayDeque<>(List.of(b5));

        board = new Board(
                tribeDeck,
                lowerRow,
                upperRow,
                new TurnOrderTrack(4),
                era2Deck,
                era3Deck,
                new OfferTrack(4)
        );
    }

    @Test
    void constructor_withPlayerCount_initializesCollectionsAndTracks() {
        Board generatedBoard = new Board(4);

        assertNotNull(generatedBoard.getTribeDeck());
        assertNotNull(generatedBoard.getLowerRow());
        assertNotNull(generatedBoard.getUpperRow());
        assertNotNull(generatedBoard.getOfferTrack());
        assertNotNull(generatedBoard.getTurnOrderTrack());
        assertNotNull(generatedBoard.getEra2BuildingDeck());
        assertNotNull(generatedBoard.getEra3BuildingDeck());
    }

    @Test
    void setupInitialRows_distributesCardsBetweenRows() {
        Board emptyBoard = new Board(
                new ArrayDeque<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new TurnOrderTrack(3),
                new ArrayDeque<>(),
                new ArrayDeque<>(),
                new OfferTrack(3)
        );

        List<BuildingCard> era1Buildings = List.of(
                b1,
                b2,
                new BuildingCard("B6", 1, 1, Era.ERA_1, "f6", "bk6", null),
                new BuildingCard("B7", 1, 1, Era.ERA_1, "f7", "bk7", null),
                new BuildingCard("B8", 1, 1, Era.ERA_1, "f8", "bk8", null),
                new BuildingCard("B9", 1, 1, Era.ERA_1, "f9", "bk9", null),
                new BuildingCard("B10", 1, 1, Era.ERA_1, "f10", "bk10", null),
                new BuildingCard("B11", 1, 1, Era.ERA_1, "f11", "bk11", null)
        );

        emptyBoard.setupInitialRows(3, era1Buildings);

        assertEquals(4, emptyBoard.getLowerRow().size());
        assertEquals(4, emptyBoard.getUpperRow().size());
        assertEquals(era1Buildings.get(0), emptyBoard.getLowerRow().get(0));
        assertEquals(era1Buildings.get(7), emptyBoard.getUpperRow().get(3));
    }

    @Test
    void getLowerRowCard_withInvalidIndex_throwsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> board.getLowerRowCard(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.getLowerRowCard(3));
    }

    @Test
    void getUpperRowCard_withInvalidIndex_throwsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> board.getUpperRowCard(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.getUpperRowCard(3));
    }

    @Test
    void removeLowerRowCard_removesExpectedCard() {
        Card removed = board.removeLowerRowCard(1);

        assertEquals(a1, removed);
        assertEquals(2, board.getLowerRow().size());
        assertFalse(board.getLowerRow().contains(a1));
    }

    @Test
    void removeUpperRowCard_removesExpectedCard() {
        Card removed = board.removeUpperRowCard(2);

        assertEquals(e2, removed);
        assertEquals(2, board.getUpperRow().size());
        assertFalse(board.getUpperRow().contains(e2));
    }

    @Test
    void getLowerRowEvents_returnsOnlyEventCards() {
        List<EventCard> events = board.getLowerRowEvents();

        assertEquals(1, events.size());
        assertEquals(e1, events.get(0));
    }


    @Test
    void testLoadTribeDeck_FileNotFound() {
        File realFile = new File("src/main/resources/character_cards.json");
        File tempFile = new File("src/main/resources/character_cards_temp.json");

        // Rename file
        if (realFile.exists()) realFile.renameTo(tempFile);
        board.loadTribeDeck(2);
        // Restore the file
        if (tempFile.exists()) tempFile.renameTo(realFile);
    }

    @Test
    void testLoadEventDeck_FileNotFound() {
        File realFile = new File("src/main/resources/event_cards.json");
        File tempFile = new File("src/main/resources/event_cards_temp.json");

        // Rename file
        if (realFile.exists()) realFile.renameTo(tempFile);
        board.loadTribeDeck(2);
        // Restore the file
        if (tempFile.exists()) tempFile.renameTo(realFile);
    }

    @Test
    void testSetTribeDeck() {
        Board board = new Board(2);

        Deque<Card> newDeck = new ArrayDeque<>();

        Card dummyCard = new Card("front", "back", Era.ERA_1) {
        };

        newDeck.add(dummyCard);

        board.setTribeDeck(newDeck);

        assertSame(newDeck, board.getTribeDeck(), "The tribe deck should be the one we just set");
        assertEquals(1, board.getTribeDeck().size(), "The deck size should match the new deck");
    }

    @Test
    void testSetLowerRow() {
        Board board = new Board(2);

        List<Card> newLowerRow = new ArrayList<>();

        Card testCard = new Card("front_img", "back_img", Era.ERA_1) {
        };
        newLowerRow.add(testCard);

        board.setLowerRow(newLowerRow);

        assertSame(newLowerRow, board.getLowerRow(), "The lower row list should be updated to the provided list.");
    }

    @Test
    void testSetUpperRow() {
        Board board = new Board(3);

        List<Card> newUpperRow = new ArrayList<>();

        Card dummy = new Card("front_path", "back_path", Era.ERA_2) {};
        newUpperRow.add(dummy);

        board.setUpperRow(newUpperRow);

        assertSame(newUpperRow, board.getUpperRow(), "The upper row should match the list we provided.");
    }

    @Test
    void testSetTurnOrderTrack() {
        Board board = new Board(2);
        TurnOrderTrack newTrack = new TurnOrderTrack(2);

        board.setTurnOrderTrack(newTrack);

        assertSame(newTrack, board.getTurnOrderTrack());
    }

    @Test
    void testSetEra2BuildingDeck() {
        Board board = new Board(2);
        Deque<Card> newDeck = new ArrayDeque<>();

        board.setEra2BuildingDeck(newDeck);

        assertSame(newDeck, board.getEra2BuildingDeck());
    }

    @Test
    void testSetEra3BuildingDeck() {
        Board board = new Board(2);
        Deque<Card> newDeck = new ArrayDeque<>();

        board.setEra3BuildingDeck(newDeck);

        assertSame(newDeck, board.getEra3BuildingDeck());
    }

    @Test
    void testSetOfferTrack() {
        Board board = new Board(2);
        OfferTrack newTrack = new OfferTrack(2);

        board.setOfferTrack(newTrack);

        assertSame(newTrack, board.getOfferTrack());
    }

    @Test
    void testGetLowerRowCard_Success() {
        Board board = new Board(2);
        List<Card> cards = new ArrayList<>();
        Card dummy = new Card("f", "b", Era.ERA_1) {};
        cards.add(dummy);
        board.setLowerRow(cards);

        assertEquals(dummy, board.getLowerRowCard(0));
    }

    @Test
    void testGetUpperRowCard_Success() {
        Board board = new Board(2);
        List<Card> cards = new ArrayList<>();
        Card dummy = new Card("f", "b", Era.ERA_1) {};
        cards.add(dummy);
        board.setUpperRow(cards);

        assertEquals(dummy, board.getUpperRowCard(0));
    }

    @Test
    void testGetUpperRowCard_NegativeIndex() {
        Board board = new Board(2);
        assertThrows(IndexOutOfBoundsException.class, () -> board.getUpperRowCard(-1));
    }

    @Test
    void testGetUpperRowCard_IndexTooHigh() {
        Board board = new Board(2);
        board.setUpperRow(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> board.getUpperRowCard(0));
    }

    @Test
    void testGetLowerRowCard_NegativeIndex() {
        Board board = new Board(2);
        assertThrows(IndexOutOfBoundsException.class, () -> board.getLowerRowCard(-1));
    }

    @Test
    void discardLowerRowTribeCards_keepsOnlyBuildings() {
        board.discardLowerRowTribeCards();

        assertEquals(1, board.getLowerRow().size());
        assertEquals(b1, board.getLowerRow().get(0));
    }

    @Test
    void moveUpperRowTribeCardsToLower_movesOnlyTribeCards() {
        board.moveUpperRowTribeCardsToLower();

        assertEquals(List.of(b2), board.getUpperRow());
        assertTrue(board.getLowerRow().contains(a2));
        assertTrue(board.getLowerRow().contains(e2));
        assertEquals(5, board.getLowerRow().size());
    }

    @Test
    void refillUpperRow_drawsUpToPlayerCountPlusOne() {
        List<Card> newCards = board.refillUpperRow(1);

        assertEquals(2, newCards.size());
        assertTrue(board.getUpperRow().containsAll(newCards));
        assertTrue(board.getTribeDeck().isEmpty());
    }

    @Test
    void refillUpperRow_withEmptyDeck_returnsEmptyList() {
        board.getTribeDeck().clear();

        List<Card> newCards = board.refillUpperRow(3);

        assertTrue(newCards.isEmpty());
        assertEquals(3, board.getUpperRow().size());
    }

    @Test
    void testRemoveLowerRowCard_Success() {
        Board board = new Board(2);
        List<Card> cards = new ArrayList<>();
        Card dummy = new Card("f", "b", Era.ERA_1) {};
        cards.add(dummy);
        board.setLowerRow(cards);

        Card removed = board.removeLowerRowCard(0);
        assertEquals(dummy, removed);
        assertTrue(board.getLowerRow().isEmpty());
    }

    @Test
    void testRemoveUpperRowCard_Success() {
        Board board = new Board(2);
        List<Card> cards = new ArrayList<>();
        Card dummy = new Card("f", "b", Era.ERA_1) {};
        cards.add(dummy);
        board.setUpperRow(cards);

        Card removed = board.removeUpperRowCard(0);
        assertEquals(dummy, removed);
        assertTrue(board.getUpperRow().isEmpty());
    }

    @Test
    void testRemoveUpperRowCard_NegativeIndex() {
        Board board = new Board(2);
        assertThrows(IndexOutOfBoundsException.class, () -> board.removeUpperRowCard(-1));
    }

    @Test
    void testRemoveUpperRowCard_IndexTooHigh() {
        Board board = new Board(2);
        board.setUpperRow(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> board.removeUpperRowCard(0));
    }

    @Test
    void testRemoveLowerRowCard_NegativeIndex() {
        Board board = new Board(2);
        assertThrows(IndexOutOfBoundsException.class, () -> board.removeLowerRowCard(-1));
    }

    @Test
    void testRemoveLowerRowCard_IndexTooHigh() {
        Board board = new Board(2);
        board.setLowerRow(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> board.removeLowerRowCard(0));
    }

    @Test
    void discardLowerRowBuilding_removesOnlyBuildingCards() {
        board.discardLowerRowBuilding();

        assertEquals(2, board.getLowerRow().size());
        assertFalse(board.getLowerRow().contains(b1));
        assertTrue(board.getLowerRow().contains(a1));
        assertTrue(board.getLowerRow().contains(e1));
    }

    @Test
    void moveUpperRowBuildingToLower_movesOnlyBuildingCards() {
        board.moveUpperRowBuildingToLower();

        assertFalse(board.getUpperRow().contains(b2));
        assertTrue(board.getLowerRow().contains(b2));
        assertEquals(6, board.getUpperRow().size() + board.getLowerRow().size());
    }

    @Test
    void revealEraBuildings_forEra2_revealsFromEra2DeckFromLast() {
        board.revealEraBuildings(2);

        assertTrue(board.getEra2BuildingDeck().isEmpty());
        assertEquals(b4, board.getUpperRow().get(3));
        assertEquals(b3, board.getUpperRow().get(4));
    }

    @Test
    void revealEraBuildings_forEra3_revealsFromEra3Deck() {
        int previousUpperSize = board.getUpperRow().size();

        board.revealEraBuildings(3);

        assertEquals(previousUpperSize + 1, board.getUpperRow().size());
        assertTrue(board.getEra3BuildingDeck().isEmpty());
        assertEquals(b5, board.getUpperRow().get(board.getUpperRow().size() - 1));
    }
}
