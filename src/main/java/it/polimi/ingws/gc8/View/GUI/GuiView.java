package it.polimi.ingws.gc8.View.GUI;

import it.polimi.ingws.gc8.View.TUI.Printer;
import it.polimi.ingws.gc8.View.View;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class GuiView extends View {

     @Override
    public void run() {
         try {

             // Load the FXML file from the resources folder
             FXMLLoader loader = new FXMLLoader(getClass().getResource("reseources/javafx_gui/main_scene.fxml"));
             Parent root = loader.load();

             // Create the scene with the loaded hierarchy
             Scene scene = new Scene(root);

             // Configure and display the primary stage
//             primaryStage.setTitle("Mesos Game - Login");
//             primaryStage.setScene(scene);
//             primaryStage.show();

         } catch (Exception e) {
             e.printStackTrace();
             System.err.println("Failed to load the FXML file. Check the path and Controller configuration.");
         }
     }

}
