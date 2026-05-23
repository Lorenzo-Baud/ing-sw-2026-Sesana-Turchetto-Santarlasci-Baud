package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Network.Messages.ConnectionUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.LobbyUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ModelUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.logging.Logger;

/**
 * JavaFX controller for the lobby screen.
 *
 * <p>Runs the first GUI dispatcher of the session: it consumes lobby updates
 * from {@code View.messages} and, on the first {@link ModelUpdateMessage},
 * transitions to the game screen. The dispatcher returns only after handing
 * the queue over to {@code GameScreenController} (which starts its own
 * dispatcher in its {@code initialize()}).
 */
public class LobbyScreenController {

    private static final Logger LOGGER = Logger.getLogger(LobbyScreenController.class.getName());

    @FXML
    private Label connectedPlayersLabel;

    /** Reference to the main view manager to coordinate scene switches. */
    private GuiView guiView;

    /** Setter used by GuiView to inject itself after loading the FXML. */
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    @FXML
    public void initialize() {
        guiView.getPrimaryStage().setTitle("Mesos Game - Attesa giocatori");
        startDispatcher();
    }

    /**
     * Starts the lobby dispatcher thread. The thread is a daemon and exits
     * as soon as the lobby phase ends (first ModelUpdateMessage or fatal
     * error); the game screen controller will own the queue from then on.
     */
    private void startDispatcher() {
        Thread updateDispatcher = new Thread(this::dispatchLoop, "gui-dispatcher-lobby");
        updateDispatcher.setDaemon(true);
        updateDispatcher.start();
    }

    private void dispatchLoop() {
        while (true) {
            NetworkMessage msg = guiView.waitForMessage();
            switch (msg) {
                case null -> {
                    // Interrupted: nothing to do, let the thread die quietly.
                    return;
                }
                case ModelUpdateMessage m -> {
                    // First game snapshot: store it and hand off to the game screen.
                    // GameScreenController.initialize() will start its own dispatcher.
                    guiView.setGameView(m.getGameModelDTO());
                    guiView.showGame();
                    return;
                }
                case LobbyUpdateMessage m -> Platform.runLater(() ->
                        connectedPlayersLabel.setText(
                                "Giocatori correnti: " + m.getCurrentPlayers()
                                        + "/" + m.getExpectedPlayers()));
                case ErrorMessage m -> {
                    guiView.showError("Errore", m.getError());
                    return;
                }
                case ConnectionUpdateMessage m -> {
                    String who = m.getNickname() == null ? "?" : m.getNickname();
                    LOGGER.fine(() -> who + (m.isConnected() ? " reconnected." : " disconnected."));
                }
                default -> {
                    // Ignore other message types in the lobby screen.
                }
            }
        }
    }
}
