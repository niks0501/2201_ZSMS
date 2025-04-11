package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardController {

    @FXML
    private Pane barPane;

    @FXML
    private MFXButton btnExit;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private Button btnNav;

    @FXML
    private HBox navigationPane;

    @FXML
    public StackPane contentPane;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean topBarVisible = false;

    @FXML
    private void initialize() {
        // Set up event handlers for exit and minimize buttons
        btnExit.setOnAction(event -> handleExit());
        btnMinimize.setOnAction(event -> handleMinimize());
        btnNav.setOnAction(event -> toggleNavigation());

        // Set up window dragging
        barPane.setOnMousePressed(this::handleMousePressed);
        barPane.setOnMouseDragged(this::handleMouseDragged);

        // Hide navigation pane initially and position it
        navigationPane.setVisible(false);
        navigationPane.setTranslateY(-navigationPane.getPrefHeight());

        // Fix: Load TopBarController and pass contentPane reference
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/topbar.fxml"));
            Parent topBar = loader.load();

            // Get the controller and pass the contentPane reference
            TopBarController topBarController = loader.getController();
            topBarController.setContentPane(contentPane);

            // Clear and add the newly loaded topbar to navigationPane
            navigationPane.getChildren().clear();
            navigationPane.getChildren().add(topBar);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load TopBarController: " + e.getMessage());
        }
    }

    private void toggleNavigation() {
        if (!topBarVisible) {
            // Show navigation bar with slide down animation and fade in
            navigationPane.setVisible(true);
            navigationPane.setOpacity(0); // Start fully transparent

            // Create slide down transition
            TranslateTransition slideDown = new TranslateTransition(Duration.millis(300), navigationPane);
            slideDown.setFromY(-navigationPane.getPrefHeight());
            slideDown.setToY(0);
            slideDown.setInterpolator(Interpolator.EASE_OUT);

            // Create fade in transition
            FadeTransition fadeIn = new FadeTransition(Duration.millis(1500), navigationPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(Interpolator.EASE_BOTH);

            // Play both animations in parallel
            ParallelTransition parallelTransition = new ParallelTransition(slideDown, fadeIn);

            // Rotate the button counter-clockwise after animations complete
            parallelTransition.setOnFinished(event -> {
                RotateTransition rotateBtn = new RotateTransition(Duration.millis(100), btnNav);
                rotateBtn.setByAngle(-90);
                rotateBtn.setInterpolator(Interpolator.EASE_BOTH);
                rotateBtn.play();
            });

            parallelTransition.play();
            topBarVisible = true;
        } else {
            // Hide navigation bar with slide up animation and fade out

            // Create slide up transition
            TranslateTransition slideUp = new TranslateTransition(Duration.millis(200), navigationPane);
            slideUp.setFromY(0);
            slideUp.setToY(-navigationPane.getPrefHeight());
            slideUp.setInterpolator(Interpolator.EASE_IN);

            // Create fade out transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), navigationPane);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setInterpolator(Interpolator.EASE_BOTH);

            // Play both animations in parallel
            ParallelTransition parallelTransition = new ParallelTransition(slideUp, fadeOut);

            // Rotate the button back clockwise after animations complete
            parallelTransition.setOnFinished(event -> {
                RotateTransition rotateBtn = new RotateTransition(Duration.millis(100), btnNav);
                rotateBtn.setByAngle(90);
                rotateBtn.setInterpolator(Interpolator.EASE_BOTH);
                rotateBtn.play();
                navigationPane.setVisible(false);
            });

            parallelTransition.play();
            topBarVisible = false;
        }
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

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) barPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

}
