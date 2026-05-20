package it.polimi.ingws.gc8.View.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;

import javafx.scene.image.ImageView;
import java.io.InputStream;

public class GameScreenController {

    private GuiView guiView;

    @FXML
    private HBox topCardsRow;

    @FXML
    private HBox middleCardsRow;

    @FXML
    private HBox bottomCardsRow;

    // Setter used by GuiView to inject itself after loading the FXML
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

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

            middleCardsRow.getChildren().add(cardView);

        }
    }

    /**
     * Helper method to wipe out all cards from the entire board.
     */
    public void clearAllRows() {
        Platform.runLater(() -> {
            topCardsRow.getChildren().clear();
            middleCardsRow.getChildren().clear();
            bottomCardsRow.getChildren().clear();
        });
    }

}
