package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.models.spinner.*;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
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

        // Initialize the spinner model with IntegerSpinnerModel
        IntegerSpinnerModel spinnerModel = new IntegerSpinnerModel(10);
        stocksSpinner.setSpinnerModel(spinnerModel);
        stocksSpinner.setPromptText("Stocks");

    }

    private void openCategoryManager() {
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/product-category.fxml"));
            javafx.scene.layout.Region root = loader.load();
            CategoryController controller = loader.getController();

            // Apply CSS
            root.getStylesheets().add(getClass().getResource("/css/product-category.css").toExternalForm());

            // Create transparent scene
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            // Create stage with transparent style
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(addMPane.getScene().getWindow());

            // Apply rounded corners to entire window with depth effect
            Rectangle clip = new Rectangle(root.getPrefWidth(), root.getPrefHeight());
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            root.setClip(clip);

            // Add outer drop shadow for depth effect that follows the rounded corners
            javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
            dropShadow.setRadius(15);
            dropShadow.setSpread(0.05);
            dropShadow.setOffsetX(0);
            dropShadow.setOffsetY(3);
            dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));

            // Add background panel to receive shadow (slightly smaller than root)
            AnchorPane shadowReceiver = new AnchorPane();
            shadowReceiver.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
            shadowReceiver.setPrefSize(root.getPrefWidth() - 2, root.getPrefHeight() - 2);
            shadowReceiver.setEffect(dropShadow);

            // Place shadow receiver behind content
            AnchorPane.setTopAnchor(shadowReceiver, 1.0);
            AnchorPane.setLeftAnchor(shadowReceiver, 1.0);
            AnchorPane.setRightAnchor(shadowReceiver, 1.0);
            AnchorPane.setBottomAnchor(shadowReceiver, 1.0);

            // Add shadow receiver at index 0 (behind other content)
            ((AnchorPane)root).getChildren().add(0, shadowReceiver);



            // Add subtle inner shadow to barPane for depth perception
            javafx.scene.effect.InnerShadow innerShadow = new javafx.scene.effect.InnerShadow();
            innerShadow.setRadius(2);
            innerShadow.setChoke(0.1);
            innerShadow.setOffsetY(1);
            innerShadow.setColor(Color.rgb(0, 0, 0, 0.1));
            controller.barPane.setEffect(innerShadow);

            // Update clip size when window is resized
            root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
                clip.setWidth(newValue.getWidth());
                clip.setHeight(newValue.getHeight());
                shadowReceiver.setPrefSize(newValue.getWidth() - 2, newValue.getHeight() - 2);
            });

            // Set up for fade-in animation
            root.setOpacity(0);

            // Make window draggable
            controller.barPane.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            controller.barPane.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });

            // Override the close button action
            controller.btnExit.setOnAction(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(event -> stage.close());
                fadeOut.play();
            });

            // Show the form
            stage.centerOnScreen();
            stage.show();

            // Play fade-in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            // Wait for stage to close
            stage.setOnHidden(e -> loadCategories());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error loading category manager: " + e.getMessage());
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
        // In a real app, this would open a camera interface
        // For now, we'll just use the file import dialog
        importImage();
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
        categoryCb.setValue(null);
        costPriceFld.clear();
        markupFld.clear();
        sellingPriceFld.clear();
        stocksSpinner.setValue(0);
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
