package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ClientConnection;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchStatus;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {

    private MatchRegistry registry;
    private MainController mainController;

    private TestConnection conn1;
    private TestConnection conn2;

    @BeforeEach
    void setUp() {
        registry = new MatchRegistry();
        mainController = registry.createMatch();
        conn1 = new TestConnection();
        conn2 = new TestConnection();
    }

    @Test
    void joinOrReconnectFirstPlayerBecomesHost() {
        ClientHandler handler = new ClientHandler(conn1, registry, "PlayerOne");
        JoinResult result = mainController.joinOrReconnect("PlayerOne", handler);

        assertTrue(result.isHost());
        assertEquals(mainController.getMatchId(), result.matchId());
        assertTrue(conn1.receivedThisMessageType(LobbyUpdateMessage.class));
    }

    @Test
    void joinOrReconnectSecondPlayerNotHost() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "PlayerOne");
        mainController.joinOrReconnect("PlayerOne", h1);

        ClientHandler h2 = new ClientHandler(conn2, registry, "PlayerTwo");
        JoinResult result = mainController.joinOrReconnect("PlayerTwo", h2);

        assertFalse(result.isHost());
    }

    @Test
    void joinOrReconnectDuplicateNicknameInLobbyThrows() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "PlayerOne");
        mainController.joinOrReconnect("PlayerOne", h1);

        ClientHandler h2 = new ClientHandler(conn2, registry, "PlayerOne");
        assertThrows(IllegalStateException.class,
                () -> mainController.joinOrReconnect("PlayerOne", h2));
    }

    @Test
    void joinOrReconnectLobbyFull() {
        for (int i = 0; i < MainController.MAX_PLAYERS; i++) {
            TestConnection c = new TestConnection();
            ClientHandler h = new ClientHandler(c, registry, "P" + i);
            mainController.joinOrReconnect("P" + i, h);
        }

        TestConnection extra = new TestConnection();
        ClientHandler extraH = new ClientHandler(extra, registry, "Extra");
        assertThrows(IllegalStateException.class,
                () -> mainController.joinOrReconnect("Extra", extraH));
    }

    @Test
    void setExpectedPlayersOnlyHost() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "Host");
        JoinResult r1 = mainController.joinOrReconnect("Host", h1);

        ClientHandler h2 = new ClientHandler(conn2, registry, "Guest");
        JoinResult r2 = mainController.joinOrReconnect("Guest", h2);

        mainController.setExpectedPlayers(r2.playerId(), 3);
        assertEquals(MatchStatus.LOBBY, mainController.getStatus());

        mainController.setExpectedPlayers(r1.playerId(), 3);
        assertEquals(MatchStatus.LOBBY, mainController.getStatus());
    }

    @Test
    void autoStartWhenExpectedPlayersReached() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "Host");
        JoinResult r1 = mainController.joinOrReconnect("Host", h1);

        ClientHandler h2 = new ClientHandler(conn2, registry, "Guest");
        mainController.joinOrReconnect("Guest", h2);

        mainController.setExpectedPlayers(r1.playerId(), 2);
        assertEquals(MatchStatus.IN_GAME, mainController.getStatus());
    }

    @Test
    void requestStartGameNotEnoughPlayers() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "Alone");
        mainController.joinOrReconnect("Alone", h1);

        mainController.requestStartGame();
        assertEquals(MatchStatus.LOBBY, mainController.getStatus());
    }

    @Test
    void requestStartGameEnoughPlayers() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "P1");
        mainController.joinOrReconnect("P1", h1);

        ClientHandler h2 = new ClientHandler(conn2, registry, "P2");
        mainController.joinOrReconnect("P2", h2);

        mainController.requestStartGame();
        assertEquals(MatchStatus.IN_GAME, mainController.getStatus());
    }

    @Test
    void unregisterClientInLobbyTransfersHost() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "Host");
        JoinResult r1 = mainController.joinOrReconnect("Host", h1);

        ClientHandler h2 = new ClientHandler(conn2, registry, "Next");
        JoinResult r2 = mainController.joinOrReconnect("Next", h2);

        assertTrue(mainController.isHost(r1.playerId()));
        mainController.unregisterClient(h1);
        assertTrue(mainController.isHost(r2.playerId()));
    }

    @Test
    void dispatchActionNoGameInProgress() {
        ClientHandler h1 = new ClientHandler(conn1, registry, "P1");
        JoinResult r = mainController.joinOrReconnect("P1", h1);

        GeneralAction action = new GeneralAction(r.playerId());
        mainController.dispatchAction(action);

        assertTrue(conn1.receivedErrorMessage("No game in progress."));
    }

    @Test
    void matchRegistryListOpenMatches() {
        assertEquals(1, registry.listOpenMatches().size());

        MainController second = registry.createMatch();
        assertEquals(2, registry.listOpenMatches().size());
    }

    @Test
    void matchRegistryRemoveMatch() {
        String id = mainController.getMatchId();
        registry.removeMatch(id);
        assertNull(registry.getMatch(id));
        assertEquals(0, registry.listOpenMatches().size());
    }

    private static class TestConnection implements ClientConnection {
        private boolean connected = true;
        private final List<Object> sentMessages = new ArrayList<>();

        @Override
        public void sendMessage(Object message) {
            sentMessages.add(message);
        }

        @Override
        public Object receiveMessage() throws Exception {
            Thread.sleep(100000);
            return null;
        }

        @Override
        public void close() {
            connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        public boolean receivedErrorMessage(String expectedText) {
            return sentMessages.stream()
                    .filter(msg -> msg instanceof ErrorMessage)
                    .map(msg -> (ErrorMessage) msg)
                    .anyMatch(err -> err.getError().equals(expectedText));
        }

        public boolean receivedThisMessageType(Class<?> type) {
            return sentMessages.stream().anyMatch(type::isInstance);
        }
    }

    private static class GeneralAction implements PlayerAction {
        private final int playerId;

        public GeneralAction(int playerId) {
            this.playerId = playerId;
        }

        @Override
        public int getPlayerId() {
            return playerId;
        }
    }
}
