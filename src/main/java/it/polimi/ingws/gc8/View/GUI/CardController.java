package it.polimi.ingws.gc8.View.GUI;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public class CardController {

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
        // Print the custom coordinates inside the terminal console
        System.out.println("Card clicked! Row: " + row + ", Position: " + positionIndex);
    }
}