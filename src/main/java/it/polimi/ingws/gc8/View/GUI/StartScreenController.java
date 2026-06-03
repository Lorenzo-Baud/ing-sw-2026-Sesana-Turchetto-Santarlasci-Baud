package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Client.ClientController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServerConnection;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

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

            JoinResult joinResult = handleMatchSelection(connection, username);
            if (joinResult == null) return;

            guiView.setPlayerId(joinResult.playerId());

            if (joinResult.isHost()) {
                int n = -1;
                n = askNumberOfPlayers();
                if(n == -1) {
                    return;
                }
                connection.setExpectedPlayers(n);
            }

            guiView.showLobby();

        } catch (Exception e) {
            guiView.showError("Errore", e.getMessage());
        }
    }

    private JoinResult handleMatchSelection(ServerConnection connection, String nickname) throws Exception {
        List<MatchInfo> matches = connection.listMatches();

        if (matches.isEmpty()) {
            return connection.createMatch(nickname);
        }

        List<String> choices = new java.util.ArrayList<>();
        choices.add("Nuova partita");
        for (MatchInfo m : matches) {
            choices.add(m.matchId() + " [" + m.hostNickname()
                    + ", " + m.currentPlayers() + "/"
                    + (m.expectedPlayers() > 0 ? m.expectedPlayers() : "?") + "]");
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Selezione partita");
        dialog.setHeaderText("Scegli una partita o creane una nuova");
        dialog.setContentText("Partita:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return null;

        String selected = result.get();
        if (selected.equals("Nuova partita")) {
            return connection.createMatch(nickname);
        }

        int idx = choices.indexOf(selected) - 1;
        return connection.joinMatch(matches.get(idx).matchId(), nickname);
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