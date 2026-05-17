package it.polimi.ingws.gc8.View.GUI;

import java.util.concurrent.ConcurrentLinkedQueue;
import it.polimi.ingws.gc8.View.View;
import it.polimi.ingws.gc8.Client.ClientController;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.View.TUI.Printer;
import it.polimi.ingws.gc8.View.TUI.TuiView;
import it.polimi.ingws.gc8.View.View;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GuiView extends View {

    /*
        Attributes declared public since both read and write
        access is needed in multiple controllers
     */
    public ServerConnection connection;
    public ClientController controller;

    // Reference to the main window of the application
    private Stage primaryStage;

    /**
     * Queue of UI notifications produced by the dispatcher thread that the
     * main UI thread must print. The dispatcher MUST NOT write to stdout
     * directly. Each entry is a tuple of (severity, text); the main thread drains
     * it whenever it is about to render a screen or read input.
     */
    private final ConcurrentLinkedQueue<UiNotice> pendingNotices = new ConcurrentLinkedQueue<>();

    private enum NoticeLevel { INFO, ERROR }
    private record UiNotice(NoticeLevel level, String text) {}

    // Getters/setters
    public Stage getPrimaryStage() {
        return primaryStage;
    }


    @Override
    public void run() {

        // Initialize the JavaFX Toolkit manually
        Platform.startup(() -> {
            try {

                // Create the main application window
                primaryStage = new Stage();
                primaryStage.setTitle("Mesos Game");

                // Show start screen
                showStartScreen();

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });
    }

    // Method to display the start screen, called during initialization
    public void showStartScreen() {
        // Run on the JavaFX Application Thread to avoid concurrency issues
        Platform.runLater(() -> {
            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/start_screen.fxml"));

                // Create the controller instance
                StartScreenController controller = new StartScreenController();
                // Inject a reference of this GuiView into the controller
                controller.setGuiView(this);
                // Set the controller to the loader
                loader.setController(controller);

                // Load the FXML view
                Parent root = loader.load();
                Scene scene = new Scene(root);

                primaryStage.setScene(scene);
                primaryStage.show();

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });
    }

    // Shows lobby method
    public void showLobby() {
        Platform.runLater(() -> {
            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/lobby_screen.fxml"));

                // Create the controller instance
                LobbyScreenController controller = new LobbyScreenController();
                // Inject a reference of this GuiView into the controller
                controller.setGuiView(this);
                // Set the controller to the loader
                loader.setController(controller);

                // Load the FXML view
                Parent root = loader.load();
                Scene scene = new Scene(root);

                primaryStage.setScene(scene);

            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }

        });
    }

    /**
     * Show an alert dialog on the GUI
     * @param title title of the dialog
     * @param content content of the dialog
     */
    public void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /** Called by the UI thread before any render / prompt. */
    public void drainPendingNotices() {
        UiNotice n;
        while ((n = pendingNotices.poll()) != null) {
            System.out.println("nuovo messaggio!");
            switch (n.level()) {
                case INFO  -> Printer.printInfo(n.text());
                case ERROR -> Printer.printError(n.text());
            }
        }
    }

}