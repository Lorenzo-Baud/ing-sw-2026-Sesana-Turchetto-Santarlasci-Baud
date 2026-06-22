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
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
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
        ServerConnection previous = this.connection;
        if (previous != null && previous != connection) {
            try { previous.disconnect(); } catch (Exception ignored) {}
        }
        this.connection = connection;
    }

    public ClientController getController() {
        return controller;
    }

    public void setController(ClientController controller) {
        ClientController previous = this.controller;
        if (previous != null && previous != controller) {
            // Stop the old controller first: this removes its ViewListener and
            // network pump. Skipping it leaks a listener across a "back to start"
            // + reconnect, so every action is sent twice — once on a stale
            // (disconnected) connection that then reports "Not connected to
            // server" on each action.
            try { previous.stop(); } catch (Exception ignored) {}
        }
        this.controller = controller;
    }

    /**
     * Ends the current network session and frees its resources: stops the
     * client controller (removing its {@link it.polimi.ingws.gc8.Client.ViewListener}
     * and network pump), closes the connection, and drops any leftover server
     * messages. Idempotent and safe to call when no session is active. Used by
     * the "back to start" / "leave match" paths so a later reconnection starts
     * from a clean state with a single active listener.
     */
    public void endSession() {
        ClientController c = controller;
        controller = null;
        if (c != null) {
            try { c.stop(); } catch (Exception ignored) {}
        }
        ServerConnection conn = connection;
        connection = null;
        if (conn != null) {
            try { conn.disconnect(); } catch (Exception ignored) {}
        }
        clearMessages();
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void run() {
        configureApplicationName();
        // Initialize the JavaFX Toolkit manually (we are not using Application.launch).
        Platform.startup(() -> {
            try {
                primaryStage = new Stage();
                primaryStage.setTitle("Mesos Game");
                configureApplicationIcon(primaryStage);
                primaryStage.setOnCloseRequest(e -> shutdownAndExit());
                showStartScreen();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to start GUI", e);
            }
        });
    }

    /**
     * Sets the desktop-visible application name before JavaFX/AWT initialise.
     * This is especially visible on macOS, where the menu bar/Dock otherwise
     * show the generic "java" process name when launched from IntelliJ.
     */
    private void configureApplicationName() {
        System.setProperty("apple.awt.application.name", "MESOS");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MESOS");
    }

    /**
     * Installs the app icon only on the OS taskbar/Dock. Do not add it to the
     * JavaFX Stage icons: on some platforms that also shows it in the window
     * title bar, which we intentionally avoid.
     */
    private void configureApplicationIcon(Stage stage) {
        try {
            if (!java.awt.Taskbar.isTaskbarSupported()) return;
            java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
            if (!taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) return;
            try (var in = getClass().getResourceAsStream(GuiAssets.APP_ICON_PATH)) {
                if (in != null) {
                    taskbar.setIconImage(javax.imageio.ImageIO.read(in));
                }
            }
        } catch (Exception e) {
            LOGGER.fine(() -> "Desktop taskbar icon not available: " + e.getMessage());
        }
    }

    /**
     * Tears down the session and terminates the JVM. Stops the network pump and
     * calls {@link ServerConnection#disconnect()}, which logs out from the server
     * (so the player is marked DISCONNECTED) and unexports the RMI callback.
     * {@link System#exit} is required because RMI keeps non-daemon threads alive,
     * so {@link Platform#exit()} alone would leave the process running headless.
     */
    public void shutdownAndExit() {
        try {
            if (controller != null) controller.stop();
        } catch (Exception ignored) {
            // Best effort: we are shutting down regardless.
        }
        try {
            if (connection != null) connection.disconnect();
        } catch (Exception ignored) {
            // Server may already be gone; closing locally is what matters.
        }
        Platform.exit();
        System.exit(0);
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
                primaryStage.sizeToScene();
                primaryStage.centerOnScreen();
                primaryStage.show();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load start_screen.fxml", e);
            }
        });
    }

    /**
     * Shows the match-selection screen (FA1 multi-match, FA2/FA3 reconnection).
     * Must be called only after the connection has been established and the
     * nickname set.
     */
    public void showMatchSelection() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/match_selection.fxml"));
                MatchSelectionController ctrl = new MatchSelectionController();
                ctrl.setGuiView(this);
                loader.setController(ctrl);
                primaryStage.setScene(new Scene(loader.load()));
                primaryStage.sizeToScene();
                primaryStage.centerOnScreen();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load match_selection.fxml", e);
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
                primaryStage.sizeToScene();
                primaryStage.centerOnScreen();
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
                primaryStage.sizeToScene();
                primaryStage.centerOnScreen();
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
        showThemedAlert(Alert.AlertType.ERROR, title, content);
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
        showThemedAlert(Alert.AlertType.INFORMATION, title, content);
    }

    /**
     * Builds and shows a modal {@link Alert} restyled to match the app's
     * dark/parchment theme. The default JavaFX (modena) icon is dropped and the
     * app stylesheet is attached to the dialog pane, so every popup routed
     * through {@link #showError}/{@link #showInfo} (suspended match, player
     * disconnected, rules, timeout, validation errors…) shares one look.
     * Error alerts get an extra {@code app-dialog-error} accent class.
     */
    private void showThemedAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initModality(Modality.APPLICATION_MODAL);
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }

        DialogPane pane = alert.getDialogPane();
        pane.setGraphic(null);
        URL css = getClass().getResource("/javafx_gui/css/theme.css");
        if (css != null) {
            pane.getStylesheets().add(css.toExternalForm());
        }
        pane.getStyleClass().add("app-dialog");
        if (type == Alert.AlertType.ERROR) {
            pane.getStyleClass().add("app-dialog-error");
        }

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
