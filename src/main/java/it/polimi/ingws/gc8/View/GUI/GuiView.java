package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Client.ClientController;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.View.View;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owner of the JavaFX primary stage and of the per-session network objects
 * ({@code ServerConnection} + {@code ClientController}). Acts as the single
 * entry point that screen controllers use to access network/state.
 *
 * <p>Threading: every scene switch is wrapped in {@link Platform#runLater};
 * the same applies to {@link #showError(String, String)} which is safe to
 * call from any thread.
 */
public class GuiView extends View {

    private static final Logger LOGGER = Logger.getLogger(GuiView.class.getName());

    /** Network connection to the server (RMI or Socket). Set by StartScreenController. */
    private ServerConnection connection;

    /** Client-side controller that pumps network messages into View.messages. */
    private ClientController controller;

    /** Reference to the main window of the application. */
    private Stage primaryStage;

    // ------------------------------------------------------------------
    // Accessors for session-scoped objects
    // ------------------------------------------------------------------

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public ServerConnection getConnection() {
        return connection;
    }

    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    public ClientController getController() {
        return controller;
    }

    public void setController(ClientController controller) {
        this.controller = controller;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void run() {
        // Initialize the JavaFX Toolkit manually (we are not using Application.launch).
        Platform.startup(() -> {
            try {
                primaryStage = new Stage();
                primaryStage.setTitle("Mesos Game");
                showStartScreen();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to start GUI", e);
            }
        });
    }

    // ------------------------------------------------------------------
    // Scene transitions — all wrapped in Platform.runLater to be safe
    // from any caller thread.
    // ------------------------------------------------------------------

    public void showStartScreen() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/start_screen.fxml"));
                StartScreenController ctrl = new StartScreenController();
                ctrl.setGuiView(this);
                loader.setController(ctrl);
                primaryStage.setScene(new Scene(loader.load()));
                primaryStage.show();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load start_screen.fxml", e);
            }
        });
    }

    public void showLobby() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/lobby_screen.fxml"));
                LobbyScreenController ctrl = new LobbyScreenController();
                ctrl.setGuiView(this);
                loader.setController(ctrl);
                primaryStage.setScene(new Scene(loader.load()));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load lobby_screen.fxml", e);
            }
        });
    }

    public void showGame() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/game_screen.fxml"));
                GameScreenController ctrl = new GameScreenController();
                ctrl.setGuiView(this);
                loader.setController(ctrl);
                primaryStage.setScene(new Scene(loader.load()));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load game_screen.fxml", e);
            }
        });
    }

    // ------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------

    /**
     * Shows an error alert. Safe to call from any thread: if not on the FX
     * application thread, the dialog is scheduled via {@link Platform#runLater}.
     */
    public void showError(String title, String content) {
        if (Platform.isFxApplicationThread()) {
            showErrorOnFxThread(title, content);
        } else {
            Platform.runLater(() -> showErrorOnFxThread(title, content));
        }
    }

    private void showErrorOnFxThread(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ------------------------------------------------------------------
    // Queue access — re-exposed protected so that screen controllers in
    // this same package can consume View.messages.
    // ------------------------------------------------------------------

    /**
     * Waits (blocking) for the next message from the server.
     * Interruptible: returns {@code null} if the thread is interrupted.
     */
    @Override
    protected NetworkMessage waitForMessage() {
        return super.waitForMessage();
    }
}
