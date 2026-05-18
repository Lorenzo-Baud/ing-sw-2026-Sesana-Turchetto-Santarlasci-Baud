package it.polimi.ingws.gc8.View.GUI;

import javafx.fxml.FXML;

public class GameScreenController {

    private GuiView guiView;

    // Setter used by GuiView to inject itself after loading the FXML
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    @FXML
    public void initialize() throws InterruptedException {

        guiView.getPrimaryStage().setTitle("Mesos Game - Gioco");

        System.out.println("Mesos Game - Gioco");

    }

}
