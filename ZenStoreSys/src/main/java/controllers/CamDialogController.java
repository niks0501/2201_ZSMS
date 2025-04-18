package controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.utils.SwingFXUtils;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CamDialogController {

    @FXML
    public Pane barPane;

    @FXML
    private MFXButton btnExit;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private MFXButton btnPicture;

    @FXML
    private AnchorPane camMainPane;

    @FXML
    private Pane camPane;

    private Webcam webcam;
    private ImageView webcamImageView;
    private Thread captureThread;
    private AddProductController parentController;
    private final AtomicBoolean webcamActive = new AtomicBoolean(false);
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void initialize() {
        //Set up exit button
        btnExit.setOnAction(event -> handleExit());

        //Set up minimize button
        btnMinimize.setOnAction(event -> handleMinimize());

        // Set up picture button
        btnPicture.setOnAction(event -> takeSnapshot());

        // Add these lines to make the barPane draggable
        barPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        barPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) barPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        // Initialize webcam after UI is visible
        Platform.runLater(this::initWebcam);
    }

    public void setParentController(AddProductController controller) {
        this.parentController = controller;
    }

    private void initWebcam() {
        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                // Set webcam to capture in 4:3 aspect ratio (common for webcams)
                webcam.setViewSize(new Dimension(640, 480));
                webcam.open();

                // Create the image view with aspect ratio preserved
                webcamImageView = new ImageView();
                webcamImageView.setPreserveRatio(true);

                // Add black background for letterbox/pillarbox areas
                camPane.setStyle("-fx-background-color: black;");

                Platform.runLater(() -> {
                    camPane.getChildren().add(webcamImageView);

                    // Center in the camPane
                    AnchorPane.setTopAnchor(webcamImageView, 0.0);
                    AnchorPane.setBottomAnchor(webcamImageView, 0.0);
                    AnchorPane.setLeftAnchor(webcamImageView, 0.0);
                    AnchorPane.setRightAnchor(webcamImageView, 0.0);

                    // Use a custom binding for sizing that preserves aspect ratio while filling the space
                    webcamImageView.fitWidthProperty().bind(camPane.widthProperty());
                    webcamImageView.fitHeightProperty().bind(camPane.heightProperty());
                });

                webcamActive.set(true);

                // Start capture thread
                captureThread = new Thread(() -> {
                    final BufferedImage[] currentFrame = new BufferedImage[1];

                    while (webcamActive.get() && !Thread.interrupted()) {
                        if (webcam != null && webcam.isOpen()) {
                            currentFrame[0] = webcam.getImage();

                            if (currentFrame[0] != null) {
                                // Update UI on JavaFX thread
                                Platform.runLater(() -> {
                                    if (webcamActive.get()) {
                                        Image fxImage = SwingFXUtils.toFXImage(currentFrame[0], null);
                                        webcamImageView.setImage(fxImage);
                                    }
                                });

                                // Limit frame rate to reduce CPU usage
                                try {
                                    Thread.sleep(50);  // ~20 fps
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }
                    }
                });

                captureThread.setDaemon(true);
                captureThread.start();

            } else {
                Platform.runLater(() -> {
                    System.err.println("No webcam detected");
                    showError("No webcam detected");
                    handleExit();
                });
            }

            camPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    closeWebcam();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to initialize webcam: " + e.getMessage());
        }
    }

    private void takeSnapshot() {
        if (webcam != null && webcam.isOpen()) {
            try {
                BufferedImage bufferedImage = webcam.getImage();

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "PNG", outputStream);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                Image fxImage = new Image(inputStream);

                if (parentController != null) {
                    parentController.setProductImage(fxImage);
                }

                handleExit();

            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to capture image: " + e.getMessage());
            }
        }
    }

    private void closeWebcam() {
        if (webcamActive.getAndSet(false) && webcam != null) {
            if (captureThread != null) {
                captureThread.interrupt();
                try {
                    captureThread.join(500);  // Wait for thread to finish
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            webcam.close();
        }
    }

    private void handleExit() {
        Stage stage = (Stage) btnExit.getScene().getWindow();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), camMainPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            closeWebcam();
            stage.close();
        });
        fadeOut.play();
    }

    private void handleMinimize() {
        Stage stage = (Stage) btnMinimize.getScene().getWindow();
        stage.setIconified(true);
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Webcam Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
