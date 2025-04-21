package controllers;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.utils.SwingFXUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import other_classes.DBConnect;
import table_models.SalesItem;
import javafx.collections.FXCollections;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SalesController {
    // Existing FXML fields remain unchanged
    @FXML private MFXButton btnCheckout;
    @FXML private MFXButton btnDiscounts;
    @FXML private Pane cameraFrame;
    @FXML private TableColumn<SalesItem, BigDecimal> discountColumn;
    @FXML private Label discountPercentage;
    @FXML private MFXProgressBar discountedProdProgress;
    @FXML private TableColumn<SalesItem, BigDecimal> finalPriceColumn;
    @FXML private ImageView pictureDialog;
    @FXML private TableColumn<SalesItem, Integer> productIdColumn;
    @FXML private TableColumn<SalesItem, String> productNameColumn;
    @FXML private TableColumn<SalesItem, Integer> productQtyColumn;
    @FXML private ListView<String> productsListView;
    @FXML private StackPane saleMainFrame;
    @FXML private Pane salesContentPane;
    @FXML private TableView<SalesItem> salesTbl;
    @FXML private MFXTextField searchFld;
    @FXML private TableColumn<SalesItem, BigDecimal> subtotalColumn;
    @FXML private MFXProgressSpinner insertionProgress;
    @FXML private MFXRadioButton applyDiscBtn;
    @FXML private MFXTextField totalAmountFld;
    @FXML private MFXToggleButton toggleMode;
    @FXML private MFXProgressSpinner cameraLoadingSpinner;
    @FXML private MFXButton btnClearTable;

    private boolean isCameraMode = false;
    private Webcam webcam;
    private Thread captureThread;
    private final AtomicBoolean webcamActive = new AtomicBoolean(false);
    private Rectangle barcodeOverlay;
    private String lastScannedCode = "";
    private long lastScanTime = 0;
    private boolean isScanning = false;
    private ScheduledExecutorService scannerExecutor;

    @FXML
    private void initialize() {
        productsListView.setVisible(true);
        productsListView.setOpacity(1.0);
        cameraFrame.setVisible(false);
        cameraFrame.setOpacity(0.0);
        searchFld.setDisable(false);

        toggleMode.setOnAction(event -> toggleMode());
        btnDiscounts.setOnAction(event -> openDiscountsManager());

        saleMainFrame.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                closeWebcam();
            }
        });

        // Add this clear table button handler
        btnClearTable.setOnAction(event -> {
            showConfirmationDialog("Are you sure you want to empty the table?", confirmed -> {
                if (confirmed) {
                    // Clear the table
                    salesTbl.getItems().clear();

                    // Reset the total amount field
                    totalAmountFld.setText("0.00");

                    // Show notification
                    showNotification("🔔 Sales table has been cleared");
                }
            });
        });

        setupSalesTable();
        setupProductSelectionHandler();
        setupSearchFilter();
        loadProductsList();
    }

    private void showConfirmationDialog(String message, Consumer<Boolean> resultHandler) {
        // Create custom alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/sales.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 10px;" +
                "-fx-border-radius: 10px;" +
                "-fx-border-color: #81B29A;" +
                "-fx-border-width: 2px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.2, 0.1, 0);");

        // Style the buttons
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        String buttonStyle = "-fx-background-color: #81B29A;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 8px 20px;";

        okButton.setStyle(buttonStyle);
        cancelButton.setStyle(buttonStyle);

        // Set result handler
        alert.showAndWait().ifPresent(result -> {
            resultHandler.accept(result == ButtonType.OK);
        });
    }

    private void showNotification(String message) {
        // Create notification pane
        Pane notification = new Pane();
        notification.setStyle(
                "-fx-background-color: #81b29a;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 3);"
        );

        // Add notification text
        Label label = new Label(message);
        label.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10px;"
        );

        // Size and position the elements
        notification.getChildren().add(label);
        double notificationWidth = 240;
        double notificationHeight = 40;

        label.setPrefWidth(notificationWidth);
        label.setPrefHeight(notificationHeight);
        label.setAlignment(Pos.CENTER);

        notification.setPrefWidth(notificationWidth);
        notification.setPrefHeight(notificationHeight);
        notification.setLayoutX(salesContentPane.getWidth() - notificationWidth - 20);
        notification.setLayoutY(salesContentPane.getHeight() - notificationHeight - 20);

        // Add to scene and animate
        salesContentPane.getChildren().add(notification);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.setOnFinished(e -> salesContentPane.getChildren().remove(notification));
        fadeOut.play();
    }

    private void openDiscountsManager() {
        try {
            // Show loading indicator first
            Stage loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.TRANSPARENT);
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.initOwner(btnDiscounts.getScene().getWindow());

            ProgressIndicator progress = new ProgressIndicator();
            progress.setStyle("-fx-progress-color: #81B29A;");

            Label loadingLabel = new Label("Opening Discounts Manager...");
            loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

            VBox loadingBox = new VBox(10, progress, loadingLabel);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(20));
            loadingBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

            Scene loadingScene = new Scene(loadingBox);
            loadingScene.setFill(Color.TRANSPARENT);
            loadingStage.setScene(loadingScene);
            loadingStage.show();

            // Load the dialog in background thread
            new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/product-discount.fxml"));
                    Parent root = loader.load();
                    root.getStylesheets().add(getClass().getResource("/css/product-discount.css").toExternalForm());

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            loadingStage.close();

                            // Create stage
                            Stage stage = new Stage();
                            stage.initModality(Modality.APPLICATION_MODAL);
                            stage.initOwner(btnDiscounts.getScene().getWindow());

                            // Apply visual styling using helper method
                            setupDialogVisuals(root, stage);

                            // Show the stage
                            stage.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        loadingStage.close();
                        // Show error alert if needed
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupDialogVisuals(Parent root, Stage stage) {
        // Set stage style
        stage.initStyle(StageStyle.UNDECORATED);

        // Configure scene with transparent background
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Apply rounded corners
        Rectangle clip = new Rectangle(root.prefWidth(-1), root.prefHeight(-1));
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        root.setClip(clip);

        // Add shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(15);
        dropShadow.setSpread(0.05);
        dropShadow.setOffsetY(3);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));

        // Create background panel to receive shadow
        AnchorPane shadowReceiver = new AnchorPane();
        shadowReceiver.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        shadowReceiver.setPrefSize(root.prefWidth(-1) - 2, root.prefHeight(-1) - 2);
        shadowReceiver.setEffect(dropShadow);

        // Insert shadow receiver behind other content
        ((AnchorPane)root).getChildren().add(0, shadowReceiver);

        // Adjust clip and shadow receiver on resize
        root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            clip.setWidth(newValue.getWidth());
            clip.setHeight(newValue.getHeight());
            shadowReceiver.setPrefSize(newValue.getWidth() - 2, newValue.getHeight() - 2);
        });

        // Make window draggable
        AtomicReference<Double> xOffset = new AtomicReference<>((double) 0);
        AtomicReference<Double> yOffset = new AtomicReference<>((double) 0);

        root.setOnMousePressed(event -> {
            xOffset.set(event.getSceneX());
            yOffset.set(event.getSceneY());
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset.get());
            stage.setY(event.getScreenY() - yOffset.get());
        });

        // Add fade-in animation
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void setupSalesTable() {
        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("productId"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discount"));
        finalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));

        if (salesTbl.getItems() == null) {
            salesTbl.setItems(FXCollections.observableArrayList());
        }
    }

    private void setupProductSelectionHandler() {
        productsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = productsListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    createFlyingAnimation(selectedItem);
                }
            }
        });
    }

    private void createFlyingAnimation(String selectedItem) {
        try {
            int productId = Integer.parseInt(selectedItem.split(" - ")[0]);
            String productName = selectedItem.substring(selectedItem.indexOf(" - ") + 3);
            // Use dynamic table position
            double endX = salesTbl.getBoundsInParent().getMinX() + 100;
            double endY = salesTbl.getBoundsInParent().getMinY() + 30;
            createFlyingAnimation(selectedItem, 400, 30, endX, endY, productId, productName, "#81B29A", "white");
        } catch (Exception e) {
            e.printStackTrace();
            addProductToSalesTable(
                    Integer.parseInt(selectedItem.split(" - ")[0]),
                    selectedItem.substring(selectedItem.indexOf(" - ") + 3)
            );
        }
    }

    private void createFlyingAnimation(String text, double startX, double startY,
                                       double endX, double endY, int productId, String productName,
                                       String bgColor, String textColor) {
        try {
            Label flyingLabel = new Label(text);
            flyingLabel.getStyleClass().add("flying-item");
            flyingLabel.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                            "-fx-text-fill: " + textColor + ";" +
                            "-fx-padding: 10px 20px;" +
                            "-fx-background-radius: 12px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 14px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.2, 0, 3);"
            );

            // Validate coordinates
            if (startX > 1030 || startY > 583 || endX > 1030 || endY > 583 ||
                    startX < 0 || startY < 0 || endX < 0 || endY < 0) {
                System.err.println("Coordinates out of bounds: startX=" + startX + ", startY=" + startY +
                        ", endX=" + endX + ", endY=" + endY);
            }

            // Create curved path
            Path path = new Path();
            path.getElements().add(new MoveTo(startX, startY));
            double controlX = (startX + endX) / 2;
            double controlY = Math.min(startY, endY) - 150;
            path.getElements().add(new QuadCurveTo(controlX, controlY, endX, endY));

            // Path transition
            PathTransition pathTransition = new PathTransition();
            pathTransition.setDuration(Duration.seconds(0.8));
            pathTransition.setPath(path);
            pathTransition.setNode(flyingLabel);
            pathTransition.setOrientation(PathTransition.OrientationType.NONE);
            pathTransition.setCycleCount(1);

            // Scale transition
            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(1.0), flyingLabel);
            scaleTransition.setFromX(0.8);
            scaleTransition.setFromY(0.8);
            scaleTransition.setToX(1.2);
            scaleTransition.setToY(1.2);
            scaleTransition.setAutoReverse(true);
            scaleTransition.setCycleCount(2);

            // Rotation transition
            RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1.0), flyingLabel);
            rotateTransition.setFromAngle(-10);
            rotateTransition.setToAngle(10);
            rotateTransition.setCycleCount(2);
            rotateTransition.setAutoReverse(true);

            // Fade transition
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(350), flyingLabel);
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.0);
            fadeTransition.setDelay(Duration.millis(350));

            // Combine animations
            ParallelTransition parallelTransition = new ParallelTransition(
                    pathTransition, scaleTransition, rotateTransition, fadeTransition
            );

            // Add label to scene
            saleMainFrame.getChildren().add(flyingLabel);
            flyingLabel.setLayoutX(startX - flyingLabel.getBoundsInLocal().getWidth() / 2);
            flyingLabel.setLayoutY(startY - flyingLabel.getBoundsInLocal().getHeight() / 2);
            flyingLabel.toFront();

            // Handle completion
            parallelTransition.setOnFinished(e -> {
                saleMainFrame.getChildren().remove(flyingLabel);
                addProductToSalesTable(productId, productName);
            });

            parallelTransition.play();
        } catch (Exception e) {
            e.printStackTrace();
            addProductToSalesTable(productId, productName);
        }
    }

    private void addProductToSalesTable(int productId, String productName) {
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT selling_price FROM products WHERE product_id = ?")) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal price = rs.getBigDecimal("selling_price");

                boolean productExists = false;
                for (SalesItem item : salesTbl.getItems()) {
                    if (item.getProductId() == productId) {
                        item.setQuantity(item.getQuantity() + 1);
                        productExists = true;
                        break;
                    }
                }

                if (!productExists) {
                    SalesItem newItem = new SalesItem(productId, productName, price);
                    salesTbl.getItems().add(newItem);
                }

                salesTbl.refresh();
                updateTotalAmount();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (SalesItem item : salesTbl.getItems()) {
            total = total.add(item.getFinalPrice());
        }
        totalAmountFld.setText(total.setScale(2, RoundingMode.HALF_UP).toString());
    }

    private void loadProductsList() {
        productsListView.getItems().clear();
        cameraLoadingSpinner.setVisible(true);

        new Thread(() -> {
            try (Connection conn = DBConnect.getConnection();
                 CallableStatement stmt = conn.prepareCall("{CALL GetAllProducts()}")) {

                ResultSet rs = stmt.executeQuery();
                java.util.List<String> items = new java.util.ArrayList<>();

                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");
                    items.add(id + " - " + name);
                }

                items.sort((item1, item2) -> {
                    int id1 = Integer.parseInt(item1.split(" - ")[0]);
                    int id2 = Integer.parseInt(item2.split(" - ")[0]);
                    return Integer.compare(id1, id2);
                });

                Platform.runLater(() -> {
                    productsListView.getItems().addAll(items);
                    cameraLoadingSpinner.setVisible(false);
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> cameraLoadingSpinner.setVisible(false));
            }
        }).start();
    }

    private void setupSearchFilter() {
        searchFld.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                loadProductsList();
            } else {
                String searchTerm = newValue.toLowerCase();
                productsListView.getItems().clear();

                try (Connection conn = DBConnect.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT product_id, name FROM products WHERE LOWER(name) LIKE ? ORDER BY product_id")) {

                    stmt.setString(1, "%" + searchTerm + "%");
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("product_id");
                            String name = rs.getString("name");
                            productsListView.getItems().add(id + " - " + name);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void animateProgress() {
        Thread progressThread = new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 5) {
                    final int progress = i;
                    Platform.runLater(() -> cameraLoadingSpinner.setProgress(progress / 100.0));
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();
    }

    private void initWebcam() {
        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(new Dimension(320, 240));
                webcam.open(true);

                pictureDialog.setPreserveRatio(false);
                pictureDialog.setFitWidth(325);
                pictureDialog.setFitHeight(234);

                pictureDialog.setLayoutX((cameraFrame.getWidth() - 325) / 2);
                pictureDialog.setLayoutY((cameraFrame.getHeight() - 234) / 2);

                cameraFrame.setStyle("-fx-background-color: transparent;");
                startCaptureThread();
            } else {
                System.err.println("No webcam detected");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCaptureThread() {
        webcamActive.set(true);

        barcodeOverlay = new Rectangle(0, 0, 200, 80);
        barcodeOverlay.setStroke(javafx.scene.paint.Color.GREEN);
        barcodeOverlay.setStrokeWidth(3);
        barcodeOverlay.setFill(javafx.scene.paint.Color.TRANSPARENT);
        barcodeOverlay.setVisible(false);

        Platform.runLater(() -> cameraFrame.getChildren().add(barcodeOverlay));

        scannerExecutor = Executors.newSingleThreadScheduledExecutor();

        captureThread = new Thread(() -> {
            while (webcamActive.get() && !Thread.interrupted()) {
                try {
                    if (webcam != null && webcam.isOpen()) {
                        BufferedImage currentFrame = webcam.getImage();
                        if (currentFrame != null) {
                            final javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(currentFrame, null);

                            if (!isScanning) {
                                isScanning = true;
                                final BufferedImage frameForScanning = currentFrame;

                                scannerExecutor.schedule(() -> {
                                    try {
                                        scanForBarcode(frameForScanning);
                                    } finally {
                                        isScanning = false;
                                    }
                                }, 0, TimeUnit.MILLISECONDS);
                            }

                            Platform.runLater(() -> {
                                if (pictureDialog != null) {
                                    pictureDialog.setImage(fxImage);
                                }
                            });
                        }
                        Thread.sleep(50);
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

    private void scanForBarcode(BufferedImage image) {
        try {
            // Enforce 3-second interval
            if (System.currentTimeMillis() - lastScanTime < 3000) {
                isScanning = false; // Reset scanning flag
                return;
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            try {
                Result result = reader.decode(bitmap);
                String scannedCode = result.getText();

                if (scannedCode.equals(lastScannedCode)) {
                    isScanning = false; // Reset scanning flag when duplicate code found
                    return;
                }

                lastScannedCode = scannedCode;
                lastScanTime = System.currentTimeMillis();

                ResultPoint[] points = result.getResultPoints();
                if (points != null && points.length >= 2) {
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = 0, maxY = 0;

                    for (ResultPoint point : points) {
                        minX = Math.min(minX, point.getX());
                        minY = Math.min(minY, point.getY());
                        maxX = Math.max(maxX, point.getX());
                        maxY = Math.max(maxY, point.getY());
                    }

                    // Calculate original barcode dimensions with padding
                    float padding = 20; // Reduced padding to prevent overflow
                    float width = maxX - minX + 2 * padding;
                    float height = maxY - minY + 2 * padding;

                    // Scale coordinates to match pictureDialog (325x234) from webcam (320x240)
                    double scaleX = pictureDialog.getFitWidth() / 320.0; // 325 / 320
                    double scaleY = pictureDialog.getFitHeight() / 240.0; // 234 / 240
                    double scaledMinX = minX * scaleX;
                    double scaledMinY = minY * scaleY;
                    double scaledWidth = width * scaleX;
                    double scaledHeight = height * scaleY;

                    // Adjust for pictureDialog's position within cameraFrame
                    double offsetX = pictureDialog.getLayoutX();
                    double offsetY = pictureDialog.getLayoutY();

                    // Clamp coordinates to stay within pictureDialog bounds
                    double finalMinX = Math.max(0, Math.min(scaledMinX + offsetX, pictureDialog.getFitWidth() - scaledWidth));
                    double finalMinY = Math.max(0, Math.min(scaledMinY + offsetY, pictureDialog.getFitHeight() - scaledHeight));
                    double finalWidth = Math.min(scaledWidth, pictureDialog.getFitWidth() - finalMinX + offsetX);
                    double finalHeight = Math.min(scaledHeight, pictureDialog.getFitHeight() - finalMinY + offsetY);

                    Platform.runLater(() -> {
                        barcodeOverlay.setX(finalMinX);
                        barcodeOverlay.setY(finalMinY);
                        barcodeOverlay.setWidth(finalWidth);
                        barcodeOverlay.setHeight(finalHeight);
                        barcodeOverlay.setVisible(true);

                        FadeTransition fade = new FadeTransition(Duration.millis(500), barcodeOverlay);
                        fade.setFromValue(1.0);
                        fade.setToValue(0.0);
                        fade.setDelay(Duration.millis(500));
                        fade.setCycleCount(2);
                        fade.setAutoReverse(true);
                        fade.play();
                    });

                    lookupProductByBarcode(scannedCode);
                }
            } catch (NotFoundException e) {
                Platform.runLater(() -> barcodeOverlay.setVisible(false));
            } finally {
                isScanning = false; // Always reset scanning flag when done
            }
        } catch (Exception e) {
            isScanning = false; // Reset scanning flag on error
            e.printStackTrace();
        }
    }

    private void lookupProductByBarcode(String barcode) {
        new Thread(() -> {
            try (Connection conn = DBConnect.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT product_id, name, selling_price FROM products WHERE product_id = ?")) {

                // Try to parse barcode as product ID
                try {
                    int productId = Integer.parseInt(barcode);
                    stmt.setInt(1, productId);

                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        int retrievedProductId = rs.getInt("product_id");
                        String productName = rs.getString("name");
                        BigDecimal sellingPrice = rs.getBigDecimal("selling_price");

                        Platform.runLater(() -> {
                            // Use same start and end positions as productListView
                            double startX = 400; // Match productListView start
                            double startY = 30;  // Match productListView start
                            double endX = salesTbl.getBoundsInParent().getMinX() + 100; // Match productListView end
                            double endY = salesTbl.getBoundsInParent().getMinY() + 30;  // Match productListView end

                            String displayText = retrievedProductId + " - " + productName;

                            // Create animation with same style as productListView
                            createFlyingAnimation(displayText, startX, startY, endX, endY,
                                    retrievedProductId, productName, "#81B29A", "white");

                            System.out.println("Barcode scanned: " + barcode + " -> " + productName);
                        });
                    } else {
                        System.out.println("No product found for barcode: " + barcode);
                        Platform.runLater(() -> showBarcodeNotFoundNotification(barcode));
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid barcode format: " + barcode);
                    Platform.runLater(() -> showBarcodeNotFoundNotification(barcode));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showBarcodeNotFoundNotification(String barcode) {
        Platform.runLater(() -> {
            // Create notification label
            Label errorLabel = new Label("Unknown barcode: " + barcode);
            errorLabel.getStyleClass().add("flying-item");
            errorLabel.setStyle("-fx-background-color: #E07A5F; -fx-padding: 5px 10px; " +
                    "-fx-background-radius: 5px; -fx-text-fill: white; " +
                    "-fx-font-weight: bold;");

            // Position at barcode overlay location
            double startX = barcodeOverlay.getX() + barcodeOverlay.getWidth()/2;
            double startY = barcodeOverlay.getY() + barcodeOverlay.getHeight()/2;

            // Add to parent pane
            Pane parent = (Pane) salesTbl.getParent();
            parent.getChildren().add(errorLabel);
            errorLabel.setLayoutX(startX - errorLabel.getPrefWidth()/2);
            errorLabel.setLayoutY(startY);

            // Create fade animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(2000), errorLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.millis(1000));
            fadeOut.setOnFinished(event -> parent.getChildren().remove(errorLabel));
            fadeOut.play();
        });
    }

    private void closeWebcam() {
        if (webcamActive.getAndSet(false) && webcam != null) {
            if (captureThread != null) {
                captureThread.interrupt();
                try {
                    captureThread.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (scannerExecutor != null) {
                scannerExecutor.shutdown();
                try {
                    scannerExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            webcam.close();
        }
    }

    private void toggleMode() {
        isCameraMode = !isCameraMode;

        if (isCameraMode) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), productsListView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            cameraLoadingSpinner.setProgress(0);
            cameraLoadingSpinner.setVisible(true);
            searchFld.setDisable(true);

            fadeOut.setOnFinished(e -> {
                productsListView.setVisible(false);
                cameraFrame.setVisible(true);

                animateProgress();

                Thread initThread = new Thread(() -> {
                    initWebcam();

                    Platform.runLater(() -> {
                        cameraLoadingSpinner.setVisible(false);
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), cameraFrame);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();
                    });
                });

                initThread.setDaemon(true);
                initThread.start();
            });

            fadeOut.play();
        } else {
            closeWebcam();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), cameraFrame);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), productsListView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            fadeOut.setOnFinished(e -> {
                cameraFrame.setVisible(false);
                productsListView.setVisible(true);
                searchFld.setDisable(false);
                fadeIn.play();
            });

            fadeOut.play();
        }
    }
}