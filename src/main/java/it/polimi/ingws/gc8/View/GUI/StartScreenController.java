package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Client.ClientController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServerConnection;
import it.polimi.ingws.gc8.Network.DTO.LoginResult;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextField;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class StartScreenController {

    // Reference to the main view manager to coordinate scene switches
    private GuiView guiView;

    @FXML
    private TextField usernameField;
    @FXML
    private TextField serverAddressField;
    @FXML
    private TextField serverPortField;
    @FXML
    private ChoiceBox<String> connectionTypeChoice;

    // Setter used by GuiView to inject itself after loading the FXML
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    @FXML
    public void initialize() {

        // Initializes connection choice box
        connectionTypeChoice.getItems().addAll("Socket", "RMI");
        connectionTypeChoice.setValue("Socket");

        // Changes primary stage title
        guiView.getPrimaryStage().setTitle("Mesos Game - Login");

        // Listen for changes in the choice box selection to update the port automatically
        connectionTypeChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.equals("RMI")) {
                    serverPortField.setText("1099");
                } else if (newValue.equals("Socket")) {
                    serverPortField.setText("1100");
                }
            }
        });

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
            guiView.showError(
                    "Username non valido",
                    "Errore: il nome utente deve essere compreso tra 1 e 12 caratteri."
            );
            return;
        }

        // Initializes connection object
        ServerConnection connection = switch (selectedType) {
            case "RMI" -> new RMIServerConnection();
            case "Socket" -> new SocketServerConnection();
            default -> throw new IllegalArgumentException("Unknown transport: " + selectedType);
        };
        guiView.setConnection(connection);

        // Initializes controller
        guiView.setController(new ClientController(connection, guiView));

        // Connection to the server
        try {
            try {
                connection.connect(address, Integer.parseInt(port));
            } catch (Exception ce) {
                guiView.showError(
                        "Errore di connessione",
                        "Errore: impossibile connettersi al server.\n" +
                                "Assicurarsi di aver inserito indirizzo e porta corretti");
                return;
            }

            // Start the network pump BEFORE login: it must be active when
            // the server starts pushing lobby callbacks to us.
            guiView.getController().start();

            guiView.setNickname(username);
            LoginResult login = connection.login(username);
            guiView.setPlayerId(login.playerId());

            // If the player is the host asks for number of players
            if (login.isFirst()) {
                int n = -1;
                while (n == -1) {
                    n = askNumberOfPlayers();
                }
                connection.setExpectedPlayers(n);
            }

            guiView.showLobby();

        } catch (Exception e) {
            guiView.showError("Errore di connessione", e.getMessage());
        }

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

}