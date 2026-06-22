package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Network.Connection.ServerConnection;
import it.polimi.ingws.gc8.Network.DTO.JoinResult;
import it.polimi.ingws.gc8.Network.DTO.MatchInfo;
import it.polimi.ingws.gc8.Network.DTO.MatchStatus;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * JavaFX controller for the match-selection screen (FA1 multi-match,
 * FA2/FA3 reconnection).
 *
 * <p>Shown after the connection is established. Offers three paths, mirroring
 * the CLI flow in {@code ClientMain}:
 * <ul>
 *   <li>join an available match by clicking it in the list;</li>
 *   <li>create a new match (name + expected player count);</li>
 *   <li>join/reconnect to a match by typing its name/id — the way back into an
 *       in-progress match after a server crash (FA2/FA3), when it may not be in
 *       the open-matches list.</li>
 * </ul>
 *
 * <p>The same {@code joinMatch(matchId, nick)} call serves both join and
 * reconnect; when {@link JoinResult#reconnected()} is true the lobby setup
 * (player count) is skipped and the full snapshot from the server drives the
 * next screen.
 */
public class MatchSelectionController {

    private GuiView guiView;

    @FXML private VBox matchesList;
    @FXML private TextField newMatchNameField;
    @FXML private ChoiceBox<Integer> playerCountChoice;
    @FXML private TextField joinByNameField;

    /** Injected by {@link GuiView} before {@code load()}. */
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    @FXML
    public void initialize() {
        guiView.getPrimaryStage().setTitle("Mesos Game - Partite");
        playerCountChoice.getItems().setAll(2, 3, 4, 5);
        playerCountChoice.setValue(2);
        refreshMatches();
    }

    /** Loads the list of open matches from the server and renders it. */
    @FXML
    private void handleRefresh() {
        refreshMatches();
    }

    private void refreshMatches() {
        matchesList.getChildren().clear();
        try {
            List<MatchInfo> matches = connection().listMatches();
            if (matches == null || matches.isEmpty()) {
                Label empty = new Label("Nessuna partita disponibile — creane una nuova.");
                empty.getStyleClass().add("subtitle");
                matchesList.getChildren().add(empty);
                return;
            }
            for (MatchInfo m : matches) {
                matchesList.getChildren().add(buildMatchRow(m));
            }
        } catch (Exception e) {
            guiView.showError("Errore", "Impossibile recuperare la lista delle partite.\n" + e.getMessage());
        }
    }

    /** Builds one clickable row describing an available match. */
    private HBox buildMatchRow(MatchInfo m) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("match-card");
        row.setMaxWidth(Double.MAX_VALUE);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label id = new Label(m.matchId());
        id.getStyleClass().add("match-title");
        String expected = m.expectedPlayers() > 0 ? String.valueOf(m.expectedPlayers()) : "?";
        Label meta = new Label("host " + m.hostNickname() + " · " + m.currentPlayers() + "/" + expected + " giocatori");
        meta.getStyleClass().add("match-meta");
        info.getChildren().addAll(id, meta);

        Label badge = new Label(statusText(m.status()));
        badge.getStyleClass().add("match-badge");

        row.getChildren().addAll(info, badge);
        row.setOnMouseClicked(e -> joinExisting(m.matchId()));
        return row;
    }

    /** New match: create then enter the lobby as host. */
    @FXML
    private void handleCreate() {
        String name = newMatchNameField.getText() == null ? "" : newMatchNameField.getText().trim();
        if (name.isEmpty()) {
            guiView.showError("Nome non valido", "Inserisci un nome per la nuova partita.");
            return;
        }
        try {
            JoinResult jr = connection().createMatch(name, guiView.getNickname());
            finishJoin(jr, playerCountChoice.getValue());
        } catch (Exception e) {
            guiView.showError("Errore", "Impossibile creare la partita.\n" + e.getMessage());
        }
    }

    /** Join / reconnect to a match by typed name (FA2/FA3 path). */
    @FXML
    private void handleJoinByName() {
        String id = joinByNameField.getText() == null ? "" : joinByNameField.getText().trim();
        if (id.isEmpty()) {
            guiView.showError("Nome non valido", "Inserisci il nome della partita a cui unirti.");
            return;
        }
        joinExisting(id);
    }

    /** Joins (or reconnects to) an existing match by its id. */
    private void joinExisting(String matchId) {
        try {
            JoinResult jr = connection().joinMatch(matchId, guiView.getNickname());
            finishJoin(jr, null);
        } catch (Exception e) {
            guiView.showError("Errore", "Impossibile unirsi alla partita.\n" + e.getMessage());
        }
    }

    /**
     * Shared post-join step: records the per-match player id, configures the
     * expected player count when this client is a fresh host, then opens the
     * lobby. On reconnection lobby setup is skipped.
     *
     * @param jr            the join/reconnect result
     * @param chosenCount   selected player count for a new match, or {@code null}
     */
    private void finishJoin(JoinResult jr, Integer chosenCount) throws Exception {
        guiView.setPlayerId(jr.playerId());
        if (jr.isHost() && !jr.reconnected()) {
            int n = chosenCount != null ? chosenCount : 2;
            connection().setExpectedPlayers(n);
        }
        guiView.showLobby();
    }

    private ServerConnection connection() {
        return guiView.getConnection();
    }

    private static String statusText(MatchStatus status) {
        if (status == null) return "";
        return switch (status) {
            case LOBBY -> "in attesa";
            case IN_GAME -> "in corso";
            case FINISHED -> "conclusa";
        };
    }
}
