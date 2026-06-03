package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Client.ClientController;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.View.TUI.Printer;
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

import static java.lang.Thread.sleep;

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

    public volatile long lastErrorMessageAt = 0;

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

    /**
     * Shows start screen
     */
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

    /**
     * Shows lobby screen
     */
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

    /**
     * Shows game screen
     */
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
     * @param title title of the error dialog
     * @param content content of the error dialog
     */
    public void showError(String title, String content) {
        if (Platform.isFxApplicationThread()) {
            showErrorOnFxThread(title, content);
        } else {
            Platform.runLater(() -> showErrorOnFxThread(title, content));
        }
    }

    /**
     * Actually shows error dialog on the FX application thread. Not to be called directly from outside this class.
     * @param title title of the error dialog
     * @param content content of the error dialog
     */
    private void showErrorOnFxThread(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows an informational alert. Safe to call from any thread: if not on the
     * FX application thread, the dialog is scheduled via {@link Platform#runLater}.
     * @param content content of the info dialog
     * @param title title of the info dialog
     */
    public void showInfo(String title, String content) {
        if (Platform.isFxApplicationThread()) {
            showInfoOnFxThread(title, content);
        } else {
            Platform.runLater(() -> showInfoOnFxThread(title, content));
        }
    }

    /***
     * Actually shows info dialog on the FX application thread
     * @param content content of the info dialog
     * @param title title of the info dialog
     */
    private void showInfoOnFxThread(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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


    /** Hard cap on how long we wait for the server to acknowledge an action. */
    private static final long SEND_TIMEOUT_MS = 3000;

    /**
     * Implementation of method sendAndAwaitUpdate described in the class-level comment.
     */
    public SendResult sendAndAwaitUpdate(Object action) {

        notifyAllListeners(action);

        long errorBefore = lastErrorMessageAt;

        // Snapshots taken AFTER notify so that errors from earlier actions
        // (already enqueued before this call) do not look "fresh" to us.
        GameDTO before = gameView;
        long deadline = System.currentTimeMillis() + SEND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (gameView != before) return SendResult.OK;
            if (lastErrorMessageAt != errorBefore) return SendResult.ERROR;
            try {
                sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return SendResult.ERROR;
            }
        }
        showError("Timeout", "The server did not respond within " + (SEND_TIMEOUT_MS / 1000) + " seconds. ");
        return SendResult.TIMEOUT;
    }


}
