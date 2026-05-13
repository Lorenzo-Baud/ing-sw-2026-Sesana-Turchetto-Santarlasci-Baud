package it.polimi.ingws.gc8.Network.Connection;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Controller.actions.PlayerAction;
import it.polimi.ingws.gc8.Controller.events.GameEventListener;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.RequestStartGameCommand;
import it.polimi.ingws.gc8.Network.Connection.Socket.commands.SetExpectedPlayersCommand;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;

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

    /**
     * Reads messages from the client and dispatches them to the
     * {@link MainController}.
     *
     * <p>On RMI, lobby commands (login, setExpectedPlayers, requestStartGame)
     * are invoked through dedicated {@code RemoteServer} methods, so this loop
     * only ever sees {@link PlayerAction}s. On TCP, lobby commands after
     * login travel in-band as serialized record commands
     * ({@link SetExpectedPlayersCommand}, {@link RequestStartGameCommand}):
     * this loop dispatches them too.</p>
     */
    @Override
    public void run() {
        try {
            while (connection.isConnected()) {
                Object message = connection.receiveMessage();

                if (message instanceof PlayerAction action) {
                    // Security: a client can only send actions for their own playerId
                    if (action.getPlayerId() != this.playerId) {
                        sendError("Cannot submit actions for another player.");
                        continue;
                    }
                    mainController.dispatchAction(action);

                } else if (message instanceof SetExpectedPlayersCommand cmd) {
                    mainController.setExpectedPlayers(this.playerId, cmd.n());

                } else if (message instanceof RequestStartGameCommand) {
                    mainController.requestStartGame();

                } else if (message != null) {
                    sendError("Unsupported message: " + message.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            // Connection lost or protocol error
        } finally {
            connection.close();
            mainController.unregisterClient(this);
        }
    }

    @Override
    public void onGameUpdate(NetworkMessage updateMessage) {
        // Se la connessione è attiva, spediamo il messaggio (es. un GameDTO) al client.
        if (connection.isConnected()) {
            connection.sendMessage(updateMessage);
        }
    }

    //Helpers

    public void sendError(String message) {
        connection.sendMessage(new ErrorMessage("GENERIC", message));
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
