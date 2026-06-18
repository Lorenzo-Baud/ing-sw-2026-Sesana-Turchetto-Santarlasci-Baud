package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Client.ClientController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the access (login) screen.
 *
 * <p>Collects the connection parameters, opens the transport, starts the
 * client controller and then hands off to the dedicated match-selection
 * screen (FA1 multi-match, FA2/FA3 reconnection). The actual match
 * create/join/reconnect logic lives in {@link MatchSelectionController}.
 */
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

    @FXML
    private void handleStartGame() {

        String address = serverAddressField.getText();
        String port = serverPortField.getText();
        String selectedType = connectionTypeChoice.getValue();
        String username = usernameField.getText();

        if (username == null || username.isEmpty() || username.length() > 12) {
            guiView.showError(
                    "Username non valido",
                    "Errore: il nome utente deve essere compreso tra 1 e 12 caratteri.");
            return;
        }

        ServerConnection connection = switch (selectedType) {
            case "RMI" -> new RMIServerConnection();
            case "Socket" -> new SocketServerConnection();
            default -> throw new IllegalArgumentException("Unknown transport: " + selectedType);
        };
        guiView.setConnection(connection);
        guiView.setController(new ClientController(connection, guiView));

        try {
            connection.connect(address, Integer.parseInt(port));
        } catch (Exception ce) {
            guiView.showError(
                    "Errore di connessione",
                    "Impossibile connettersi al server.\nVerificare indirizzo e porta.");
            return;
        }

        guiView.getController().start();
        guiView.setNickname(username);

        // FA1 (multi-match) + FA2/FA3 (reconnection) are handled on the
        // dedicated, themed match-selection screen.
        guiView.showMatchSelection();
    }
}
