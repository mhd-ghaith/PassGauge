package com.passgauge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/** Entry point. Loads the main FXML view and shows the PassGauge window. */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlUrl = getClass().getResource("/com/passgauge/view/main.fxml");
        if (fxmlUrl == null) {
            throw new IOException("Could not find main.fxml on the classpath. " +
                    "Make sure the 'resources' folder is marked as a Resources Root in IntelliJ.");
        }
        Parent root = FXMLLoader.load(fxmlUrl);
        Scene scene = new Scene(root, 1120, 720);
        scene.getStylesheets().add(getClass().getResource("/com/passgauge/css/light.css").toExternalForm());

        stage.setTitle("PassGauge");
        stage.setScene(scene);
        addAppIcon(stage);
        stage.show();
    }

    /** Loads every bundled icon resolution so the window/taskbar picks the sharpest one. */
    private void addAppIcon(Stage stage) {
        int[] sizes = {16, 32, 48, 64, 128, 256};
        for (int size : sizes) {
            URL iconUrl = getClass().getResource("/com/passgauge/images/icon-" + size + ".png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
