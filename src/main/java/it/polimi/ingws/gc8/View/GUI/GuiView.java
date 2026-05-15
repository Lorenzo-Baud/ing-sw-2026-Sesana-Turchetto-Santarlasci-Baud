package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Client.Bootstrap;
import it.polimi.ingws.gc8.Client.ClientController;
import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServerConnection;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;
import it.polimi.ingws.gc8.View.TUI.Printer;
import it.polimi.ingws.gc8.View.View;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GuiView extends View {

    ServerConnection connection;
    ClientController controller;

    @FXML
    private TextField usernameField;
    @FXML
    private TextField serverAddressField;
    @FXML
    private TextField serverPortField;
    @FXML
    private ChoiceBox<String> connectionTypeChoice;

    @Override
    public void run() {

        // Initialize the JavaFX Toolkit manually
        javafx.application.Platform.startup(() -> {
            try {
                Stage primaryStage = new Stage();

                // Load the FXML file from the resources folder
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/start_screen.fxml"));
                loader.setController(this);

                Parent root = loader.load();

                Scene scene = new Scene(root);

                // Configure and display the primary stage
                primaryStage.setTitle("Mesos Game - Login");
                primaryStage.setScene(scene);
                primaryStage.show();

                // Initializes connection choice box
                connectionTypeChoice.getItems().addAll("Socket", "RMI");
                connectionTypeChoice.setValue("Socket");

            } catch (Exception e) {
                System.out.println("Failed to load the GUI: " + e.getMessage());
            }
        });
    }

    /**
     * Show an alert dialog on the GUI
     * @param title title of the dialog
     * @param content content of the dialog
     */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null); // Rimuove l'header per un look più pulito
        alert.setContentText(content);
        alert.showAndWait();
    }


    /**
     * Asks the first player how many participants the match should have.
     * Called only when the server reports {@code isFirst == true}.
     * @return the chosen number of players
     */
    private int askNumberOfPlayers() {
        // Define the available options
        List<Integer> choices = Arrays.asList(2, 3, 4, 5);

        // Create the dialog with a default selection
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(2, choices);
        dialog.setTitle("Configurazione del gioco");
        dialog.setHeaderText("Imposta il numero di giocatori");
        dialog.setContentText("Numero di giocatori:");

        // Display the dialog and capture the response
        Optional<Integer> result = dialog.showAndWait();

        return result.orElse(-1);

    }

    /**
     * This method will be called when the "Start Game" button is clicked in the FXML
     * It sets up the connection and the controller, then tries to connect to the server and start the game
     */
    @FXML
    private void handleStartGame() {

        String address = serverAddressField.getText();
        String port = serverPortField.getText();
        String selectedType = connectionTypeChoice.getValue();
        String username = usernameField.getText();

        // Username validation
        if(username == null || username.isEmpty() || username.length() > 12) {
            showError(
                    "Username non valido",
                    "Errore: il nome utente deve essere compreso tra 1 e 12 caratteri."
            );
            return;
        }

        // Initializes connection object
        switch (selectedType) {
            case "RMI" -> connection = new RMIServerConnection();
            case "Socket" -> connection = new SocketServerConnection();
            default -> throw new IllegalArgumentException("Unknown transport: " + selectedType);
        }

        // Initializes controller
        controller = new ClientController(connection, this);

        // Connection to the server
        try {
            try {
                connection.connect(address, Integer.parseInt(port));
            } catch (Exception ce) {
                showError(
                        "Errore di connessione",
                        "Errore: impossibile connettersi al server.\n" +
                                "Assicurarsi di aver inserito indirizzo e porta corretti");
                Printer.printError(ce.getMessage());
                return;
            }

            Printer.printSuccess("[Client] Connected to "
                    + address + ":" + port);

            // Start the network pump BEFORE login: it must be active when
            // the server starts pushing lobby callbacks to us.
            controller.start();

            this.setNickname(username);
            LoginResult login = connection.login(username);
            this.setPlayerId(login.playerId());

            if (login.isFirst()) {
                int n = -1;
                while (n == -1) {
                    n = askNumberOfPlayers();
                }
                connection.setExpectedPlayers(n);
            } else {

                // Redirects to lobby

            }

        } catch (Exception e) {
            showError("Errore di connessione", e.getMessage());
        } finally {
            controller.stop();
            connection.disconnect();
        }

    }

}