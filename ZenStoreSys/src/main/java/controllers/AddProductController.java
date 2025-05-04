package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.models.spinner.*;
import io.github.palexdev.materialfx.utils.SwingFXUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import other_classes.ProductDAO;
import table_models.Category;
import utils.ProductUtils;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AddProductController {

    @FXML
    private AnchorPane addMPane;

    @FXML
    private MFXButton btnAddProduct;

    @FXML
    private MFXButton btnEditCategory;

    @FXML
    private MFXButton btnImport;

    @FXML
    private MFXButton btnPic;

    @FXML
    private MFXButton btnPrintCode;

    @FXML
    private MFXComboBox<Category> categoryCb;

    @FXML
    private MFXTextField costPriceFld;

    @FXML
    private MFXTextField markupFld;

    @FXML
    private MFXTextField nameFld;

    @FXML
    private Pane picFrame;

    @FXML
    private ImageView productPic;

    @FXML
    private MFXTextField sellingPriceFld;

    @FXML
    private MFXSpinner<Integer> stocksSpinner;

    private File selectedImageFile;

    private ProductController productController;

    private double xOffset = 0;
    private double yOffset = 0;


    @FXML
    public void initialize() {
        // Load categories
        loadCategories();

        validateCostPrice();


        markupFld.textProperty().addListener((obs, old, newVal) -> calculateSellingPrice());

        // Setup button actions
        btnAddProduct.setOnAction(e -> addProduct());
        btnPic.setOnAction(e -> captureImage());
        btnImport.setOnAction(e -> importImage());
        btnEditCategory.setOnAction(e -> openCategoryManager());
        btnPrintCode.setOnAction(e -> openPrintDialog());

        // Initialize the spinner model with IntegerSpinnerModel
        IntegerSpinnerModel spinnerModel = new IntegerSpinnerModel(10);
        stocksSpinner.setSpinnerModel(spinnerModel);
        stocksSpinner.setPromptText("Stocks");

    }

    private void openPrintDialog() {
        try {
            // Create and show loading indicator first
            Stage loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.TRANSPARENT);
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.initOwner(addMPane.getScene().getWindow());

            ProgressIndicator progress = new ProgressIndicator();
            progress.setStyle("-fx-progress-color: #81B29A;");

            Label loadingLabel = new Label("Opening Print Dialog...");
            loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

            VBox loadingBox = new VBox(10, progress, loadingLabel);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(20));
            loadingBox.setStyle("-fx-background-color: transparent; -fx-background-radius: 10;");

            Scene loadingScene = new Scene(loadingBox);
            loadingScene.setFill(Color.TRANSPARENT);
            loadingStage.setScene(loadingScene);
            loadingStage.show();

            // Load dialog in background
            new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/print-dialog.fxml"));
                    Parent root = loader.load();
                    root.getStylesheets().add(getClass().getResource("/css/print-dialog.css").toExternalForm());

                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // Close loading stage
                            loadingStage.close();

                            // Setup and show actual dialog
                            Scene scene = new Scene(root);
                            scene.setFill(Color.TRANSPARENT);

                            Stage stage = new Stage();
                            stage.initStyle(StageStyle.TRANSPARENT);
                            stage.setScene(scene);
                            stage.initModality(Modality.APPLICATION_MODAL);
                            stage.initOwner(addMPane.getScene().getWindow());

                            // Apply visual effects (rounded corners, shadows)
                            setupDialogVisuals(root, stage);

                            stage.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Error loading print dialog: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        loadingStage.close();
                        showAlert(Alert.AlertType.ERROR, "Error loading print dialog: " + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error opening print dialog: " + e.getMessage());
        }
    }

    // Helper method for visual setup
    private void setupDialogVisuals(Parent root, Stage stage) {
        // Apply rounded corners
        Rectangle clip = new Rectangle(root.prefWidth(-1), root.prefHeight(-1));
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        root.setClip(clip);

        // Add shadow effect
        javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
        dropShadow.setRadius(15);
        dropShadow.setSpread(0.05);
        dropShadow.setOffsetY(3);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));

        AnchorPane shadowReceiver = new AnchorPane();
        shadowReceiver.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        shadowReceiver.setPrefSize(root.prefWidth(-1) - 2, root.prefHeight(-1) - 2);
        shadowReceiver.setEffect(dropShadow);

        AnchorPane.setTopAnchor(shadowReceiver, 1.0);
        AnchorPane.setLeftAnchor(shadowReceiver, 1.0);
        AnchorPane.setRightAnchor(shadowReceiver, 1.0);
        AnchorPane.setBottomAnchor(shadowReceiver, 1.0);

        ((AnchorPane)root).getChildren().add(0, shadowReceiver);

        // Adjust clip on resize
        root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            clip.setWidth(newValue.getWidth());
            clip.setHeight(newValue.getHeight());
            shadowReceiver.setPrefSize(newValue.getWidth() - 2, newValue.getHeight() - 2);
        });

        // Fade-in animation
        root.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void openCategoryManager() {
        try {
            // Show loading indicator first
            Stage loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.TRANSPARENT);
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.initOwner(addMPane.getScene().getWindow());

            ProgressIndicator progress = new ProgressIndicator();
            progress.setStyle("-fx-progress-color: #81B29A;");

            Label loadingLabel = new Label("Opening Category Manager...");
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
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/product-category.fxml"));
                    Parent root = loader.load();
                    root.getStylesheets().add(getClass().getResource("/css/product-category.css").toExternalForm());

                    javafx.application.Platform.runLater(() -> {
                        try {
                            loadingStage.close();

                            CategoryController controller = loader.getController();

                            // Create stage with transparent style
                            Stage stage = new Stage();
                            stage.initStyle(StageStyle.TRANSPARENT);
                            stage.initModality(Modality.APPLICATION_MODAL);
                            stage.initOwner(addMPane.getScene().getWindow());

                            // Apply visual enhancements
                            setupDialogVisuals(root, stage);

                            // Add inner shadow to barPane
                            javafx.scene.effect.InnerShadow innerShadow = new javafx.scene.effect.InnerShadow();
                            innerShadow.setRadius(2);
                            innerShadow.setChoke(0.1);
                            innerShadow.setOffsetY(1);
                            innerShadow.setColor(Color.rgb(0, 0, 0, 0.1));
                            controller.barPane.setEffect(innerShadow);

                            // Make window draggable
                            controller.barPane.setOnMousePressed(event -> {
                                xOffset = event.getSceneX();
                                yOffset = event.getSceneY();
                            });

                            controller.barPane.setOnMouseDragged(event -> {
                                stage.setX(event.getScreenX() - xOffset);
                                stage.setY(event.getScreenY() - yOffset);
                            });

                            // Override close button action
                            controller.btnExit.setOnAction(e -> {
                                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
                                fadeOut.setFromValue(1.0);
                                fadeOut.setToValue(0.0);
                                fadeOut.setOnFinished(event -> stage.close());
                                fadeOut.play();
                            });

                            // Create scene and show stage
                            Scene scene = new Scene(root);
                            scene.setFill(Color.TRANSPARENT);
                            stage.setScene(scene);
                            stage.centerOnScreen();
                            stage.show();

                            // Play fade-in animation
                            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                            fadeIn.setFromValue(0.0);
                            fadeIn.setToValue(1.0);
                            fadeIn.play();

                            // Wait for stage to close
                            stage.setOnHidden(e -> loadCategories());

                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Error setting up category dialog: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        loadingStage.close();
                        showAlert(Alert.AlertType.ERROR, "Error loading category manager: " + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error opening category manager: " + e.getMessage());
        }
    }



    public void setProductController(ProductController productController) {
        // Store reference to ProductController if needed
        this.productController = productController;
    }

    private void validateCostPrice(){
        costPriceFld.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains("-")) {
                // Play Windows alert sound
                java.awt.Toolkit.getDefaultToolkit().beep();

                costPriceFld.clear();

                // Show styled warning dialog
                showStyledAlert(Alert.AlertType.WARNING, "Negative values are not allowed for cost price");
            } else {
                calculateSellingPrice(); // Only calculate if input is valid
            }
        });
    }

    private void showStyledAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Product Management");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Get the dialog pane
        DialogPane dialogPane = alert.getDialogPane();

        // Add custom stylesheet
        dialogPane.getStylesheets().add(getClass().getResource("/css/products.css").toExternalForm());
        dialogPane.getStyleClass().add("styled-alert");
        dialogPane.setId("elegant-dialog");

        // Apply dialog style
        dialogPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-border-color: #81B29A;" +
                        "-fx-border-width: 2px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.2, 0.1, 0);"
        );

        // Get the button and add style class
        javafx.scene.control.Button okButton = (javafx.scene.control.Button)
                dialogPane.lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.getStyleClass().add("dialog-button");

        // Define normal, hover, and pressed styles
        String normalStyle = "-fx-background-color: #81B29A;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 8px 20px;" +
                "-fx-cursor: hand;" +
                "-fx-transition: all 0.2s ease;";

        String hoverStyle = normalStyle +
                "-fx-background-color: #6d9a86;" +  // Darker shade for hover
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0.1, 0, 1);" +
                "-fx-scale-x: 1.03;" +
                "-fx-scale-y: 1.03;";

        String pressedStyle = normalStyle +
                "-fx-background-color: #5e8a75;" +  // Even darker shade for pressed
                "-fx-scale-x: 0.95;" +             // Scale down when pressed
                "-fx-scale-y: 0.95;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0.1, 0, 1);";

        // Apply initial style
        okButton.setStyle(normalStyle);

        // Add mouse event handlers for interactive effects
        okButton.setOnMouseEntered(e -> okButton.setStyle(hoverStyle));
        okButton.setOnMouseExited(e -> okButton.setStyle(normalStyle));
        okButton.setOnMousePressed(e -> okButton.setStyle(pressedStyle));
        okButton.setOnMouseReleased(e -> {
            // Check if mouse is still within button bounds when released
            if (okButton.isHover()) {
                okButton.setStyle(hoverStyle);
            } else {
                okButton.setStyle(normalStyle);
            }
        });

        // Style the content area
        dialogPane.lookup(".content.label").setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 10px 0px;" +
                        "-fx-text-fill: #333333;" +
                        "-fx-font-family: 'Open Sans';"
        );

        // Style the graphic (icon)
        if (dialogPane.getGraphic() != null) {
            dialogPane.getGraphic().setStyle("-fx-opacity: 0.9;");
        }

        // Show and wait
        alert.showAndWait();
    }

    private void loadCategories() {
        categoryCb.setItems(ProductDAO.getAllCategories());
    }

    private void calculateSellingPrice() {
        try {
            double costPrice = Double.parseDouble(costPriceFld.getText().trim());
            double markup = Double.parseDouble(markupFld.getText().trim());
            double sellingPrice = costPrice * (1 + markup/100);
            sellingPriceFld.setText(String.format("%.2f", sellingPrice));
        } catch (NumberFormatException e) {
            // Ignore if input is not valid numbers
        }
    }

    private void captureImage() {
        try {
            // Load the camera dialog FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/cam-dialog.fxml"));
            javafx.scene.layout.Region root = loader.load();
            CamDialogController controller = loader.getController();

            // Set parent controller reference for callback
            controller.setParentController(this);

            // Apply CSS
            root.getStylesheets().add(getClass().getResource("/css/cam-dialog.css").toExternalForm());

            // Create transparent scene
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            // Create stage with transparent style
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(addMPane.getScene().getWindow());

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

            // Add drop shadow for depth effect
            javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
            dropShadow.setRadius(5);
            dropShadow.setSpread(0.05);
            dropShadow.setOffsetY(3);
            dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));

            // Add background panel to receive shadow
            AnchorPane shadowReceiver = new AnchorPane();
            shadowReceiver.setStyle("-fx-background-color: #F5EBE0; -fx-background-radius: 10;");
            shadowReceiver.setPrefSize(root.getPrefWidth() - 2, root.getPrefHeight() - 2);
            shadowReceiver.setEffect(dropShadow);

            // Place shadow receiver behind content
            AnchorPane.setTopAnchor(shadowReceiver, 2.0);
            AnchorPane.setLeftAnchor(shadowReceiver, 2.0);
            AnchorPane.setRightAnchor(shadowReceiver, 2.0);
            AnchorPane.setBottomAnchor(shadowReceiver, 2.0);

            // Add shadow receiver at index 0 (behind other content)
            ((AnchorPane)root).getChildren().add(0, shadowReceiver);

            // Update clip size when window is resized
            root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
                clip.setWidth(newValue.getWidth());
                clip.setHeight(newValue.getHeight());
                shadowReceiver.setPrefSize(newValue.getWidth() - 2, newValue.getHeight() - 2);
            });


            // Set initial opacity for fade-in
            root.setOpacity(0);

            // Show the dialog
            stage.centerOnScreen();
            stage.show();

            // Play fade-in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error loading camera dialog: " + e.getMessage());
        }
    }

    // Add this method to AddProductController to receive the captured image
    public void setProductImage(Image image) {
        if (image != null) {
            productPic.setImage(image);

            // Create a temporary file to store the captured image
            try {
                File tempDir = new File("C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage");
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }

                // Create a unique file name
                String fileName = "captured_" + System.currentTimeMillis() + ".png";
                File imageFile = new File(tempDir, fileName);

                // Convert Image to BufferedImage and save to file
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bufferedImage, "png", imageFile);

                // Store the file reference for later use during product saving
                selectedImageFile = imageFile;

                showNotification("Image captured successfully!");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Failed to save captured image: " + e.getMessage());
            }
        }
    }


    private void importImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(addMPane.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;

            // Display image preview
            try {
                Image image = new Image(file.toURI().toString());
                productPic.setImage(image);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error loading image");
            }
        }
    }

    private void addProduct() {
        try {
            // Validate inputs
            if (nameFld.getText().trim().isEmpty() || categoryCb.getValue() == null ||
                    costPriceFld.getText().trim().isEmpty() || markupFld.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please fill all required fields");
                return;
            }

            // Get values from fields
            String name = nameFld.getText().trim();
            Category category = (Category) categoryCb.getValue();
            double costPrice = Double.parseDouble(costPriceFld.getText().trim());
            double markup = Double.parseDouble(markupFld.getText().trim());
            int stock = stocksSpinner.getValue();
            double sellingPrice = Double.parseDouble(sellingPriceFld.getText().trim());

            // Save image if selected
            String imagePath = null;
            if (selectedImageFile != null) {
                imagePath = ProductUtils.saveProductImage(selectedImageFile);
            }

            // Insert product and get ID
            int productId = ProductDAO.insertProduct(
                    name, category.getId(), costPrice, markup, stock, sellingPrice, imagePath
            );

            if (productId > 0) {
                // Generate and save barcode
                String barcodePath = ProductUtils.generateBarcode(productId);
                ProductDAO.updateBarcodePath(productId, barcodePath);

                // Show notification instead of alert
                showNotification("Product added successfully!");
                // Refresh the product table
                if (productController != null) {
                    productController.refreshProductTable();
                }
                clearFields();
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to add product");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Please enter valid numbers for prices");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error: " + e.getMessage());
        }
    }

    private void clearFields() {
        nameFld.clear();
        categoryCb.selectFirst();
        costPriceFld.clear();
        markupFld.clear();
        sellingPriceFld.clear();
        stocksSpinner.setValue(10);
        productPic.setImage(null);
        selectedImageFile = null;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type.toString());
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showNotification(String message) {
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
        double notificationWidth = 220;
        double notificationHeight = 40;

        label.setPrefWidth(notificationWidth);
        label.setPrefHeight(notificationHeight);
        label.setAlignment(Pos.CENTER);

        notification.setPrefWidth(notificationWidth);
        notification.setPrefHeight(notificationHeight);
        notification.setLayoutX(addMPane.getWidth() - notificationWidth - 20);
        notification.setLayoutY(addMPane.getHeight() - notificationHeight - 270);

        // Add to scene and animate
        addMPane.getChildren().add(notification);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.setOnFinished(e -> addMPane.getChildren().remove(notification));
        fadeOut.play();
    }

}
