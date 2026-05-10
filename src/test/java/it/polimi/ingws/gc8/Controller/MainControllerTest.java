package it.polimi.ingws.gc8.Controller;

import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Network.Connection.ClientConnection;
import it.polimi.ingws.gc8.Network.Connection.ClientHandler;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {

    private MainController mainController;

    private TestConnection conn1;
    private TestConnection conn2;
    private TestConnection conn3;

    private ClientHandler client1;
    private ClientHandler client2;


    @BeforeEach
    void setUp() {
        mainController = new MainController();

        conn1 = new TestConnection();
        conn2 = new TestConnection();
        conn3 = new TestConnection();

        client1 = new ClientHandler(conn1, mainController, 1, "PlayerOne");
        client2 = new ClientHandler(conn2, mainController, 2, "PlayerTwo");
    }

    @Test
    void registerClientSuccessAndHostAssignmentTesting() {
        mainController.registerClient(client1);

        assertEquals(1, mainController.getLobbySize());
        assertTrue(mainController.isHost(1));
        assertFalse(mainController.isHost(2));
        assertTrue(conn1.receivedThisMessageType(LobbyUpdateMessage.class));
    }

    @Test
    void registerClientGameAlreadyInProgressTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);
        mainController.requestStartGame();

        TestConnection lateConn = new TestConnection();
        ClientHandler lateClient = new ClientHandler(lateConn, mainController, 4, "LatePlayer");

        mainController.registerClient(lateClient);

        assertEquals(2, mainController.getLobbySize());
    }


    @Test
    void unregisterClientNotInGameTransfersHostTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);

        assertTrue(mainController.isHost(1));

        mainController.unregisterClient(client1);

        assertEquals(1, mainController.getLobbySize());
        assertFalse(mainController.isHost(1));
        assertTrue(mainController.isHost(2));
    }

    @Test
    void unregisterClientNotInGameEmptyLobbyTest() {
        mainController.registerClient(client1);
        mainController.unregisterClient(client1);

        assertEquals(0, mainController.getLobbySize());
        assertFalse(mainController.isHost(1));
    }

    @Test
    void unregisterClient_GameInProgressTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);
        mainController.requestStartGame();

        mainController.unregisterClient(client1);

        assertTrue(mainController.isGameInProgress());
    }

    @Test
    void setExpectedPlayersInvalidConditionsTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);

        mainController.setExpectedPlayers(2, 3);
        assertEquals(0, mainController.getExpectedPlayers());

        mainController.setExpectedPlayers(1, 1);
        assertEquals(0, mainController.getExpectedPlayers());

        mainController.setExpectedPlayers(1, 6);
        assertEquals(0, mainController.getExpectedPlayers());
    }

    @Test
    void setExpectedPlayersAutoStartGameTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);

        mainController.setExpectedPlayers(1, 2); // Host (1) sets expected players to 2

        assertEquals(2, mainController.getExpectedPlayers());
        assertTrue(mainController.isGameInProgress()); // Game should auto-start
    }

    @Test
    void setExpectedPlayersGameAlreadyInProgressTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);
        mainController.requestStartGame();

        mainController.setExpectedPlayers(1, 3); // Attempting to change mid-game

        assertEquals(0, mainController.getExpectedPlayers()); // Should remain unchanged
    }

    @Test
    void requestStartGameNotEnoughPlayersTesting() {
        mainController.registerClient(client1); // Only 1 player
        mainController.requestStartGame();

        assertFalse(mainController.isGameInProgress()); // Start aborted
    }

    @Test
    void dispatchActionSuccessTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);
        mainController.requestStartGame();

        GeneralAction action = new GeneralAction(1);
        try {
            mainController.dispatchAction(action);
        } catch (Exception ignored) { }

        assertFalse(conn1.receivedErrorMessage("No game in progress."));
    }

    @Test
    void reconnectionGameInProgressTesting() {
        mainController.registerClient(client1);
        mainController.registerClient(client2);
        mainController.requestStartGame();

        TestConnection reconnectConn = new TestConnection();
        ClientHandler reconnectingClient = new ClientHandler(reconnectConn, mainController, 1, "Roberto Trani");

        mainController.handleReconnection(reconnectingClient, 1);
        assertTrue(mainController.isGameInProgress());
    }

    @Test
    void reconnectionGameNotInProgressTesting() {
        mainController.registerClient(client1);

        TestConnection reconnectConn = new TestConnection();
        ClientHandler reconnectingClient = new ClientHandler(reconnectConn, mainController, 1, "Roberto Trani");

        mainController.handleReconnection(reconnectingClient, 1);
        assertFalse(mainController.isGameInProgress());
    }

    @Test
    void assignPlayerIdTesting() {
        int firstId = mainController.assignPlayerId();
        int secondId = mainController.assignPlayerId();

        assertEquals(1, firstId);
        assertEquals(2, secondId);
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

        // Utility to check if a specific ErrorMessage was sent
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