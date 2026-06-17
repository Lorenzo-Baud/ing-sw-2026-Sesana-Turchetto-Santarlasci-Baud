package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Controller.actions.*;
import it.polimi.ingws.gc8.Model.Board.OffertileId;
import it.polimi.ingws.gc8.Model.Enum.*;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.DTO.OfferTileDTO;
import it.polimi.ingws.gc8.Network.Messages.ConnectionUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.ErrorMessage;
import it.polimi.ingws.gc8.Network.Messages.EventResolvedMessage;
import it.polimi.ingws.gc8.Network.Messages.GameOverMessage;
import it.polimi.ingws.gc8.Network.Messages.ModelUpdateMessage;
import it.polimi.ingws.gc8.Network.Messages.NetworkMessage;
import it.polimi.ingws.gc8.View.View;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

    /** Variables to track how many cards have been taken from each row during the current turn, to enforce quotas. */
    int turnUpperTaken = 0;
    int turnLowerTaken = 0;
    boolean turnFoodTaken = false;

    /** Reference to the owning GuiView, injected before {@code load()}. */
    private GuiView guiView;

    /** Tracks whether the previous render saw "my turn", to fire the
     * notification popup only on transition (false → true). */
    private boolean previousMyTurn = false;

    /** Label displaying if it's player's turn */
    @FXML private Label yourTurnLabel;

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

    /** Labels for food in turn order track */
    @FXML Label turnOrderTrackLabel1;
    @FXML Label turnOrderTrackLabel2;
    @FXML Label turnOrderTrackLabel3;
    @FXML Label turnOrderTrackLabel4;
    @FXML Label turnOrderTrackLabel5;

    /** Player markers on sidebar. */
    @FXML Circle leftBarTurnOrderCircle1;
    @FXML Circle leftBarTurnOrderCircle2;
    @FXML Circle leftBarTurnOrderCircle3;
    @FXML Circle leftBarTurnOrderCircle4;
    @FXML Circle leftBarTurnOrderCircle5;
    private List<Circle> leftBarTurnOrderCircles;

    @FXML AnchorPane leftBarTurnOrderTile1;
    @FXML AnchorPane leftBarTurnOrderTile2;
    @FXML AnchorPane leftBarTurnOrderTile3;
    @FXML AnchorPane leftBarTurnOrderTile4;
    @FXML AnchorPane leftBarTurnOrderTile5;
    private List<AnchorPane> leftBarTurnOrderTiles;

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

    /** Right bar labels */
    @FXML Label artistNumberLabel;
    @FXML Label shamanNumberLabel;
    @FXML Label constructorNumberLabel;
    @FXML Label hunterNumberLabel;
    @FXML Label collectorNumberLabel;
    @FXML Label inventorNumberLabel;
    @FXML Label buildingNumberLabel;

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
        leftBarTurnOrderTiles = List.of(leftBarTurnOrderTile1, leftBarTurnOrderTile2, leftBarTurnOrderTile3,  leftBarTurnOrderTile4,  leftBarTurnOrderTile5);

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

        yourTurnLabel.setVisible(guiView.isMyTurn());

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

        // Turn order track initialization
        for (int i = guiView.getGameView().numberOfPlayers(); i < 5; i++) {
            leftBarTurnOrderCircles.get(i).setVisible(false);
            leftBarTurnOrderTiles.get(i).setVisible(false);
        }

        // Turn order left bar initialization
        switch(guiView.getGameView().players().size()) {
            case 2:
                turnOrderTrackLabel1.setText("+1\uD83C\uDF56");
                turnOrderTrackLabel2.setText("-1\uD83C\uDF56/-2PP");
                turnOrderTrackLabel1.setVisible(true);
                turnOrderTrackLabel2.setVisible(true);
                break;
            case 3:
                turnOrderTrackLabel1.setText("+2\uD83C\uDF56");
                turnOrderTrackLabel3.setText("-1\uD83C\uDF56/-2PP");
                turnOrderTrackLabel1.setVisible(true);
                turnOrderTrackLabel3.setVisible(true);
                break;
            case 4:
                turnOrderTrackLabel1.setText("+2\uD83C\uDF56");
                turnOrderTrackLabel2.setText("+1\uD83C\uDF56");
                turnOrderTrackLabel4.setText("-1\uD83C\uDF56/-2PP");
                turnOrderTrackLabel1.setVisible(true);
                turnOrderTrackLabel2.setVisible(true);
                turnOrderTrackLabel4.setVisible(true);
                break;
            case 5:
                turnOrderTrackLabel1.setText("+3\uD83C\uDF56");
                turnOrderTrackLabel2.setText("+1\uD83C\uDF56");
                turnOrderTrackLabel5.setText("-1\uD83C\uDF56/-2PP");
                turnOrderTrackLabel1.setVisible(true);
                turnOrderTrackLabel2.setVisible(true);
                turnOrderTrackLabel5.setVisible(true);
                break;
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
        // Only fire when we transition from "not my turn" to "my turn",
        // not every update.
        yourTurnLabel.setVisible(guiView.isMyTurn());

        OfferTileDTO myTile = guiView.getGameView().offerTrack().stream()
                .filter(tile -> tile.playerIndexOccupant() == guiView.getPlayerId())
                .findFirst()
                .orElse(null);

        if(myTile != null) {
            int maxUpper = View.maxUpperPicks(myTile.id());
            int maxLower = View.maxLowerPicks(myTile.id());

            // Sets turn label text
            yourTurnLabel.setText("È il tuo turno! ↓" + turnLowerTaken + "/" + maxLower + " ↑" + turnUpperTaken + "/" + maxUpper);
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
            if(snap.turnOrderTrack().get(i) != null) {
                applyTotemFill(leftBarTurnOrderCircles.get(i),
                        snap.turnOrderTrack().get(i).totemColor());
            } else {
                // Sets the filling color to transparent so the circle becomes completely invisible
                leftBarTurnOrderCircles.get(i).setStyle("-fx-fill: transparent;");
            }

        }

        // ---------- RIGHT BAR ----------
        artistNumberLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).tribe().stream()
            .filter(c -> c.type() == CardType.CHARACTER && c.characterType() == CharacterType.ARTIST)
            .count()));
        shamanNumberLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).tribe().stream()
            .filter(c -> c.type() == CardType.CHARACTER && c.characterType() == CharacterType.SHAMAN)
            .count()));
        constructorNumberLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).tribe().stream()
            .filter(c -> c.type() == CardType.CHARACTER && c.characterType() == CharacterType.BUILDER)
            .count()));
        hunterNumberLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).tribe().stream()
            .filter(c -> c.type() == CardType.CHARACTER && c.characterType() == CharacterType.HUNTER)
            .count()));
        collectorNumberLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).tribe().stream()
            .filter(c -> c.type() == CardType.CHARACTER && c.characterType() == CharacterType.COLLECTOR)
            .count()));
        inventorNumberLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).tribe().stream()
            .filter(c -> c.type() == CardType.CHARACTER && c.characterType() == CharacterType.INVENTOR)
            .count()));
        buildingNumberLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).tribe().stream()
            .filter(c -> c.type() == CardType.BUILDING)
            .count()));

        // ---------- CARDS ROWS ----------
        // Clear and rebuild: avoids appending duplicates at every update.
        topCardsRow.getChildren().clear();
        middleCardsRow.getChildren().clear();
        bottomCardsRow.getChildren().clear();

        // Upper row cards
        for (int i = 0; i < snap.upperRowCards().size(); i++) {
            addCardToRow(CardRow.UPPER, snap.upperRowCards().get(i), i);
        }

        // Offer track cards
        for(int i = 0; i < snap.offerTrack().size(); i++) {
            String circleStyle = "-fx-fill: transparent; -fx-stroke: transparent;";
            if(snap.offerTrack().get(i).playerIndexOccupant() != -1) {
                // Determine the CSS styling injection based on the player's totem color
                switch (snap.players().get(snap.offerTrack().get(i).playerIndexOccupant()-1).totemColor()) {
                    case RED -> circleStyle = "-fx-fill: red;";
                    case BLUE -> circleStyle = "-fx-fill: blue;";
                    case WHITE -> circleStyle = "-fx-fill: #f7f7f7; -fx-stroke: #d0d0d0; -fx-stroke-type: inside; -fx-stroke-width: 1px;";
                    case YELLOW -> circleStyle = "-fx-fill: yellow;";
                    case BLACK -> circleStyle = "-fx-fill: black;";
                }
            }
            addCardToOfferTrack(snap.offerTrack().get(i), circleStyle);
        }

        // Lower row cards
        for (int i = 0; i < snap.lowerRowCards().size(); i++) {
            addCardToRow(CardRow.LOWER, snap.lowerRowCards().get(i), i);
        }

        // ---------- SIDE BAR ----------
        sideBarPrestigeLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).prestigePoints()));
        sideBarFoodLabel.setText(String.valueOf(snap.players().get(guiView.getPlayerId()-1).foodPoints()));
    }

    /** Hard cap on how long we wait for the server to acknowledge an action. */
    private static final long SEND_TIMEOUT_MS = 3000;

    /** Convenience overload that reads the current snapshot. */
    private void updateInterface() {
        renderFromSnapshot(guiView.getGameView());

        /*
            If the player placed his totem on the food tile takes food and
            returns to turn order track
         */
        if(guiView.isMyTurn()) {
            OfferTileDTO myTile = guiView.getGameView().offerTrack().stream()
                    .filter(tile -> tile.playerIndexOccupant() == guiView.getPlayerId())
                    .findFirst()
                    .orElse(null);
            if(myTile != null) {
                if(myTile.id() == OffertileId.A && !turnFoodTaken) {
                    turnFoodTaken = true;
                    if (guiView.sendAndAwaitUpdate(new TakeFoodAction(guiView.getPlayerId())) == View.SendResult.OK) {
                        endTurnAndReturnTotem();
                        guiView.showInfo("Turno terminato", "Hai guadagnato 3 cibo e sei ritornato automaticamente in posizione");
                    } else {
                        turnFoodTaken = false;
                    }
                }
            }
        }

    }

    /** Maps a TotemColor to a JavaFX fill style and applies it to the given circle. */
    private void applyTotemFill(Circle circle, TotemColor color) {
        switch (color) {
            case RED -> circle.setStyle("-fx-fill: red;");
            case BLUE -> circle.setStyle("-fx-fill: blue;");
            case WHITE -> circle.setStyle("-fx-fill: #f7f7f7; -fx-stroke: #d0d0d0; -fx-stroke-type: inside; -fx-stroke-width: 1px;");
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
                case EventResolvedMessage m -> guiView.showInfo("Eventi risolti", formatResolvedEvents(m));
                case ErrorMessage m -> {
                    LOGGER.fine(() -> "Received error message from server: " + m.getError());
                    guiView.lastErrorMessageAt = System.currentTimeMillis();
                    guiView.showError("Errore", m.getError());
                }
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
     * Builds a human-readable summary of the events resolved at the end of a
     * round / era, including the per-player prestige variation each produced.
     */
    private static String formatResolvedEvents(EventResolvedMessage m) {
        StringBuilder sb = new StringBuilder();
        for (EventResolvedMessage.ResolvedEvent ev : m.getEvents()) {
            sb.append("• ").append(ev.eventType());
            if (ev.isFinal()) sb.append(" [FINAL]");
            if (ev.deltas().isEmpty()) {
                sb.append(": nessuna variazione di prestigio.");
            } else {
                sb.append(": ");
                for (int i = 0; i < ev.deltas().size(); i++) {
                    EventResolvedMessage.PlayerDelta d = ev.deltas().get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(d.nickname())
                            .append(d.prestigeDelta() >= 0 ? " +" : " ")
                            .append(d.prestigeDelta()).append("PP");
                }
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * Adds card on specific row
     * @param row row where to add the card
     * @param card card to add
     * @param index index where to add the card
     */
    private void addCardToRow(CardRow row, CardDTO card, int index) {
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

            cardView.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    handleCardClick(row, index);
                } else if (event.getButton() == MouseButton.SECONDARY) {
                    displayCardInfo(row, index);
                }
            });

            switch (row) {
                case CardRow.UPPER -> topCardsRow.getChildren().add(cardView);
                case CardRow.LOWER -> bottomCardsRow.getChildren().add(cardView);
            }

        } catch (Exception e) {
            guiView.showError("Errore", e.getMessage());
        }
    }

    /**
     * Adds card to offer track
     * @param tile card to add
     * @param circleStyle CSS style for the token circle, determined by the occupant player's totem color
     */
    private void addCardToOfferTrack(OfferTileDTO tile, String circleStyle) {
        try {
            InputStream imageStream = getClass().getResourceAsStream(tile.image());
            if (imageStream == null) {
                LOGGER.warning(() -> "Card image not found: " + tile.image());
                return;
            }

            StackPane cardContainer = new StackPane();

            Image cardImage = new Image(imageStream);
            ImageView cardView = new ImageView(cardImage);
            cardView.setFitHeight(120.0);
            cardView.setPreserveRatio(true);

            Circle tokenCircle = new Circle();
            tokenCircle.setRadius(8.0);
            tokenCircle.setStyle(circleStyle);

            StackPane.setAlignment(tokenCircle, Pos.TOP_CENTER);
            StackPane.setMargin(tokenCircle, new Insets(30.0, 0.0, 0.0, 0.0));

            cardContainer.getChildren().addAll(cardView, tokenCircle);

            cardContainer.setOnMouseClicked(event ->
                    handleOfferTrackPlacement(tile));

            middleCardsRow.getChildren().add(cardContainer);

        } catch (Exception e) {
            guiView.showError("Errore", e.getMessage());
        }
    }

    /**
     * Handles player totem placement on turn order track
     */
    private void handleOfferTrackPlacement(OfferTileDTO tile) {
        if(guiView.isMyTurn()) {

            // Create the concrete network action payload
            PlayerAction action = new PlaceTotemAction(guiView.getPlayerId(), tile.id().name());

            // Dispatch the action over the network and await the server response snapshot
            View.SendResult result = guiView.sendAndAwaitUpdate(action);

            if (result == View.SendResult.ERROR) {
                System.out.println("Placement rejected by the server rulebook.");
            }

        }
    }

    /**
     * Handles clicks on cards in the upper/lower rows. The index is needed to identify the card in the snapshot,
     * and the row is needed to determine which row of the snapshot to look at.
     * @param row row of the clicked card
     * @param index index of the clicked card inside the row
     */
    private void handleCardClick(CardRow row, int index) {

        GameDTO g = guiView.getGameView();

        // Ensure that is currently my turn
        if (!guiView.isMyTurn()) {
            return;
        }

        // Identify the specific row dataset based on the enum parameter
        List<CardDTO> targetRowCards = (row == CardRow.UPPER) ? g.upperRowCards() : g.lowerRowCards();
        if (targetRowCards == null || index < 0 || index >= targetRowCards.size()) {
            guiView.showError("Errore", "Impossibile trovare la carta.");
            return;
        }

        // Quota Validation: Fetch the player's offer tile configuration to check quotas
        OfferTileDTO myTile = guiView.getGameView().offerTrack().stream()
                .filter(tile -> tile.playerIndexOccupant() == guiView.getPlayerId())
                .findFirst()
                .orElse(null);
        if (myTile == null) {
            guiView.showError("Errore", "Impossibile recuperare la tua posizione attuale sulla pista delle offerte.");
            return;
        }

        // Enforce quota limitations based on current in-flight turn counters
        if (row == CardRow.UPPER) {
            int maxUpper = View.maxUpperPicks(myTile.id());
            if (turnUpperTaken >= maxUpper) {
                guiView.showError("Errore", "Hai già esaurito la tua quota di pescate dalla fila superiore (" + turnUpperTaken + "/" + maxUpper + ").");
                return;
            }
        } else {
            int maxLower = View.maxLowerPicks(myTile.id());
            if (turnLowerTaken >= maxLower) {
                guiView.showError("Errore", "Hai già esaurito la tua quota di pescate dalla fila inferiore (" + turnLowerTaken + "/" + maxLower + ").");
                return;
            }
        }

        if (guiView.getPlayerId() < 0) {
            guiView.showError("Errore", "Id player inconsistente.");
            return;
        }

        View.SendResult result = guiView.sendAndAwaitUpdate(new TakeCardAction(guiView.getPlayerId(), index, row));

        if (result == View.SendResult.OK) {
            // Internally update local resolution counters upon successful server verification
            if (row == CardRow.UPPER) {
                turnUpperTaken++;
            } else {
                turnLowerTaken++;
            }
            System.out.println("GUI: Card draw applied successfully. Upper: " + turnUpperTaken + ", Lower: " + turnLowerTaken);
        }

        int maxUpper = View.maxUpperPicks(myTile.id());
        int maxLower = View.maxLowerPicks(myTile.id());

        // Automatically return the totem when all possible action quotas are completely exhausted
        if (((turnLowerTaken <= maxLower && guiView.getGameView().lowerRowCards().isEmpty()) && turnUpperTaken == maxUpper) ||
            ((turnUpperTaken <= maxUpper && guiView.getGameView().upperRowCards().isEmpty()) && turnLowerTaken == maxLower) ||
            (guiView.getGameView().upperRowCards().isEmpty() && guiView.getGameView().lowerRowCards().isEmpty()) ||
            (turnUpperTaken == maxUpper && turnLowerTaken == maxLower) ) {
            System.out.println("GUI: All picks completed. Triggering automatic totem return.");
            endTurnAndReturnTotem();
        }


    }


    /**
     * Checks if all actions are completed or can be invoked manually
     * to return the totem and finalize the player's turn.
     */
    private void endTurnAndReturnTotem() {

        GameDTO g = guiView.getGameView();

        if (g == null) {
            guiView.showError("Errore", "Stato inconsistente.");
            return;
        }

        OfferTileDTO myTile = g.offerTrack().stream()
                .filter(tile -> tile.playerIndexOccupant() == guiView.getPlayerId())
                .findFirst()
                .orElse(null);
        if (myTile == null) {
            guiView.showError("Errore", "Impossibile recuperare la tua posizione attuale sulla pista delle offerte.");
            return;
        }

        // Fire the ReturnTotemAction over the network pump
        View.SendResult result = guiView.sendAndAwaitUpdate(new ReturnTotemAction(guiView.getPlayerId()));

        if (result ==View. SendResult.OK) {
            turnUpperTaken = 0;
            turnLowerTaken = 0;
            turnFoodTaken = false;
        } else {
            guiView.showError("Errore", "Impossibile restituire il totem.");
        }
    }

    @FXML
    private void handleShowTribePopupDirectly() {

        // Creates new container
        FlowPane tribeCardsContainer = new FlowPane();
        tribeCardsContainer.setHgap(12.0);
        tribeCardsContainer.setVgap(12.0);
        tribeCardsContainer.setPadding(new Insets(15.0));
        tribeCardsContainer.setPrefWrapLength(520.0);

        tribeCardsContainer.setStyle("-fx-background-color: #34495e;");

        // Populate the container with the player's current tribe card list
        for (CardDTO card : guiView.getGameView().players().get(guiView.getPlayerId()-1).tribe()) {

            InputStream imageStream = getClass().getResourceAsStream(card.front());

            if (imageStream != null) {
                StackPane cardHolder = new StackPane();

                Image cardImage = new Image(imageStream);
                ImageView cardView = new ImageView(cardImage);
                cardView.setFitHeight(140.0);
                cardView.setPreserveRatio(true);
                cardView.setSmooth(true);

                cardHolder.getChildren().add(cardView);
                tribeCardsContainer.getChildren().add(cardHolder);
            }
        }

        // Wrap the FlowPane inside a ScrollPane to support infinite cards vertical scrolling
        ScrollPane scrollPane = new ScrollPane(tribeCardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefSize(550.0, 400.0);
        scrollPane.setStyle("-fx-background: #34495e; -fx-background-color: #34495e; -fx-padding: 0; -fx-background-insets: 0;");

        // Set up the secondary Stage object structure acting as a modal window
        Stage popupStage = new Stage();
        popupStage.setTitle("La mia tribù");

        // Enforce modal layer over the primary game screen layout
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(middleCardsRow.getScene().getWindow());

        // Set the scene and display the dynamic frame to the client user
        Scene scene = new Scene(scrollPane);
        popupStage.setScene(scene);
        popupStage.setResizable(false);
        popupStage.showAndWait();
    }

    /**
     * Displays infos for a card
     * @param row card row
     * @param index card index
     */
    void displayCardInfo(CardRow row, int index) {

        // Retrieve card data
        CardDTO card;
        if(row == CardRow.UPPER) {
            card = guiView.getGameView().upperRowCards().get(index);
        } else {
            card = guiView.getGameView().lowerRowCards().get(index);
        }


        String title = "";
        String description = "";

        switch (card.type()) {
            case EVENT:
                switch (card.eventType()) {
                    case sustenance:
                        title = "Evento sostentamento";
                        description = "Pagate 1 cibo per ogni carta personaggio nella vostra tribù (gli edifici non contano). " +
                                "Se una volta pagato tutto il cibo che avete non siete riusciti a sfamare tutti i " +
                                "vostri personaggi , perdete i punti prestigio indicati dalla carta evento per ogni personaggio che" +
                                " non avete sfamato ";
                        break;
                    case ritual:
                        title = "Evento rituale sciamanico";
                        description = "Il giocatore con più icone nella propria tribù, guadagna i punti prestigio indicati." +
                                " Il giocatore con meno icone nella propria tribù, perde i punti prestigio indicati. " +
                                "In caso di parità, tutti i giocatori in parità ottengono o perdono i punti prestigio indicati ";
                        break;
                    case cavePaintings:
                        title = "Evento pitture rupestri";
                        description = "Per ottenere punti con questo evento dovete avere un numero minimo di artisti," +
                                " come indicato sulla carta evento. Se avete il numero di artisti indicato nella riga " +
                                "superiore della carta evento, perdete i punti prestigio indicati. Se invece avete il " +
                                "numero di artisti indicato nella riga inferiore della carta evento guadagnate i punti prestigio " +
                                "indicati per ogni vostro artista ";
                        break;
                    case hunt:
                        title = "Evento caccia";
                        description = "Prendete un cibo e guadagnate i punti prestigio indicati sulla carta " +
                                "evento per ogni cacciatore nella vostra tribù";
                        break;
                }
                break;
            case CHARACTER:
                switch (card.characterType()) {
                    case BUILDER:
                        title = "Costruttore";
                        description = "Durante la partita ogni costruttore riduce di quanto indicato " +
                                "in alto a destra il costo in cibo di ogni edificio che prendete. A fine partita ogni " +
                                "costruttore fornisce i punti prestigio indicati in basso a sinistra sulla carta ";
                        break;

                    case HUNTER:
                        title = "Cacciatore";
                        description = "Ogni volta che aggiungete la vostra tribù un cacciatore senza icona, non ottenete niente." +
                                " Ogni volta che aggiungete un cacciatore con un'icona, prendete immediatamente un cibo " +
                                "per ogni cacciatore nella vostra tribù (con o senza icona). Durante l'evento caccia prendete " +
                                "cibo e guadagnate punti prestigio in base al numero di cacciatori nella vostra tribù. ";
                        break;

                    case SHAMAN:
                        title = "Sciamano";
                        description = "Possono mostrare da una a tre icone. Durante l'evento rituale sciamanico, avere la" +
                                " maggioranza di queste icone fornisce punti prestigio. Averne invece la minoranza fa perdere punti prestigio.";
                        break;

                    case COLLECTOR:
                        title = "Raccoglitore";
                        description = "Durante l'evento sostentamento forniscono uno sconto di tre cibi sul totale che dovreste pagare." +
                                " Nota: non ottenete mai cibo direttamente dai raccoglitori";
                        break;

                    case INVENTOR:
                        title = "Inventore";
                        description = "A fine partita forniscono un numero di punti prestigio pari al numero di inventori " +
                                "nella vostra tribù moltiplicato per il numero di icone e invenzioni diverse che avete. " +
                                "Ci sono 10 diverse icone invenzione";
                        break;

                    case ARTIST:
                        title = "Artista";
                        description = "Durante l'evento pittura rupestri potete ottenere o perdere punti prestigio in" +
                                " base al numero di artisti che avete nella vostra tribù. A fine partita ottenete 10 " +
                                "punti prestigio per ogni due pittori nella vostra tribù.";
                        break;
                }
                break;
            case BUILDING:
                title = "Edificio";
                description = "Questa carta edificio ha un costo di " + card.buildingFoodCost().toString() + " cibo e " +
                        "permette di ottenere " + card.buildingPrestigeBonus() + " punti prestigio a fine partita. " +
                        "L'effetto è riportato nell'immagine";
                break;
        }

        showCardDialog(card, title, description);

    }

    /**
     * Displays a dialog containing an image and a description
     * @param card card DTO
     * @param cardDescription card description
     * @param dialogTitle title of the dialog
     */
    public void showCardDialog(CardDTO card, String dialogTitle, String cardDescription) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(dialogTitle);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Label descriptionLabel = new Label(cardDescription);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(300);

        VBox dialogContainer = new VBox(15);
        dialogContainer.setAlignment(Pos.CENTER);

        InputStream imageStream = getClass().getResourceAsStream(card.front());
        if (imageStream == null) {
            LOGGER.warning(() -> "Card image not found: " + card.front());
            descriptionLabel.setText("Immagine non trovata! " + cardDescription);
        } else {
            Image cardImage = new Image(imageStream);
            ImageView cardView = new ImageView(cardImage);
            cardView.setFitHeight(150.0);
            cardView.setPreserveRatio(true);
            dialogContainer.getChildren().add(cardView);
        }

        dialogContainer.getChildren().add(descriptionLabel);
        dialog.getDialogPane().setContent(dialogContainer);
        dialog.showAndWait();

    }

}
