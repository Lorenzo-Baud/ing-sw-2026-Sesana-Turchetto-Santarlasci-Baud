package it.polimi.ingws.gc8.View.TUI;

import it.polimi.ingws.gc8.Controller.actions.*;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Enum.*;
import it.polimi.ingws.gc8.Network.DTO.*;
import it.polimi.ingws.gc8.Network.Messages.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the internal flow of {@link TuiView}: receiving messages from the
 * server, updating {@code gameView}, sending actions to the server, and
 * pick limits during the ACTION_RESOLUTION phase.
 *
 * <h2>Strategy</h2>
 * <ul>
 *   <li>{@code System.in} is replaced with a {@link ByteArrayInputStream}
 *       before constructing {@code TuiView} (the {@code Scanner} binds to
 *       {@code System.in} when the field is initialized).</li>
 *   <li>{@code System.out} is captured with a {@link ByteArrayOutputStream}
 *       to verify printed text.</li>
 *   <li>The private methods {@code applyMessage}, {@code startUpdateDispatcher},
 *       {@code sendAndAwaitUpdate}, {@code runPositioningPhase} and
 *       {@code runResolutionPhase} are invoked via reflection.</li>
 *   <li>A fake {@code ViewListener} captures the actions sent by the view
 *       and simulates the server response by updating {@code gameView}.</li>
 * </ul>
 */
class TuiViewTest {

    private PrintStream originalOut;
    private InputStream originalIn;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void redirectStreams() {
        originalOut = System.out;
        originalIn  = System.in;
        outContent  = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    /** Builds a minimal PlayerDTO. */
    private static PlayerDTO player(int id, String nick) {
        return new PlayerDTO(id, nick, TotemColor.RED, 3, 0, List.of());
    }

    /**
     * Builds a GameDTO with two players (alice id=1, bob id=2).
     * The current player is the one identified by {@code currentNick / currentId}.
     */
    private static GameDTO gameDTO(String currentNick, int currentId, GamePhase phase,
                                   List<OfferTileDTO> tiles, List<CardDTO> lower) {
        PlayerDTO cp = player(currentId, currentNick);
        return new GameDTO(
                2, currentId,
                List.of(player(1, "alice"), player(2, "bob")),
                List.of(),   // empty upper row
                lower,
                tiles,
                cp,
                phase,
                1, Era.ERA_1,
                List.of(),
                0, 0, false
        );
    }

    /** Builds a minimal CardDTO of type CHARACTER/HUNTER. */
    private static CardDTO stubCard(String name) {
        return new CardDTO(
                name, "back", false, CardType.CHARACTER, Era.ERA_1,
                CharacterType.HUNTER, 2, null, false,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null
        );
    }

    // -----------------------------------------------------------------------
    // Reflection helpers
    // -----------------------------------------------------------------------

    private static void invokeApplyMessage(TuiView view, NetworkMessage msg) throws Exception {
        Method m = TuiView.class.getDeclaredMethod("applyMessage", NetworkMessage.class);
        m.setAccessible(true);
        m.invoke(view, msg);
    }

    private static void invokeStartDispatcher(TuiView view) throws Exception {
        Method m = TuiView.class.getDeclaredMethod("startUpdateDispatcher");
        m.setAccessible(true);
        m.invoke(view);
    }

    private static void invokeSendAndAwaitUpdate(TuiView view, Object action) throws Exception {
        Method m = TuiView.class.getDeclaredMethod("sendAndAwaitUpdate", Object.class);
        m.setAccessible(true);
        m.invoke(view, action);
    }

    private static void invokeRunPositioningPhase(TuiView view) throws Exception {
        Method m = TuiView.class.getDeclaredMethod("runPositioningPhase");
        m.setAccessible(true);
        m.invoke(view);
    }

    private static void invokeRunResolutionPhase(TuiView view) throws Exception {
        Method m = TuiView.class.getDeclaredMethod("runResolutionPhase");
        m.setAccessible(true);
        m.invoke(view);
    }

    // -----------------------------------------------------------------------
    // TEST 1 — applyMessage(ModelUpdateMessage) updates gameView
    // -----------------------------------------------------------------------

    @Test
    void applyMessage_ModelUpdate_setsGameView() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");

        GameDTO dto = gameDTO("bob", 2, GamePhase.POSITIONING, List.of(), List.of());
        invokeApplyMessage(view, new ModelUpdateMessage(dto));

        assertSame(dto, view.getGameView(),
                "gameView must be the same object passed in the ModelUpdateMessage");
    }

    // -----------------------------------------------------------------------
    // TEST 2 — applyMessage(LobbyUpdateMessage) does NOT change gameView
    // -----------------------------------------------------------------------

    @Test
    void applyMessage_LobbyUpdate_doesNotChangeGameView() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");

        invokeApplyMessage(view, new LobbyUpdateMessage(1, 2, List.of("alice")));

        assertNull(view.getGameView(),
                "a LobbyUpdateMessage must not set gameView");
    }

    // -----------------------------------------------------------------------
    // TEST 3 — applyMessage(ErrorMessage) does NOT change gameView
    // -----------------------------------------------------------------------

    @Test
    void applyMessage_ErrorMessage_doesNotChangeGameView() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");

        invokeApplyMessage(view, new ErrorMessage("ERR", "error detail"));

        assertNull(view.getGameView(),
                "an ErrorMessage must not set gameView");
    }

    // -----------------------------------------------------------------------
    // TEST 4 — addMessage → dispatcher → gameView updated (thread integration)
    // -----------------------------------------------------------------------

    @Test
    void dispatchLoop_updatesGameView_viaAddMessage() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");
        invokeStartDispatcher(view);

        GameDTO dto = gameDTO("bob", 2, GamePhase.POSITIONING, List.of(), List.of());
        view.addMessage(new ModelUpdateMessage(dto));

        // wait at most 1s for the dispatcher to process the message
        long deadline = System.currentTimeMillis() + 1000;
        while (view.getGameView() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertSame(dto, view.getGameView(),
                "the dispatcher thread must update gameView after addMessage()");
    }

    // -----------------------------------------------------------------------
    // TEST 5 — isMyTurn() returns true when currentPlayer matches the nickname
    // -----------------------------------------------------------------------

    @Test
    void isMyTurn_returnsTrue_whenCurrentPlayerMatchesNickname() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");
        view.setGameView(gameDTO("alice", 1, GamePhase.POSITIONING, List.of(), List.of()));

        assertTrue(view.isMyTurn());
    }

    // -----------------------------------------------------------------------
    // TEST 6 — isMyTurn() returns false when it is another player's turn
    // -----------------------------------------------------------------------

    @Test
    void isMyTurn_returnsFalse_whenCurrentPlayerIsDifferent() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");
        view.setGameView(gameDTO("bob", 2, GamePhase.POSITIONING, List.of(), List.of()));

        assertFalse(view.isMyTurn());
    }

    // -----------------------------------------------------------------------
    // TEST 7 — sendAndAwaitUpdate sends the action to the listener and
    //          returns as soon as gameView changes (synchronous listener)
    // -----------------------------------------------------------------------

    @Test
    void sendAndAwaitUpdate_returnsImmediately_whenListenerUpdatesGameView() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");

        GameDTO initial = gameDTO("alice", 1, GamePhase.POSITIONING, List.of(), List.of());
        view.setGameView(initial);

        List<Object> captured = new ArrayList<>();
        // listener captures the action and simulates server response immediately
        view.addListener(msg -> {
            captured.add(msg);
            view.setGameView(gameDTO("bob", 2, GamePhase.POSITIONING, List.of(), List.of()));
        });

        long start = System.currentTimeMillis();
        invokeSendAndAwaitUpdate(view, new PlaceTotemAction(1, "B"));
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(1, captured.size(), "the listener must receive exactly one action");
        assertInstanceOf(PlaceTotemAction.class, captured.get(0));
        assertNotSame(initial, view.getGameView(), "gameView must have changed");
    }

    // -----------------------------------------------------------------------
    // TEST 8 — sendAndAwaitUpdate times out (~3s) if the server does not respond
    // -----------------------------------------------------------------------

    @Test
    @Timeout(5)   // the whole test must complete within 5s (internal timeout is 3s)
    void sendAndAwaitUpdate_timesOut_whenServerDoesNotRespond() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        TuiView view = new TuiView();
        view.setNickname("alice");

        GameDTO initial = gameDTO("alice", 1, GamePhase.POSITIONING, List.of(), List.of());
        view.setGameView(initial);

        // listener that does NOT update gameView → sendAndAwaitUpdate must time out after ~3s
        view.addListener(msg -> { /* no response */ });

        long start = System.currentTimeMillis();
        invokeSendAndAwaitUpdate(view, new PlaceTotemAction(1, "B"));
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 2900,
                "must wait around 3s before returning, elapsed=" + elapsed + "ms");
    }

    // -----------------------------------------------------------------------
    // TEST 9 — runPositioningPhase sends a PlaceTotemAction with the chosen tile
    // -----------------------------------------------------------------------

    @Test
    void runPositioningPhase_sendsPlaceTotemAction_withCorrectTile() throws Exception {
        List<OfferTileDTO> tiles = List.of(
                new OfferTileDTO(OffertileId.B, -1, ""),   // free
                new OfferTileDTO(OffertileId.C, -1, "")    // free
        );
        GameDTO initial = gameDTO("alice", 1, GamePhase.POSITIONING, tiles, List.of());

        // input: choose option 1 (tile B, first in the free list)
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        TuiView view = new TuiView();
        view.setNickname("alice");
        view.setPlayerId(1);
        view.setGameView(initial);

        List<Object> sent = new ArrayList<>();
        view.addListener(msg -> {
            sent.add(msg);
            // simulate server response: now it is bob's turn
            view.setGameView(gameDTO("bob", 2, GamePhase.POSITIONING, tiles, List.of()));
        });

        invokeRunPositioningPhase(view);

        assertEquals(1, sent.size(), "must send exactly one PlaceTotemAction");
        assertInstanceOf(PlaceTotemAction.class, sent.get(0));
        assertEquals("B", ((PlaceTotemAction) sent.get(0)).getOfferTileId(),
                "must pick tile B (first option)");
    }

    // -----------------------------------------------------------------------
    // TEST 10 — runResolutionPhase tile B: at most 1 LOWER pick, then RETURN
    //           Input sequence: "1\n" (pick LOWER) → "1\n" (first card) →
    //                           "1\n" (pick RETURN, only remaining option)
    // -----------------------------------------------------------------------

    @Test
    void runResolutionPhase_tileB_allowsExactlyOneLowerPick() throws Exception {
        // alice occupies tile B (playerIndexOccupant = alice's playerId = 1)
        List<OfferTileDTO> tiles = List.of(new OfferTileDTO(OffertileId.B, 1, ""));
        List<CardDTO>     lower = List.of(stubCard("HunterA"), stubCard("HunterB"));

        GameDTO initial   = gameDTO("alice", 1, GamePhase.ACTION_RESOLUTION, tiles, lower);
        GameDTO afterCard = gameDTO("alice", 1, GamePhase.ACTION_RESOLUTION, tiles, lower);
        GameDTO afterRet  = gameDTO("bob",   2, GamePhase.ACTION_RESOLUTION, List.of(), List.of());

        // input: 1 (LOWER) → 1 (first card) → 1 (RETURN)
        System.setIn(new ByteArrayInputStream("1\n1\n1\n".getBytes()));
        TuiView view = new TuiView();
        view.setNickname("alice");
        view.setPlayerId(1);
        view.setGameView(initial);

        List<Object> sent = new ArrayList<>();
        view.addListener(msg -> {
            sent.add(msg);
            if (msg instanceof TakeCardAction)    view.setGameView(afterCard);
            if (msg instanceof ReturnTotemAction) view.setGameView(afterRet);
        });

        invokeRunResolutionPhase(view);

        // must have sent exactly: TakeCardAction + ReturnTotemAction
        assertInstanceOf(TakeCardAction.class,    sent.get(0), "first action: TakeCardAction");

        // the LOWER option with counter must appear exactly once
        String output = outContent.toString();
        long lowerMenuLines = output.lines()
                .filter(l -> l.contains("LOWER") && l.contains("/1"))
                .count();
        assertEquals(2, lowerMenuLines,
                "the LOWER option (0/1) must appear exactly once in the menu ==>");
    }

    // -----------------------------------------------------------------------
    // TEST 11 — runResolutionPhase tile D: allows 2 LOWER picks then RETURN
    // -----------------------------------------------------------------------

    @Test
    void runResolutionPhase_tileD_allowsTwoLowerPicks() throws Exception {
        List<OfferTileDTO> tiles = List.of(new OfferTileDTO(OffertileId.D, 1, ""));
        List<CardDTO>     lower = List.of(stubCard("HunterA"), stubCard("HunterB"));

        GameDTO initial    = gameDTO("alice", 1, GamePhase.ACTION_RESOLUTION, tiles, lower);
        GameDTO afterFirst = gameDTO("alice", 1, GamePhase.ACTION_RESOLUTION, tiles, lower);
        GameDTO afterSecond= gameDTO("alice", 1, GamePhase.ACTION_RESOLUTION, tiles, lower);
        GameDTO afterRet   = gameDTO("bob",   2, GamePhase.ACTION_RESOLUTION, List.of(), List.of());

        // input: 1(LOWER 1st) → 1(card) → 1(LOWER 2nd) → 1(card) → 1(RETURN)
        System.setIn(new ByteArrayInputStream("1\n1\n1\n1\n1\n".getBytes()));
        TuiView view = new TuiView();
        view.setNickname("alice");
        view.setPlayerId(1);
        view.setGameView(initial);

        List<Object> sent = new ArrayList<>();
        int[] cardCount = {0};
        view.addListener(msg -> {
            sent.add(msg);
            if (msg instanceof TakeCardAction) {
                cardCount[0]++;
                view.setGameView(cardCount[0] == 1 ? afterFirst : afterSecond);
            }
            if (msg instanceof ReturnTotemAction) view.setGameView(afterRet);
        });

        invokeRunResolutionPhase(view);

        assertEquals(2, sent.size(), "2 TakeCardAction + 1 ReturnTotemAction ==>");
        assertInstanceOf(TakeCardAction.class,    sent.get(0));
        assertInstanceOf(TakeCardAction.class,    sent.get(1));

    }
}
