package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.models.spinner.*;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import other_classes.ProductDAO;
import table_models.Category;
import utils.ProductUtils;


import java.io.File;

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

        // Initialize the spinner model with IntegerSpinnerModel
        IntegerSpinnerModel spinnerModel = new IntegerSpinnerModel(10);
        stocksSpinner.setSpinnerModel(spinnerModel);
        stocksSpinner.setPromptText("Stocks");

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

                showAlert(Alert.AlertType.INFORMATION, "Product added successfully!");
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

}
