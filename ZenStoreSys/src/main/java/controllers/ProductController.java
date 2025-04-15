package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import other_classes.ProductDAO;
import services.ProductLoadService;
import table_models.Product;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

public class ProductController {

    @FXML
    private TableColumn<Product, Button> actionColumn;

    @FXML
    private TableColumn<Product, String> barcodeColumn;

    @FXML
    private Button btnPopup;

    @FXML
    private TableColumn<Product, String> categoryColumn;

    @FXML
    private TableColumn<Product, BigDecimal> costPriceColumn;

    @FXML
    private TableColumn<Product, BigDecimal> markupColumn;

    @FXML
    private Pane prodContentPane;

    @FXML
    private StackPane prodMainFrame;

    @FXML
    private TableColumn<Product, ImageView> productImgColumn;

    @FXML
    private TableColumn<Product, String> productNameColumn;

    @FXML
    private TableView<Product> productTbl;

    @FXML
    private Pagination productTblPage;

    @FXML
    private MFXTextField searchFld;

    @FXML
    private TableColumn<Product, BigDecimal> sellingPriceColumn;

    @FXML
    private MFXComboBox<?> sortTbl;

    @FXML
    private TableColumn<Product, Integer> stocksColumn;

    @FXML
    private StackPane popupPane;

    private Parent addProductForm;
    private boolean isFormVisible = false;
    private double initialYPosition = -1; // Default: auto-calculated
    private double finalYPosition = 250;    // Default: top of container
    private ProductLoadService productLoadService;
    private ObservableList<Product> allProducts;

    @FXML
    public void initialize() {
        // Hide popupPane initially
        popupPane.setVisible(false);
        popupPane.setOpacity(0);

        // Store original button position and parent for later use
        btnPopup.setUserData(new double[]{
                btnPopup.getLayoutX(),
                btnPopup.getLayoutY()
        });

        loadAddProductForm();

        // Button click handler
        btnPopup.setOnAction(event -> toggleAddProductForm());

        // Set up table columns
        setupTableColumns();

        // Initialize product load service
        productLoadService = ProductLoadService.getInstance();

        // Set up service success handler
        productLoadService.setOnSucceeded(event -> {
            allProducts = productLoadService.getValue();
            initializePagination(allProducts);
        });

        // Set up service failure handler
        productLoadService.setOnFailed(event -> {
            Throwable exception = productLoadService.getException();
            System.err.println("Failed to load products: " + exception.getMessage());
            // Display error to user if needed
        });

        // Start loading products
        productLoadService.reloadProducts();
    }

    private void loadAddProductForm(){
        // Load the add-product form
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/add-product.fxml"));
            addProductForm = loader.load();
            addProductForm.getStylesheets().add(getClass().getResource("/css/add-product.css").toExternalForm());

            // Get controller and set reference to this ProductController
            AddProductController addProductController = loader.getController();
            addProductController.setProductController(this);

            // Position form initially off-screen
            addProductForm.setTranslateY(getInitialYPosition());

            // Add form to popupPane
            popupPane.getChildren().add(addProductForm);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        productImgColumn.setCellValueFactory(new PropertyValueFactory<>("productImage"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        costPriceColumn.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        markupColumn.setCellValueFactory(new PropertyValueFactory<>("markupPercentage"));
        sellingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        stocksColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("barcodeImage"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("actionButton"));
    }

    private void initializePagination(ObservableList<Product> productList) {
        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil((double) productList.size() / itemsPerPage);

        // Set page count for standard JavaFX Pagination
        productTblPage.setPageCount(Math.max(1, totalPages));

        // Set up page factory
        productTblPage.setPageFactory(pageIndex -> {
            updateTableForPage(productList, pageIndex, itemsPerPage);
            return new Pane();
        });
    }

    private void updateTableForPage(ObservableList<Product> productList, int pageIndex, int itemsPerPage) {
        int fromIndex = pageIndex * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, productList.size());

        if (fromIndex >= productList.size()) {
            productTbl.setItems(FXCollections.emptyObservableList());
        } else {
            productTbl.setItems(FXCollections.observableArrayList(
                    productList.subList(fromIndex, toIndex)
            ));
        }
    }

    // Method to refresh products table - can be called from other controllers
    public void refreshProductTable() {
        productLoadService.reloadProducts();
    }

    private double getInitialYPosition() {
        // If manually set, use that value, otherwise auto-calculate
        if (initialYPosition >= 0) {
            return initialYPosition;
        } else if (addProductForm != null) {
            return addProductForm.getBoundsInLocal().getHeight();
        }
        return 400; // Fallback default
    }

    private void toggleAddProductForm() {
        if (isFormVisible) {
            hideAddProductForm();
        } else {
            showAddProductForm();
        }
    }


    private void showAddProductForm() {
        // Make popupPane visible
        popupPane.setVisible(true);

        // Calculate more reasonable button position (just above the form)
        double buttonY = finalYPosition - 652; // Much smaller offset

        // Create form animation (slide up)
        TranslateTransition slideUpForm = new TranslateTransition(Duration.millis(350), addProductForm);
        slideUpForm.setToY(finalYPosition);
        slideUpForm.setInterpolator(Interpolator.EASE_OUT);

        // Create fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), popupPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_IN);

        // Create button animation
        TranslateTransition slideUpButton = new TranslateTransition(Duration.millis(350), btnPopup);
        slideUpButton.setToY(buttonY);
        slideUpButton.setInterpolator(Interpolator.EASE_OUT);

        // Make sure button is clickable
        btnPopup.toFront(); // More reliable than setViewOrder
        btnPopup.setMouseTransparent(false);

        // Make sure popupPane doesn't block clicks to the button
        popupPane.setPickOnBounds(false);

        // Play animations
        ParallelTransition parallelTransition = new ParallelTransition(slideUpForm, fadeIn, slideUpButton);
        parallelTransition.setOnFinished(event -> {
            isFormVisible = true;
            btnPopup.toFront(); // Ensure it stays in front after animation
        });
        parallelTransition.play();
    }

    private void hideAddProductForm() {
        // Use a direct value instead of relying on getUserData()
        // This assumes btnPopup has a defined layout position in the FXML
        double originalY = 0; // Default position - adjust as needed for your layout

        // Create form animation
        TranslateTransition slideDownForm = new TranslateTransition(Duration.millis(350), addProductForm);
        slideDownForm.setToY(getInitialYPosition());
        slideDownForm.setInterpolator(Interpolator.EASE_IN);

        // Create button animation
        TranslateTransition slideDownButton = new TranslateTransition(Duration.millis(350), btnPopup);
        slideDownButton.setToY(originalY);
        slideDownButton.setInterpolator(Interpolator.EASE_IN);

        // Create fade-out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), popupPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_OUT);

        // Play animations
        ParallelTransition parallelTransition = new ParallelTransition(slideDownForm, slideDownButton, fadeOut);
        parallelTransition.setOnFinished(event -> {
            popupPane.setVisible(false);
            isFormVisible = false;
        });
        parallelTransition.play();
    }

}
