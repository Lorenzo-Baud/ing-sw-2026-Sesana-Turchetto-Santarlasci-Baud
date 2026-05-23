package it.polimi.ingws.gc8.View.GUI;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.util.logging.Logger;

public class CardController {

    private static final Logger LOGGER = Logger.getLogger(CardController.class.getName());

    @FXML
    private AnchorPane cardRoot;

    /**
     * Represents row:
     *  0. Upper row
     *  1. Middle row
     *  2. Lower row
     */
    private int row;
    private int positionIndex;

    /**
     * Set the coordinate tracking data for this card instance.
     */
    public void setCoordinates(int row, int positionIndex) {
        this.row = row;
        this.positionIndex = positionIndex;
    }

    /**
     * FXML OnMouseClicked handler attached to the main card container.
     */
    @FXML
    private void handleCardClick(MouseEvent event) {
        LOGGER.fine(() -> "Card clicked! Row: " + row + ", Position: " + positionIndex);
        // TODO: forward click to the owning controller / build a PlayerAction
    }
}
