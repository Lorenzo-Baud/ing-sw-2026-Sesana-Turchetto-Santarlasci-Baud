package it.polimi.ingws.gc8.Network;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Model.Enum.GamePhase;

/**
 * Handles the bidirectional communication with a single connected player.
 * Transport-agnostic: delegates all I/O to a {@link ClientConnection}
 * (Socket, RMI, or any future transport).
 *
 * <ul>
 *   <li>Input: reads {@link PlayerAction} objects via {@link ClientConnection#receiveMessage()},
 *       validates them, and forwards to {@link MainController}.</li>
 *   <li>Output: implements {@link GameEventListener} so it receives
 *       notifications from ViewUpdater and sends them via
 *       {@link ClientConnection#sendMessage(Object)}.</li>
 * </ul>
 */
public class ClientHandler implements Runnable, GameEventListener {

    private final ClientConnection connection;
    private final MainController mainController;
    private final int playerId;
    private final String nickname;

    public ClientHandler(ClientConnection connection, MainController mainController,
                         int playerId, String nickname) {
        this.connection = connection;
        this.mainController = mainController;
        this.playerId = playerId;
        this.nickname = nickname;
    }

    //Input loop (Runnable)

    @Override
    public void run() {
        try {
            while (connection.isConnected()) {
                Object message = connection.receiveMessage();

                // RMI close() sends a PoisonPill to unblock receiveMessage()
                if (message instanceof RMIClientConnection.PoisonPill) {
                    break;
                }

                if (message instanceof PlayerAction) {
                    PlayerAction action = (PlayerAction) message;

                    // Security: a client can only send actions for their own playerId
                    if (action.getPlayerId() != this.playerId) {
                        sendError("Cannot submit actions for another player.");
                        continue;
                    }

                    mainController.dispatchAction(action);
                }
                // TODO: handle lobby messages (e.g., start game request)
            }
        } catch (Exception e) {
            // Connection lost or protocol error
        } finally {
            connection.close();
            mainController.unregisterClient(this);
        }
    }

    @Override
    public void onGameUpdate(Object updateMessage) {
        // Se la connessione è attiva, spediamo il messaggio (es. un GameDTO) al client.
        if (connection.isConnected()) {
            connection.sendMessage(updateMessage);
        }
    }




    //Helpers

    public void sendError(String message) {
        connection.sendMessage("ERROR:GENERIC:" + message);
    }

    //Getters
    @Override
    public int getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void close() {
        connection.close();
    }
}
