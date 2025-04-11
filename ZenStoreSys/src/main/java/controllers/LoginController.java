package controllers;

import javafx.animation.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import other_classes.DBConnect;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class LoginController {

    @FXML
    private Label invalidLbl;

    @FXML
    private Pane barPane;

    @FXML
    private MFXButton btnExit;

    @FXML
    private Button btnLogin;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private VBox contentPane;

    @FXML
    private MFXPasswordField passwordFld;

    @FXML
    private MFXTextField usernameFld;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void initialize() {
        usernameFld.setOnKeyPressed(this::handleEnterKey);
        passwordFld.setOnKeyPressed(this::handleEnterKey);

        barPane.setOnMousePressed(this::handleMousePressed);
        barPane.setOnMouseDragged(this::handleMouseDragged);

        btnExit.setOnAction(event -> handleExit());
        btnMinimize.setOnAction(event -> handleMinimize());
        btnLogin.setOnAction(event -> {
            animateButton(btnLogin);
            handleLogin();
        });

    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) btnExit.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) btnMinimize.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void animateButton(Button button) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.9);
        st.setToY(0.9);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) barPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    private void handleEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            animateButton(btnLogin);
            handleLogin();
        }
    }


    @FXML
    private void handleLogin() {
        String username = usernameFld.getText();
        String password = passwordFld.getText();

        if (DBConnect.validateCredentials(username, password)) {
            try {
                // Close current login stage
                Stage loginStage = (Stage) btnLogin.getScene().getWindow();

                // Load the dashboard
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/dashboard.fxml"));
                javafx.scene.layout.Region root = fxmlLoader.load();

                // Create a new stage
                Stage dashboardStage = new Stage();

                // Create scene with transparent background
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());

                // Apply rounded corners
                Rectangle clip = new Rectangle(root.getPrefWidth(), root.getPrefHeight());
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                root.setClip(clip);

                // Update clip size when window is resized
                root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
                    clip.setWidth(newValue.getWidth());
                    clip.setHeight(newValue.getHeight());
                });

                // Configure stage
                dashboardStage.initStyle(StageStyle.TRANSPARENT);
                dashboardStage.setScene(scene);
                dashboardStage.centerOnScreen();

                // Add fade-in transition
                root.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.setCycleCount(1);

                // Close login stage and show dashboard
                loginStage.close();
                dashboardStage.show();
                fadeIn.play();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            invalidLbl.setText("Invalid login credentials");
            invalidLbl.setVisible(true);

            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(5),
                    ae -> invalidLbl.setVisible(false)
            ));
            timeline.setCycleCount(1);
            timeline.play();
        }
    }

}
