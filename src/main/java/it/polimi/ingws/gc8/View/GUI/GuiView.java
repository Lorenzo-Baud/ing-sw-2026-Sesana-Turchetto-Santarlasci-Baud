package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.View.TUI.Printer;
import it.polimi.ingws.gc8.View.View;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiView extends View {

    @Override
    public void run() {
        System.out.println("gui view");

        // Initialize the JavaFX Toolkit manually
        javafx.application.Platform.startup(() -> {
            try {
                // Now that the toolkit is initialized, we can create UI elements
                Stage primaryStage = new Stage();

                // Load the FXML file from the resources folder
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_gui/start_screen.fxml"));
                Parent root = loader.load();

                // Create the scene with the loaded hierarchy
                Scene scene = new Scene(root);

                // Configure and display the primary stage
                primaryStage.setTitle("Mesos Game - Login");
                primaryStage.setScene(scene);
                primaryStage.show();

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to load the FXML file. Check the path and Controller configuration.");
            }
        });
    }
}