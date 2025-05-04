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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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
        // Setup UI controls first
        btnExit.setOnAction(event -> handleExit());
        btnMinimize.setOnAction(event -> handleMinimize());
        btnPicture.setOnAction(event -> takeSnapshot());
        setupWindowDrag();

        // Show loading indicator
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setStyle("-fx-progress-color: #81B29A;");
        loadingIndicator.setPrefSize(64, 64);

        // Use JavaFX Label instead of AWT Label
        javafx.scene.control.Label loadingLabel = new javafx.scene.control.Label("Initializing camera...");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox loadingBox = new VBox(10, loadingIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setStyle("-fx-background-color: transparent;");
        camPane.getChildren().add(loadingBox);

        // Initialize webcam in background thread
        new Thread(() -> {
            try {
                // Initialize hardware
                webcam = Webcam.getDefault();
                if (webcam != null) {
                    webcam.setViewSize(new Dimension(640, 480));
                    webcam.open(true); // non-blocking open

                    // Create UI components on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            // Create ImageView for camera feed
                            webcamImageView = new ImageView();
                            webcamImageView.setPreserveRatio(true);

                            // Remove loading indicator
                            camPane.getChildren().clear();
                            camPane.getChildren().add(webcamImageView);

                            // Setup styling and positioning
                            camPane.setStyle("-fx-background-color: black;");
                            AnchorPane.setTopAnchor(webcamImageView, 0.0);
                            AnchorPane.setBottomAnchor(webcamImageView, 0.0);
                            AnchorPane.setLeftAnchor(webcamImageView, 0.0);
                            AnchorPane.setRightAnchor(webcamImageView, 0.0);

                            webcamImageView.fitWidthProperty().bind(camPane.widthProperty());
                            webcamImageView.fitHeightProperty().bind(camPane.heightProperty());

                            // Start capture thread
                            startCaptureThread();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showError("Error setting up camera display: " + e.getMessage());
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        showError("No webcam detected");
                        handleExit();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Failed to initialize webcam: " + e.getMessage());
                    handleExit();
                });
            }
        }).start();
    }

    private void setupWindowDrag() {
        barPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        barPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) barPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private void startCaptureThread() {
        webcamActive.set(true);
        captureThread = new Thread(() -> {
            BufferedImage currentFrame;

            while (webcamActive.get() && !Thread.interrupted()) {
                try {
                    if (webcam != null && webcam.isOpen()) {
                        currentFrame = webcam.getImage();

                        if (currentFrame != null) {
                            final Image fxImage = SwingFXUtils.toFXImage(currentFrame, null);
                            Platform.runLater(() -> {
                                if (webcamImageView != null) {
                                    webcamImageView.setImage(fxImage);
                                }
                            });
                        }

                        // Lower frame rate to reduce CPU usage
                        Thread.sleep(100);  // 10 fps is usually sufficient for preview
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
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
            // Use JavaFX Rectangle instead of AWT Rectangle
            javafx.scene.shape.Rectangle flash = new javafx.scene.shape.Rectangle(
                    camPane.getWidth(), camPane.getHeight());
            flash.setFill(javafx.scene.paint.Color.WHITE);
            flash.setOpacity(0.7);

            Platform.runLater(() -> {
                camPane.getChildren().add(flash);

                // Flash animation
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), flash);
                fadeOut.setFromValue(0.7);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    camPane.getChildren().remove(flash);

                    // Process image in background thread
                    new Thread(() -> {
                        try {
                            BufferedImage bufferedImage = webcam.getImage();

                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            ImageIO.write(bufferedImage, "PNG", outputStream);
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                            Image fxImage = new Image(inputStream);

                            Platform.runLater(() -> {
                                if (parentController != null) {
                                    parentController.setProductImage(fxImage);
                                }
                                handleExit();
                            });
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            Platform.runLater(() -> showError("Failed to capture image: " + ex.getMessage()));
                        }
                    }).start();
                });

                fadeOut.play();
            });
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
