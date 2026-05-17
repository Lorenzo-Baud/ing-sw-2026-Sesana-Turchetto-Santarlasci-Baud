package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.View.TUI.Printer;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

import static java.lang.Thread.sleep;

public class LobbyScreenController {


    // Reference to the main view manager to coordinate scene switches
    private GuiView guiView;


    // Setter used by GuiView to inject itself after loading the FXML
    public void setGuiView(GuiView guiView) {
        this.guiView = guiView;
    }

    @FXML
    public void initialize() throws InterruptedException {

        guiView.getPrimaryStage().setTitle("Mesos Game - Attesa giocatori");
        waitForLobbyStart();

    }

    /** Waits for the first game snapshot. */
    private void waitForLobbyStart() {
        Printer.printInfo("Waiting for other players…");
        // Spawn a dedicated background thread to handle the polling loop
        Thread pollingThread = new Thread(() -> {
            while (true) {
                // Fetch notices from the view model/network layer
                guiView.drainPendingNotices();

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // Exit the loop cleanly if the thread gets interrupted
                    break;
                }
            }
        });

        // Start the background loop execution
        pollingThread.start();
    }

}
