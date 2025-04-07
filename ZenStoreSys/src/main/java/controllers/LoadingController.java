package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;


public class LoadingController {

    @FXML
    private Label loadingLbl;

    @FXML
    private MFXProgressBar progressBar;

    @FXML
    private MFXButton btnExit;

    @FXML
    private void initialize() {
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(3),
                ae -> {
                    try {
                        switchToLogin();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        ));
        timeline.setCycleCount(1);
        timeline.play();
    }

    @FXML
    private void onExit() {
        javafx.stage.Stage stage = (javafx.stage.Stage) btnExit.getScene().getWindow();
        stage.close();
    }

    public void switchToLogin() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/login.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);

        // Load CSS for styling
        scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());

        // Make background transparent
        scene.setFill(Color.TRANSPARENT);

        // Apply a clipping rectangle with adjustable corner radius
        double radius = 20; // Change this to adjust curvature
        Rectangle clip = new Rectangle(root.prefWidth(-1), root.prefHeight(-1));
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        root.setClip(clip);

        Stage loginStage = new Stage();
        loginStage.setScene(scene);

        // Ensure the stage has no default decorations
        loginStage.initStyle(StageStyle.TRANSPARENT);

        loginStage.centerOnScreen();
        loginStage.show();

        // Close the current loading stage
        ((Stage) progressBar.getScene().getWindow()).close();
    }




}
