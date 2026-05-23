package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Model.Enum.TotemColor;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.Messages.ConnectionUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.GameOverMessage;
import it.polimi.ingws.gc8.Network.Messages.ModelUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * JavaFX controller for the main game screen.
 *
 * <p>Owns the per-game dispatcher thread that drains {@code View.messages} and
 * re-renders the scene on the FX thread when a {@code ModelUpdateMessage}
 * arrives. All UI mutations are confined to the JavaFX Application Thread.
 *
 * <p>Render rule: every render reads {@code guiView.getGameView()} <b>once</b>
 * into a local snapshot and uses it for the whole frame.
 */
public class GameScreenController {

    private static final Logger LOGGER = Logger.getLogger(GameScreenController.class.getName());

    /** Reference to the owning GuiView, injected before {@code load()}. */
    private GuiView guiView;

    /** Tracks whether the previous render saw "my turn", to fire the
     * notification popup only on transition (false → true). */
    private boolean previousMyTurn = false;

    /** HBox container for the top row of card image nodes. */
    @FXML private HBox topCardsRow;
    /** HBox container for the middle row of card image nodes. */
    @FXML private HBox middleCardsRow;
    /** HBox container for the bottom row of card image nodes. */
    @FXML private HBox bottomCardsRow;

    /** Label used in lobby updates to show current/expected players. */
    @FXML private Label connectedPlayersLabel;

    /** Panes used on top bar to display players stats. */
    @FXML Pane topBarP3Box;
    @FXML Pane topBarP4Box;
    @FXML Pane topBarP5Box;

    /** Player markers on top bar. */
    @FXML Circle topBarP1Marker;
    @FXML Circle topBarP2Marker;
    @FXML Circle topBarP3Marker;
    @FXML Circle topBarP4Marker;
    @FXML Circle topBarP5Marker;
    private List<Circle> topBarPlayerMarkers;

    /** Player markers on side bar. */
    @FXML Circle leftBarTurnOrderCircle1;
    @FXML Circle leftBarTurnOrderCircle2;
    @FXML Circle leftBarTurnOrderCircle3;
    @FXML Circle leftBarTurnOrderCircle4;
    @FXML Circle leftBarTurnOrderCircle5;
    private List<Circle> leftBarTurnOrderCircles;

    /** Other labels on top bar. */
    @FXML Label topBarP1PrestigeLabel;
    @FXML Label topBarP2PrestigeLabel;
    @FXML Label topBarP3PrestigeLabel;
    @FXML Label topBarP4PrestigeLabel;
    @FXML Label topBarP5PrestigeLabel;
    private List<Label> topBarPlayerPrestigeLabels;

    @FXML Label topBarEraLabel;
    @FXML Label topBarRoundLabel;

    @FXML Label sideBarFoodLabel;
    @FXML Label sideBarPrestigeLabel;

    /** Injects the owning {@link GuiView} instance. */
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    /**
     * Runs on the JavaFX Application Thread. Performs the first render from
     * the snapshot already stored in {@code guiView.getGameView()} and starts
     * the dispatcher thread that consumes subsequent {@code NetworkMessage}s.
     */
    @FXML
    public void initialize() {
        guiView.getPrimaryStage().setTitle("Mesos Game - Gioco");

        // Bind list-of-FXML-nodes views once: they don't change after load().
        topBarPlayerMarkers = List.of(topBarP1Marker, topBarP2Marker, topBarP3Marker, topBarP4Marker, topBarP5Marker);
        topBarPlayerPrestigeLabels = List.of(topBarP1PrestigeLabel, topBarP2PrestigeLabel, topBarP3PrestigeLabel, topBarP4PrestigeLabel, topBarP5PrestigeLabel);
        leftBarTurnOrderCircles = List.of(leftBarTurnOrderCircle1, leftBarTurnOrderCircle2, leftBarTurnOrderCircle3, leftBarTurnOrderCircle4, leftBarTurnOrderCircle5);

        // First render from the existing snapshot (we're already on the FX thread).
        GameDTO snap = guiView.getGameView();
        initializeStaticInterface(snap);
        renderFromSnapshot(snap);

        // Start the per-game dispatcher: the lobby dispatcher returned right
        // after calling showGame(), so the queue would otherwise have no consumer.
        Thread updateDispatcher = new Thread(this::dispatchLoop, "gui-dispatcher-game");
        updateDispatcher.setDaemon(true);
        updateDispatcher.start();
    }

    /**
     * One-shot setup that depends on data that does not change during the game
     * (number of players, totem colors). Reads the snapshot exactly once.
     */
    private void initializeStaticInterface(GameDTO snap) {
        if (snap == null) return;
        final int n = snap.numberOfPlayers();

        // Hide top bar panes for non-existing players.
        if (n == 2) {
            topBarP3Box.setVisible(false);
            topBarP4Box.setVisible(false);
            topBarP5Box.setVisible(false);
        } else if (n == 3) {
            topBarP4Box.setVisible(false);
            topBarP5Box.setVisible(false);
        } else if (n == 4) {
            topBarP5Box.setVisible(false);
        }

        // Initial top-bar marker colors (mirror of player totem colors).
        for (int i = 0; i < n; i++) {
            applyTotemFill(topBarPlayerMarkers.get(i), snap.players().get(i).totemColor());
        }

        // Initial prestige labels (zeroed at game start).
        for (int i = 0; i < n; i++) {
            topBarPlayerPrestigeLabels.get(i).setText("P" + (i + 1) + ": 0PP");
        }
    }

    /**
     * Per-frame render. Reads {@code guiView.getGameView()} once and uses the
     * same snapshot for the whole frame.
     * Must be called on the FX thread.
     */
    private void renderFromSnapshot(GameDTO snap) {
        if (snap == null) return;
        final int n = snap.numberOfPlayers();

        // ---------- Turn-change notification ----------
        boolean myTurn = guiView.isMyTurn();
        if (myTurn && !previousMyTurn) {
            // Only fire when we transition from "not my turn" to "my turn",
            // not every update.
            guiView.showError("Il tuo turno!", "È il tuo turno, effettua la tua mossa.");
        }
        previousMyTurn = myTurn;

        // ---------- TOP BAR ----------
        for (int i = 0; i < n; i++) {
            topBarPlayerPrestigeLabels.get(i).setText(
                    "P" + (i + 1) + ": " + snap.players().get(i).prestigePoints() + "PP");
        }
        topBarEraLabel.setText("Era " + snap.currentEra());
        topBarRoundLabel.setText("Round " + snap.currentRound() + "/10");

        // ---------- LEFT BAR (turn order track) ----------
        for (int i = 0; i < n; i++) {
            applyTotemFill(leftBarTurnOrderCircles.get(i),
                    snap.turnOrderTrack().get(i).totemColor());
        }

        // ---------- CARDS ROWS ----------
        // Clear and rebuild: avoids appending duplicates at every update.
        topCardsRow.getChildren().clear();
        middleCardsRow.getChildren().clear();
        bottomCardsRow.getChildren().clear();

        for (int i = 0; i < snap.upperRowCards().size(); i++) {
            addCardToRow(0, snap.upperRowCards().get(i), i);
        }
        for (int i = 0; i < snap.lowerRowCards().size(); i++) {
            addCardToRow(2, snap.lowerRowCards().get(i), i);
        }

        // ---------- SIDE BAR ----------
        sideBarPrestigeLabel.setText(String.valueOf(snap.currentPlayer().prestigePoints()));
        sideBarFoodLabel.setText(String.valueOf(snap.currentPlayer().foodPoints()));
    }

    /** Convenience overload that reads the current snapshot. */
    private void updateInterface() {
        renderFromSnapshot(guiView.getGameView());
    }

    /** Maps a TotemColor to a JavaFX fill style and applies it to the given circle. */
    private void applyTotemFill(Circle circle, TotemColor color) {
        switch (color) {
            case RED -> circle.setStyle("-fx-fill: red;");
            case BLUE -> circle.setStyle("-fx-fill: blue;");
            case WHITE -> circle.setStyle("-fx-fill: white;");
            case YELLOW -> circle.setStyle("-fx-fill: yellow;");
            case BLACK -> circle.setStyle("-fx-fill: black;");
        }
    }

    /**
     * Continuously waits for network messages and dispatches UI updates.
     * Runs on the {@code gui-dispatcher-game} thread; all FX-thread work is
     * wrapped in {@link Platform#runLater}.
     */
    private void dispatchLoop() {
        while (true) {
            NetworkMessage msg = guiView.waitForMessage();
            switch (msg) {
                case null -> {
                    guiView.showError("Errore", "Connessione al server persa.");
                    return;
                }
                case ModelUpdateMessage m -> {
                    guiView.setGameView(m.getGameModelDTO());
                    Platform.runLater(this::updateInterface);
                }
                case GameOverMessage m -> {
                    // TODO: show game over screen
                }
                case ErrorMessage m -> guiView.showError("Errore", m.getError());
                case ConnectionUpdateMessage m -> {
                    // TODO: handle this
                }
                default -> {
                    // Ignore other message types here.
                }
            }
        }
    }

    /**
     * Adds a card image to the given row.
     *  0 = top row, 1 = middle row, 2 = bottom row.
     * Must be called on the FX thread.
     */
    private void addCardToRow(int row, CardDTO card, int index) {
        try {
            InputStream imageStream = getClass().getResourceAsStream(card.front());
            if (imageStream == null) {
                LOGGER.warning(() -> "Card image not found: " + card.front());
                return;
            }
            Image cardImage = new Image(imageStream);
            ImageView cardView = new ImageView(cardImage);
            cardView.setFitHeight(120.0);
            cardView.setPreserveRatio(true);

            cardView.setOnMouseClicked(event ->
                    LOGGER.fine(() -> "Card clicked! Row: " + row + ", Position: " + index));
            // TODO: build and send a PlayerAction here instead of logging.

            switch (row) {
                case 0 -> topCardsRow.getChildren().add(cardView);
                case 1 -> middleCardsRow.getChildren().add(cardView);
                case 2 -> bottomCardsRow.getChildren().add(cardView);
            }
        } catch (Exception e) {
            guiView.showError("Errore", e.getMessage());
        }
    }
}
