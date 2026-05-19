package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.Messages.*;
import it.polimi.ingws.gc8.View.TUI.Printer;
import it.polimi.ingws.gc8.View.TUI.TuiView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

public class LobbyScreenController {

    @FXML
    private Label connectedPlayersLabel;

    // Reference to the main view manager to coordinate scene switches
    private GuiView guiView;

    protected final LinkedBlockingQueue<NetworkMessage> messages = new LinkedBlockingQueue<>();


    // Setter used by GuiView to inject itself after loading the FXML
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    @FXML
    public void initialize() throws InterruptedException {

        guiView.getPrimaryStage().setTitle("Mesos Game - Attesa giocatori");
        waitForLobbyStart();

    }

    /** Waits for the first game snapshot. */
    private void waitForLobbyStart() {

        Thread updateDispatcher = new Thread(this::dispatchLoop, "tui-dispatcher");
        updateDispatcher.setDaemon(true);
        updateDispatcher.start();

    }

    private void dispatchLoop() {
        while (true) {
            NetworkMessage msg = guiView.waitForMessage();
            switch (msg) {
                case null -> {
                    System.out.println("Dispatcher thread interrupted, exiting.");
                    return;
                }
                case ModelUpdateMessage m -> {
                    GameDTO previous = guiView.getGameView();
                    GameDTO next = m.getGameModelDTO();
                    guiView.setGameView(next);
                    guiView.showGame();
                    return;
                }
                case LobbyUpdateMessage m -> {
                    // Run on the JavaFX Application Thread to safely update the UI component
                    Platform.runLater(() -> {
                        connectedPlayersLabel.setText("Giocatori correnti: " + m.getCurrentPlayers() + "/" + m.getExpectedPlayers());
                    });
                }
                case ErrorMessage m -> {
                    guiView.showError("Errore", m.getError());
                    return;
                }
                case ConnectionUpdateMessage m -> {
                    String who = m.getNickname() == null ? "?" : m.getNickname();
                    String text = m.isConnected()
                            ? who + " reconnected."
                            : who + " disconnected.";
                    System.out.println(text);
                }
                default -> {
                    // Ignore other message types in the lobby screen
                }
            }

        }
    }


}
