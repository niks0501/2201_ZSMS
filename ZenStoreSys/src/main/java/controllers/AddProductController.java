package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.models.spinner.IntegerSpinnerModel;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

        // Setup automatic selling price calculation
        costPriceFld.textProperty().addListener((obs, old, newVal) -> calculateSellingPrice());
        markupFld.textProperty().addListener((obs, old, newVal) -> calculateSellingPrice());

        // Setup button actions
        btnAddProduct.setOnAction(e -> addProduct());
        btnPic.setOnAction(e -> captureImage());
        btnImport.setOnAction(e -> importImage());

        // Initialize the spinner model with IntegerSpinnerModel
        IntegerSpinnerModel spinnerModel = new IntegerSpinnerModel(0);
        stocksSpinner.setSpinnerModel(spinnerModel);
        stocksSpinner.setPromptText("Stocks");


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
