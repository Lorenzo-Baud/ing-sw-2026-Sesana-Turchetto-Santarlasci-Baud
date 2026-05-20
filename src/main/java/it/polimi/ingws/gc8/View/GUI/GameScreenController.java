package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.Network.Messages.*;
import it.polimi.ingws.gc8.View.TUI.TuiView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import javafx.scene.image.ImageView;
import java.io.InputStream;

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

        String imagePath = "/test.png";
        InputStream imageStream = getClass().getResourceAsStream(imagePath);

        if (imageStream != null) {

            Image cardImage = new Image(imageStream);

            ImageView cardView = new ImageView(cardImage);
            cardView.setFitHeight(120.0);
            cardView.setPreserveRatio(true);

            // Attach the mouse click event listener directly to the ImageView node
            cardView.setOnMouseClicked(event -> {
                // This block executes whenever the specific image is clicked
                System.out.println("Card clicked! Row: " + 0 + ", Position: " + 0);
            });

            middleCardsRow.getChildren().add(cardView);

        }
    }

    /**
     * Helper method to remove all card nodes from every row on the board.
     *
     * <p>The clear is executed on the JavaFX Application Thread via {@link Platform#runLater}.
     */
    public void clearAllRows() {
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

                    // TODO: update view
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

}
