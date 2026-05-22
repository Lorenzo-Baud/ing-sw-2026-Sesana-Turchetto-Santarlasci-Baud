package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.Messages.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.io.InputStream;
import java.util.List;

/**
 * JavaFX controller for the main game screen.
 *
 * <p>This controller is responsible for initializing parts of the game UI and
 * providing helper methods used by the {@code GuiView}. The controller exposes
 * fields injected from the FXML and lifecycle methods that update the view.
 *
 * <p>Note: the controller delegates most of the model-driven updates to the
 * {@link GuiView} instance which is injected via {@link #setGuiView(GuiView)}.
 */
public class GameScreenController {

    /** Reference to the owning GuiView used to interact with the rest of the UI and network.
     * This is set by the view after loading the FXML. */
    private GuiView guiView;

    /** HBox container for the top row of card image nodes. */
    @FXML
    private HBox topCardsRow;

    /** HBox container for the middle row of card image nodes. */
    @FXML
    private HBox middleCardsRow;

    /** HBox container for the bottom row of card image nodes. */
    @FXML
    private HBox bottomCardsRow;

    /** Label used in lobby updates to show current/expected players. Injected from FXML. */
    @FXML
    private Label connectedPlayersLabel;

    /** Panes used on top bar to display players stats*/
    @FXML
    Pane topBarP3Box;
    @FXML
    Pane topBarP4Box;
    @FXML
    Pane topBarP5Box;

    /** Player markers on top bar */
    @FXML
    Circle topBarP1Marker;
    @FXML
    Circle topBarP2Marker;
    @FXML
    Circle topBarP3Marker;
    @FXML
    Circle topBarP4Marker;
    @FXML
    Circle topBarP5Marker;
    List<Circle> topBarPlayerMarkers;

    /** Player markers on side bar */
    @FXML
    Circle leftBarTurnOrderCircle1;
    @FXML
    Circle leftBarTurnOrderCircle2;
    @FXML
    Circle leftBarTurnOrderCircle3;
    @FXML
    Circle leftBarTurnOrderCircle4;
    @FXML
    Circle leftBarTurnOrderCircle5;
    List<Circle> leftBarTurnOrderCircles;

    /** Other labels on top bar */
    @FXML
    Label topBarP1PrestigeLabel;
    @FXML
    Label topBarP2PrestigeLabel;
    @FXML
    Label topBarP3PrestigeLabel;
    @FXML
    Label topBarP4PrestigeLabel;
    @FXML
    Label topBarP5PrestigeLabel;
    List<Label> topBarPlayerPrestigeLabels;

    @FXML
    Label topBarEraLabel;
    @FXML
    Label topBarRoundLabel;

    @FXML
    Label sideBarFoodLabel;
    @FXML
    Label sideBarPrestigeLabel;

    /**
     * Injects the owning {@link GuiView} instance.
     *
     * @param guiView the GuiView that owns this controller; must not be {@code null}
     */
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    /**
     * JavaFX initialize lifecycle method called after FXML fields are injected.
     *
     * <p>This method performs lightweight UI initialization such as setting the
     * stage title and adding a sample card image to the middle row. It may throw
     * InterruptedException if interrupted while performing setup (kept for parity
     * with the original signature).
     *
     * @throws InterruptedException if the initialization is interrupted
     */
    @FXML
    public void initialize() throws InterruptedException {

        guiView.getPrimaryStage().setTitle("Mesos Game - Gioco");

        // Initializes top bar
        initializeInterface();

        updateInterface();

    }

    /**
     * Method to correctly initialize the interface. Performs following actions:
     *  - Hides top bar panes that don't correspond to any player in the current game
     *    (e.g. hides P3-P5 panes in a 2-player game)
     *  - Changes top bar icon colors for players
     *  - Updates prestige points of all players
     *  - Updates sidebars
     */
    private void initializeInterface() {

        // Initializes array
        topBarPlayerMarkers = List.of(topBarP1Marker, topBarP2Marker, topBarP3Marker, topBarP4Marker, topBarP5Marker);
        topBarPlayerPrestigeLabels = List.of(topBarP1PrestigeLabel, topBarP2PrestigeLabel, topBarP3PrestigeLabel, topBarP4PrestigeLabel, topBarP5PrestigeLabel);
        leftBarTurnOrderCircles = List.of(leftBarTurnOrderCircle1, leftBarTurnOrderCircle2, leftBarTurnOrderCircle3, leftBarTurnOrderCircle4, leftBarTurnOrderCircle5);

        // Hides useless top bar panes
        if(guiView.getGameView().numberOfPlayers() == 2) {
            topBarP3Box.setVisible(false);
            topBarP4Box.setVisible(false);
            topBarP5Box.setVisible(false);
        } else if(guiView.getGameView().numberOfPlayers() == 3) {
            topBarP4Box.setVisible(false);
            topBarP5Box.setVisible(false);
        } else if(guiView.getGameView().numberOfPlayers() == 4) {
            topBarP3Box.setVisible(false);
        }

        // Sets top bar marker colors based on player totem color
        for(int i = 0; i < guiView.getGameView().numberOfPlayers(); i++) {
            switch (guiView.getGameView().players().get(i).totemColor()) {
                case RED -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: red;");
                case BLUE -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: blue;");
                case WHITE -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: white;");
                case YELLOW -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: yellow;");
                case BLACK -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: black;");
            }
        }

        // Sets left bar marker colors based on player totem color
        for(int i = 0; i < guiView.getGameView().numberOfPlayers(); i++) {
            switch (guiView.getGameView().turnOrderTrack().get(i).totemColor()) {
                case RED -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: red;");
                case BLUE -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: blue;");
                case WHITE -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: white;");
                case YELLOW -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: yellow;");
                case BLACK -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: black;");
            }
        }

        // Sets top bar prestige points label to 0
        for(int i = 0; i < guiView.getGameView().numberOfPlayers(); i++) {
            topBarPlayerPrestigeLabels.get(i).setText("P" + (i+1) + ": 0PP");
        }

        // Sidebar update
        sideBarPrestigeLabel.setText(String.valueOf(guiView.getGameView().currentPlayer().prestigePoints()));
        sideBarFoodLabel.setText(String.valueOf(guiView.getGameView().currentPlayer().foodPoints()));

    }

    /**
     * Method to update the entire interface when a new model object is received
     */
    private void updateInterface() {

        if(guiView.isMyTurn()) {
            guiView.showError("Il tuo turno!", "È il tuo turno, effettua la tua mossa.");
        }

        /* ----------- TOP BAR UPDATE ----------- */
        // Sets food and prestige points on top bar
        for(int i = 0; i < guiView.getGameView().numberOfPlayers(); i++) {
            topBarPlayerPrestigeLabels.get(i).setText("P" + (i+1) + ": " + guiView.getGameView().players().get(i).prestigePoints() + "PP");
        }

        // Updates era on top bar
        topBarEraLabel.setText("Era " + guiView.getGameView().currentEra());

        // Updates round on top bar
        topBarRoundLabel.setText("Round " + guiView.getGameView().currentRound() + "/10");

        /* ----------- LEFT BAR UPDATE ----------- */
        // Sets top bar marker colors based on player totem color
        for(int i = 0; i < guiView.getGameView().numberOfPlayers(); i++) {
            switch (guiView.getGameView().players().get(i).totemColor()) {
                case RED -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: red;");
                case BLUE -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: blue;");
                case WHITE -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: white;");
                case YELLOW -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: yellow;");
                case BLACK -> topBarPlayerMarkers.get(i).setStyle("-fx-fill: black;");
            }
        }

        /* ----------- CARDS ROW UPDATE ----------- */
        // Clears all rows
        //clearAllRows();

        // Upper row update
        for(int i = 0; i < guiView.getGameView().upperRowCards().size(); i++) {
            addCardToRow(0, guiView.getGameView().upperRowCards().get(i), i);
        }

        // Lower row update
        for(int i = 0; i < guiView.getGameView().lowerRowCards().size(); i++) {
            addCardToRow(2, guiView.getGameView().lowerRowCards().get(i), i);
        }

        /* ----------- SIDE BAR UPDATE ----------- */
        sideBarPrestigeLabel.setText(String.valueOf(guiView.getGameView().currentPlayer().prestigePoints()));
        sideBarFoodLabel.setText(String.valueOf(guiView.getGameView().currentPlayer().foodPoints()));

    }

    /**
     * Helper method to remove all card nodes from every row on the board.
     *
     * <p>The clear is executed on the JavaFX Application Thread via {@link Platform#runLater}.
     */
    private void clearAllRows() {
        Platform.runLater(() -> {
            topCardsRow.getChildren().clear();
            middleCardsRow.getChildren().clear();
            bottomCardsRow.getChildren().clear();
        });
    }

    /**
     * Continuously waits for network messages and dispatches UI updates.
     *
     * <p>The loop retrieves messages from the {@link GuiView#waitForMessage} and
     * handles different {@link NetworkMessage} subclasses. Some updates are
     * dispatched on the JavaFX Application Thread where required.
     */
    private void dispatchLoop() {
        while (true) {

            NetworkMessage msg = guiView.waitForMessage();

            System.out.println("Messaggio ricevuto");

            switch (msg) {
                case null -> {
                    guiView.showError("Errore", "Connessione al server persa.");
                    return;
                }
                case ModelUpdateMessage m -> {
                    GameDTO previous = guiView.getGameView();
                    GameDTO next = m.getGameModelDTO();
                    updateInterface();
                }
                case GameOverMessage m -> {
                    // TODO: show game over screen
                }
                case ErrorMessage m -> guiView.showError("Errore", m.getError());
                case ConnectionUpdateMessage m -> {
                    // TODO: handle this
                }
                default -> {
                }
            }

        }
    }

    /**
     * Helper method to add a certain card to a row
     *  0. Top row
     *  1. Middle row
     *  2. Lower row
     */
    private void addCardToRow(int row, CardDTO card, int index) {

        InputStream imageStream = getClass().getResourceAsStream(card.front());
        Image cardImage = new Image(imageStream);

        try{

            ImageView cardView = new ImageView(cardImage);
            cardView.setFitHeight(120.0);
            cardView.setPreserveRatio(true);

            // Attach the mouse click event listener directly to the ImageView node
            cardView.setOnMouseClicked(event -> {
                // This block executes whenever the specific image is clicked
                System.out.println("Card clicked! Row: " + row + ", Position: " + index);
            });

            // Adds card to corresponding row
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
