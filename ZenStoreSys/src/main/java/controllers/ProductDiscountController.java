package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import other_classes.DBConnect;
import table_models.Category;
import table_models.Product;

import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ResourceBundle;

public class ProductDiscountController implements Initializable {

    @FXML
    private Pane barPane;

    @FXML
    private AnchorPane discountMFrame;

    @FXML
    private MFXButton btnAddDisc;

    @FXML
    private MFXButton btnExit;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private Button btnPullSide;

    @FXML
    private MFXComboBox<?> categoryCb;

    @FXML
    private TableColumn<?, ?> categoryColumn;

    @FXML
    private ListView<?> categoryListView;

    @FXML
    private MFXTextField categorySearchFld;

    @FXML
    private TableColumn<?, ?> discountIdColumn;

    @FXML
    private Pane discountPane;

    @FXML
    private TableView<?> discountTbl;

    @FXML
    private MFXComboBox<?> discountTypeCb;

    @FXML
    private TableColumn<?, ?> discountTypeColumn;

    @FXML
    private TableColumn<?, ?> discountValColumn;

    @FXML
    private MFXDatePicker endDate;

    @FXML
    private TableColumn<?, ?> endDateColumn;

    @FXML
    private TableColumn<?, ?> minQtyColumn;

    @FXML
    private MFXTextField minQtyFld;

    @FXML
    private ImageView searchImg1;

    @FXML
    private TableColumn<?, ?> productNameColumn;

    @FXML
    private MFXTextField productSearchFld;

    @FXML
    private ListView<?> productsListView;

    @FXML
    private MFXTextField searchFld;

    @FXML
    private MFXRadioButton showColumnsBtn;

    @FXML
    private MFXDatePicker startDate;

    @FXML
    private TableColumn<?, ?> startDateColumn;

    @FXML
    private TableColumn<?, ?> statusColumn;

    @FXML
    private MFXComboBox<?> productCb;

    @FXML
    private MFXRadioButton toggleByCategory;

    @FXML
    private MFXRadioButton toggleByProduct;

    @FXML
    private MFXTextField valueFld;

    @FXML
    private ImageView searchImg2;

    // Window drag variables
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupWindowControls();
        initializeDataComponents();

        // Set up Add Discount button functionality
        btnAddDisc.setOnAction(event -> addDiscount());
    }

    private void addDiscount() {
        try {
            // Validate inputs
            String discountType = (String) discountTypeCb.getValue();
            if (discountType == null || discountType.isEmpty()) {
                showAlert("Validation Error", "Please select a discount type");
                return;
            }

            String valueText = valueFld.getText();
            if (valueText == null || valueText.trim().isEmpty()) {
                showAlert("Validation Error", "Please enter a discount value");
                return;
            }

            // Parse discount value
            double discountValue;
            try {
                discountValue = Double.parseDouble(valueText);
                if (discountValue <= 0) {
                    showAlert("Validation Error", "Discount value must be greater than zero");
                    return;
                }

                // For percentage, ensure it's not over 100%
                if (discountType.equals("PERCENTAGE") && discountValue > 100) {
                    showAlert("Validation Error", "Percentage discount cannot exceed 100%");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation Error", "Invalid discount value");
                return;
            }

            // Parse minimum quantity
            int minQuantity = 1; // Default value
            String minQtyText = minQtyFld.getText();
            if (minQtyText != null && !minQtyText.trim().isEmpty()) {
                try {
                    minQuantity = Integer.parseInt(minQtyText);
                    if (minQuantity <= 0) {
                        showAlert("Validation Error", "Minimum quantity must be greater than zero");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Validation Error", "Invalid minimum quantity");
                    return;
                }
            }

            // Validate dates
            if (startDate.getValue() == null) {
                showAlert("Validation Error", "Please select a start date");
                return;
            }

            if (endDate.getValue() == null) {
                showAlert("Validation Error", "Please select an end date");
                return;
            }

            if (startDate.getValue().isAfter(endDate.getValue())) {
                showAlert("Validation Error", "Start date must be before or equal to end date");
                return;
            }

            // Determine if we're using product or category discount based on which toggle is selected
            Integer productId = null;
            Integer categoryId = null;

            if (toggleByProduct.isSelected()) {
                Product selectedProduct = (Product) productCb.getValue();
                if (selectedProduct == null) {
                    showAlert("Validation Error", "Please select a product");
                    return;
                }
                productId = selectedProduct.getProductId();
            } else if (toggleByCategory.isSelected()) {
                Category selectedCategory = (Category) categoryCb.getValue();
                if (selectedCategory == null) {
                    showAlert("Validation Error", "Please select a category");
                    return;
                }
                categoryId = selectedCategory.getId();
            } else {
                showAlert("Validation Error", "Please select discount type (Product or Category)");
                return;
            }

            // Convert LocalDate to java.sql.Date
            java.sql.Date sqlStartDate = java.sql.Date.valueOf(startDate.getValue());
            java.sql.Date sqlEndDate = java.sql.Date.valueOf(endDate.getValue());

            // Insert into database
            try (Connection conn = DBConnect.getConnection();
                 CallableStatement stmt = conn.prepareCall("{CALL sp_insert_discount(?, ?, ?, ?, ?, ?, ?, ?)}")) {

                // Set parameters
                stmt.setObject(1, productId);
                stmt.setObject(2, categoryId);
                stmt.setString(3, discountType);
                stmt.setDouble(4, discountValue);
                stmt.setInt(5, minQuantity);
                stmt.setDate(6, sqlStartDate);
                stmt.setDate(7, sqlEndDate);
                stmt.registerOutParameter(8, Types.BOOLEAN);

                // Execute the stored procedure
                stmt.execute();

                // Get result
                boolean success = stmt.getBoolean(8);

                if (success) {
                    // Show success message
                    showSuccessAlert("Success", "Discount added successfully");

                    // Clear form fields
                    clearFormFields();
                } else {
                    showAlert("Error", "Failed to add discount");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Database Error", "Could not add discount: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private void clearFormFields() {
        // Reset form fields
        if (toggleByProduct.isSelected()) {
            productCb.setValue(null);
        } else {
            categoryCb.setValue(null);
        }

        discountTypeCb.setValue(null);
        valueFld.clear();
        minQtyFld.clear();
        startDate.setValue(null);
        endDate.setValue(null);
    }

    private void initializeDataComponents() {
        // Initialize combo boxes and load data
        setupComboBoxes();

        // Initialize list views and load data
        setupListViews();

        // Set up search functionality
        setupSearchFields();
    }

    private void setupSearchFields() {
        // Set up search functionality for products
        MFXTextField productSearch = (MFXTextField) productSearchFld;
        productSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProductListView(newValue);
        });

        // Set up search functionality for categories
        MFXTextField categorySearch = (MFXTextField) categorySearchFld;
        categorySearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCategoryListView(newValue);
        });
    }

    private void loadProductsForComboBox(MFXComboBox<Product> comboBox) {
        try {
            String query = "SELECT product_id, name FROM products ORDER BY product_id ASC";
            try (var conn = other_classes.DBConnect.getConnection();
                 var stmt = conn.prepareStatement(query);
                 var rs = stmt.executeQuery()) {

                comboBox.getItems().clear();

                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");

                    // Create a simplified Product object with just id and name
                    Product product = new Product(id, null, name, null, null, null, 0, null, null);
                    comboBox.getItems().add(product);
                }
            }
        } catch (Exception e) {
            showAlert("Database Error", "Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupComboBoxes() {
        // Type casting for combo boxes
        MFXComboBox<Product> prodCb = (MFXComboBox<Product>) productCb;
        MFXComboBox<Category> catCb = (MFXComboBox<Category>) categoryCb;

        // Setup discount type combo box
        MFXComboBox<String> discTypeCb = (MFXComboBox<String>) discountTypeCb;
        discTypeCb.getItems().addAll("PERCENTAGE", "FIXED", "BOGO", "BULK");

        // Load products for product combo box
        loadProductsForComboBox(prodCb);

        // Load categories for category combo box
        loadCategoriesForComboBox(catCb);

        // Set converters to display only names
        prodCb.setConverter(new StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }

            @Override
            public Product fromString(String string) {
                return null; // Not needed for this implementation
            }
        });

        catCb.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getName() : "";
            }

            @Override
            public Category fromString(String string) {
                return null; // Not needed for this implementation
            }
        });
    }

    private void setupListViews() {
        // Load product list
        loadProductsForListView((ListView<String>) productsListView);

        // Load category list
        loadCategoriesForListView((ListView<String>) categoryListView);

        // Set up listeners for list view selection
        ((ListView<String>) productsListView).getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        // Extract product ID from the list item (format: "ID - Name")
                        int productId = Integer.parseInt(newValue.split(" - ")[0]);

                        // Find matching product in combo box and select it
                        MFXComboBox<Product> prodCb = (MFXComboBox<Product>) productCb;
                        prodCb.getItems().stream()
                                .filter(product -> product.getProductId() == productId)
                                .findFirst()
                                .ifPresent(prodCb::selectItem);
                    }
                }
        );

        ((ListView<String>) categoryListView).getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        // Extract category ID from the list item (format: "ID - Name")
                        int categoryId = Integer.parseInt(newValue.split(" - ")[0]);

                        // Find matching category in combo box and select it
                        MFXComboBox<Category> catCb = (MFXComboBox<Category>) categoryCb;
                        catCb.getItems().stream()
                                .filter(category -> category.getId() == categoryId)
                                .findFirst()
                                .ifPresent(catCb::selectItem);
                    }
                }
        );

        // Set up toggle event handlers
        toggleByProduct.setOnAction(event -> {
            if (toggleByProduct.isSelected()) {
                toggleByCategory.setSelected(false);
                clearFormFields();


                // Create fade out transitions
                FadeTransition fadeOutCategory = new FadeTransition(Duration.millis(350));
                fadeOutCategory.setFromValue(1);
                fadeOutCategory.setToValue(0);
                fadeOutCategory.setNode(categoryListView);
                fadeOutCategory.setInterpolator(Interpolator.EASE_IN);

                FadeTransition fadeOutCategorySearch = new FadeTransition(Duration.millis(350));
                fadeOutCategorySearch.setFromValue(1);
                fadeOutCategorySearch.setToValue(0);
                fadeOutCategorySearch.setNode(categorySearchFld);
                fadeOutCategorySearch.setInterpolator(Interpolator.EASE_IN);

                FadeTransition fadeOutCategoryCb = new FadeTransition(Duration.millis(350));
                fadeOutCategoryCb.setFromValue(1);
                fadeOutCategoryCb.setToValue(0);
                fadeOutCategoryCb.setNode(categoryCb);
                fadeOutCategoryCb.setInterpolator(Interpolator.EASE_IN);

                // Create fade in transitions
                FadeTransition fadeInProducts = new FadeTransition(Duration.millis(350));
                fadeInProducts.setFromValue(0);
                fadeInProducts.setToValue(1);
                fadeInProducts.setNode(productsListView);
                fadeInProducts.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInProductSearch = new FadeTransition(Duration.millis(350));
                fadeInProductSearch.setFromValue(0);
                fadeInProductSearch.setToValue(1);
                fadeInProductSearch.setNode(productSearchFld);
                fadeInProductSearch.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInProductCb = new FadeTransition(Duration.millis(350));
                fadeInProductCb.setFromValue(0);
                fadeInProductCb.setToValue(1);
                fadeInProductCb.setNode(productCb);
                fadeInProductCb.setInterpolator(Interpolator.EASE_OUT);

                // Group fade out and fade in transitions
                ParallelTransition parallelFadeOut = new ParallelTransition(
                        fadeOutCategory, fadeOutCategorySearch, fadeOutCategoryCb
                );

                ParallelTransition parallelFadeIn = new ParallelTransition(
                        fadeInProducts, fadeInProductSearch, fadeInProductCb
                );

                // Create sequence of animations
                SequentialTransition sequence = new SequentialTransition(parallelFadeOut, parallelFadeIn);
                sequence.setOnFinished(e -> {
                    categoryListView.setVisible(false);
                    categorySearchFld.setVisible(false);
                    categoryCb.setVisible(false);

                    productsListView.setVisible(true);
                    productSearchFld.setVisible(true);
                    productCb.setVisible(true);
                });

                sequence.play();
            }
        });

        toggleByCategory.setOnAction(event -> {
            if (toggleByCategory.isSelected()) {
                toggleByProduct.setSelected(false);
                clearFormFields();

                // Create fade out transitions
                FadeTransition fadeOutProducts = new FadeTransition(Duration.millis(350));
                fadeOutProducts.setFromValue(1);
                fadeOutProducts.setToValue(0);
                fadeOutProducts.setNode(productsListView);
                fadeOutProducts.setInterpolator(Interpolator.EASE_IN);

                FadeTransition fadeOutProductSearch = new FadeTransition(Duration.millis(350));
                fadeOutProductSearch.setFromValue(1);
                fadeOutProductSearch.setToValue(0);
                fadeOutProductSearch.setNode(productSearchFld);
                fadeOutProductSearch.setInterpolator(Interpolator.EASE_IN);

                FadeTransition fadeOutProductCb = new FadeTransition(Duration.millis(350));
                fadeOutProductCb.setFromValue(1);
                fadeOutProductCb.setToValue(0);
                fadeOutProductCb.setNode(productCb);
                fadeOutProductCb.setInterpolator(Interpolator.EASE_IN);

                // Create fade in transitions
                FadeTransition fadeInCategory = new FadeTransition(Duration.millis(350));
                fadeInCategory.setFromValue(0);
                fadeInCategory.setToValue(1);
                fadeInCategory.setNode(categoryListView);
                fadeInCategory.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInCategorySearch = new FadeTransition(Duration.millis(350));
                fadeInCategorySearch.setFromValue(0);
                fadeInCategorySearch.setToValue(1);
                fadeInCategorySearch.setNode(categorySearchFld);
                fadeInCategorySearch.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInCategoryCb = new FadeTransition(Duration.millis(350));
                fadeInCategoryCb.setFromValue(0);
                fadeInCategoryCb.setToValue(1);
                fadeInCategoryCb.setNode(categoryCb);
                fadeInCategoryCb.setInterpolator(Interpolator.EASE_OUT);

                // Parallel fade out transitions
                ParallelTransition parallelFadeOut = new ParallelTransition(
                        fadeOutProducts, fadeOutProductSearch, fadeOutProductCb
                );

                // Parallel fade in transitions
                ParallelTransition parallelFadeIn = new ParallelTransition(
                        fadeInCategory, fadeInCategorySearch, fadeInCategoryCb
                );

                // Create sequence of animations
                SequentialTransition sequence = new SequentialTransition(parallelFadeOut, parallelFadeIn);
                sequence.setOnFinished(e -> {
                    productsListView.setVisible(false);
                    productSearchFld.setVisible(false);
                    productCb.setVisible(false);

                    categoryListView.setVisible(true);
                    categorySearchFld.setVisible(true);
                    categoryCb.setVisible(true);
                });

                sequence.play();
            }
        });
    }

    private void loadCategoriesForComboBox(MFXComboBox<Category> comboBox) {
        try {
            String query = "SELECT category_id, category_name FROM categories ORDER BY category_id ASC";
            try (var conn = other_classes.DBConnect.getConnection();
                 var stmt = conn.prepareStatement(query);
                 var rs = stmt.executeQuery()) {

                comboBox.getItems().clear();

                while (rs.next()) {
                    int id = rs.getInt("category_id");
                    String name = rs.getString("category_name");

                    Category category = new Category(id, name);
                    comboBox.getItems().add(category);
                }
            }
        } catch (Exception e) {
            showAlert("Database Error", "Failed to load categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadProductsForListView(ListView<String> listView) {
        try {
            String query = "SELECT product_id, name FROM products ORDER BY product_id ASC";
            try (var conn = other_classes.DBConnect.getConnection();
                 var stmt = conn.prepareStatement(query);
                 var rs = stmt.executeQuery()) {

                // Store original items for filtering
                javafx.collections.ObservableList<String> items = javafx.collections.FXCollections.observableArrayList();

                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");
                    items.add(id + " - " + name);
                }

                listView.setItems(items);
            }
        } catch (Exception e) {
            showAlert("Database Error", "Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategoriesForListView(ListView<String> listView) {
        try {
            String query = "SELECT category_id, category_name FROM categories ORDER BY category_id ASC";
            try (var conn = other_classes.DBConnect.getConnection();
                 var stmt = conn.prepareStatement(query);
                 var rs = stmt.executeQuery()) {

                // Store original items for filtering
                javafx.collections.ObservableList<String> items = javafx.collections.FXCollections.observableArrayList();

                while (rs.next()) {
                    int id = rs.getInt("category_id");
                    String name = rs.getString("category_name");
                    items.add(id + " - " + name);
                }

                listView.setItems(items);
            }
        } catch (Exception e) {
            showAlert("Database Error", "Failed to load categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterProductListView(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            // If search text is empty, reload the original list
            loadProductsForListView((ListView<String>) productsListView);
            return;
        }

        // Convert search text to lowercase for case-insensitive searching
        searchText = searchText.toLowerCase();

        try {
            String query = "SELECT product_id, name FROM products WHERE LOWER(name) LIKE ? ORDER BY product_id ASC";
            try (var conn = other_classes.DBConnect.getConnection();
                 var stmt = conn.prepareStatement(query)) {

                stmt.setString(1, "%" + searchText + "%");

                try (var rs = stmt.executeQuery()) {
                    javafx.collections.ObservableList<String> filteredItems = javafx.collections.FXCollections.observableArrayList();

                    while (rs.next()) {
                        int id = rs.getInt("product_id");
                        String name = rs.getString("name");
                        filteredItems.add(id + " - " + name);
                    }

                    ((ListView<String>) productsListView).setItems(filteredItems);
                }
            }
        } catch (Exception e) {
            showAlert("Search Error", "Failed to search products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterCategoryListView(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            // If search text is empty, reload the original list
            loadCategoriesForListView((ListView<String>) categoryListView);
            return;
        }

        // Convert search text to lowercase for case-insensitive searching
        searchText = searchText.toLowerCase();

        try {
            String query = "SELECT category_id, category_name FROM categories WHERE LOWER(category_name) LIKE ? ORDER BY category_id ASC";
            try (var conn = other_classes.DBConnect.getConnection();
                 var stmt = conn.prepareStatement(query)) {

                stmt.setString(1, "%" + searchText + "%");

                try (var rs = stmt.executeQuery()) {
                    javafx.collections.ObservableList<String> filteredItems = javafx.collections.FXCollections.observableArrayList();

                    while (rs.next()) {
                        int id = rs.getInt("category_id");
                        String name = rs.getString("category_name");
                        filteredItems.add(id + " - " + name);
                    }

                    ((ListView<String>) categoryListView).setItems(filteredItems);
                }
            }
        } catch (Exception e) {
            showAlert("Search Error", "Failed to search categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setupWindowControls() {
        // Exit and minimize functionality
        btnExit.setOnAction(event -> {
            Stage stage = (Stage) btnExit.getScene().getWindow();
            stage.close();
        });

        btnMinimize.setOnAction(event -> {
            Stage stage = (Stage) btnMinimize.getScene().getWindow();
            stage.setIconified(true);
        });

        // Window dragging
        barPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        barPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) barPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        // Set initial visibility state
        discountPane.setVisible(false);
        discountPane.setTranslateX(-discountPane.getPrefWidth());
        toggleByProduct.setVisible(false);
        toggleByCategory.setVisible(false);
        productSearchFld.setVisible(false);
        categoryListView.setVisible(false);
        productsListView.setVisible(false);
        categorySearchFld.setVisible(false);

        // Setup slide animation for the side panel
        setupSlidePanelAnimation();
    }

    private void setupSlidePanelAnimation() {
        btnPullSide.setOnAction(event -> {
            if (!discountPane.isVisible()) {
                // Show discount pane
                discountPane.setVisible(true);

                // Setup slide-in animation
                TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), discountPane);
                slideIn.setFromX(-discountPane.getPrefWidth());
                slideIn.setToX(0);
                slideIn.setInterpolator(Interpolator.EASE_OUT);

                // Setup fade-out animation for table elements
                FadeTransition fadeOutTable = new FadeTransition(Duration.millis(250));
                fadeOutTable.setFromValue(1);
                fadeOutTable.setToValue(0);
                fadeOutTable.setInterpolator(Interpolator.EASE_OUT);
                fadeOutTable.setNode(discountTbl);

                // Setup fade-out for search elements and show columns button
                FadeTransition fadeOutSearchImg1 = new FadeTransition(Duration.millis(250));
                fadeOutSearchImg1.setFromValue(1);
                fadeOutSearchImg1.setToValue(0);
                fadeOutSearchImg1.setNode(searchImg1);

                FadeTransition fadeOutSearchFld = new FadeTransition(Duration.millis(250));
                fadeOutSearchFld.setFromValue(1);
                fadeOutSearchFld.setToValue(0);
                fadeOutSearchFld.setNode(searchFld);

                FadeTransition fadeOutColumns = new FadeTransition(Duration.millis(250));
                fadeOutColumns.setFromValue(1);
                fadeOutColumns.setToValue(0);
                fadeOutColumns.setNode(showColumnsBtn);

                // Create parallel animation for fading out elements
                ParallelTransition fadeOutAll = new ParallelTransition(
                        fadeOutTable, fadeOutSearchImg1, fadeOutSearchFld, fadeOutColumns
                );

                // Create the sequence
                SequentialTransition sequence = new SequentialTransition();
                sequence.getChildren().addAll(fadeOutAll, slideIn);

                sequence.setOnFinished(e -> {
                    // Hide table elements
                    discountTbl.setVisible(false);
                    searchImg1.setVisible(false);
                    searchFld.setVisible(false);
                    showColumnsBtn.setVisible(false);

                    // Show discount panel elements
                    toggleByProduct.setVisible(true);
                    toggleByCategory.setVisible(true);
                    productSearchFld.setVisible(true);
                    productsListView.setVisible(true);

                    // Set initial toggle state
                    toggleByProduct.setSelected(true);
                    toggleByCategory.setSelected(false);
                });

                sequence.play();
            } else {
                // Setup slide-out animation
                TranslateTransition slideOut = new TranslateTransition(Duration.millis(350), discountPane);
                slideOut.setFromX(0);
                slideOut.setToX(-discountPane.getPrefWidth());
                slideOut.setInterpolator(Interpolator.EASE_IN);

                // Hide discount panel elements
                toggleByProduct.setVisible(false);
                toggleByCategory.setVisible(false);
                productSearchFld.setVisible(false);
                productsListView.setVisible(false);
                categoryListView.setVisible(false);

                // Show table elements
                discountTbl.setVisible(true);
                searchImg1.setVisible(true);
                searchImg2.setVisible(true);
                searchFld.setVisible(true);
                showColumnsBtn.setVisible(true);

                // Setup fade-in animation for table elements
                FadeTransition fadeInTable = new FadeTransition(Duration.millis(350));
                fadeInTable.setFromValue(0);
                fadeInTable.setToValue(1);
                fadeInTable.setInterpolator(Interpolator.EASE_OUT);
                fadeInTable.setNode(discountTbl);

                FadeTransition fadeInSearchImg1 = new FadeTransition(Duration.millis(350));
                fadeInSearchImg1.setFromValue(0);
                fadeInSearchImg1.setToValue(1);
                fadeInSearchImg1.setNode(searchImg1);
                fadeInSearchImg1.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInSearchImg2 = new FadeTransition(Duration.millis(350));
                fadeInSearchImg2.setFromValue(0);
                fadeInSearchImg2.setToValue(1);
                fadeInSearchImg2.setNode(searchImg2);
                fadeInSearchImg2.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInSearchFld = new FadeTransition(Duration.millis(350));
                fadeInSearchFld.setFromValue(0);
                fadeInSearchFld.setToValue(1);
                fadeInSearchFld.setNode(searchFld);
                fadeInSearchFld.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInColumns = new FadeTransition(Duration.millis(350));
                fadeInColumns.setFromValue(0);
                fadeInColumns.setToValue(1);
                fadeInColumns.setNode(showColumnsBtn);
                fadeInColumns.setInterpolator(Interpolator.EASE_OUT);

                // Create parallel animation for fading in elements
                ParallelTransition fadeInAll = new ParallelTransition(
                        fadeInTable, fadeInSearchImg1, fadeInSearchImg2, fadeInSearchFld, fadeInColumns
                );

                // Create the sequence
                SequentialTransition sequence = new SequentialTransition(slideOut);
                sequence.getChildren().add(fadeInAll);

                sequence.setOnFinished(e -> {
                    discountPane.setVisible(false);
                });

                sequence.play();
            }
        });

        // Toggle behavior between products and categories remains the same
        toggleByProduct.setOnAction(event -> {
            if (toggleByProduct.isSelected()) {
                toggleByCategory.setSelected(false);

                // Transition between list views
                FadeTransition fadeOutCategory = new FadeTransition(Duration.millis(250));
                fadeOutCategory.setFromValue(categoryListView.isVisible() ? 1 : 0);
                fadeOutCategory.setToValue(0);
                fadeOutCategory.setNode(categoryListView);
                fadeOutCategory.setInterpolator(Interpolator.EASE_IN);

                // Fade out category search field and combo box
                FadeTransition fadeOutCategorySearch = new FadeTransition(Duration.millis(250));
                fadeOutCategorySearch.setFromValue(categorySearchFld.isVisible() ? 1 : 0);
                fadeOutCategorySearch.setToValue(0);
                fadeOutCategorySearch.setNode(categorySearchFld);
                fadeOutCategorySearch.setInterpolator(Interpolator.EASE_IN);

                FadeTransition fadeOutCategoryCb = new FadeTransition(Duration.millis(250));
                fadeOutCategoryCb.setFromValue(categoryCb.isVisible() ? 1 : 0);
                fadeOutCategoryCb.setToValue(0);
                fadeOutCategoryCb.setNode(categoryCb);
                fadeOutCategoryCb.setInterpolator(Interpolator.EASE_IN);

                // Parallel fade out transitions
                ParallelTransition parallelFadeOut = new ParallelTransition(
                        fadeOutCategory, fadeOutCategorySearch, fadeOutCategoryCb
                );

                // Fade in products list view and related elements
                FadeTransition fadeInProducts = new FadeTransition(Duration.millis(350));
                fadeInProducts.setFromValue(0);
                fadeInProducts.setToValue(1);
                fadeInProducts.setNode(productsListView);
                fadeInProducts.setInterpolator(Interpolator.EASE_OUT);

                // Fade in product search field and combo box
                FadeTransition fadeInProductSearch = new FadeTransition(Duration.millis(350));
                fadeInProductSearch.setFromValue(0);
                fadeInProductSearch.setToValue(1);
                fadeInProductSearch.setNode(productSearchFld);
                fadeInProductSearch.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInProductCb = new FadeTransition(Duration.millis(350));
                fadeInProductCb.setFromValue(0);
                fadeInProductCb.setToValue(1);
                fadeInProductCb.setNode(productCb);
                fadeInProductCb.setInterpolator(Interpolator.EASE_OUT);

                // Parallel fade in transitions
                ParallelTransition parallelFadeIn = new ParallelTransition(
                        fadeInProducts, fadeInProductSearch, fadeInProductCb
                );

                // Create sequence of animations
                SequentialTransition sequence = new SequentialTransition(parallelFadeOut, parallelFadeIn);
                sequence.setOnFinished(e -> {
                    categoryListView.setVisible(false);
                    categorySearchFld.setVisible(false);
                    categoryCb.setVisible(false);

                    productsListView.setVisible(true);
                    productSearchFld.setVisible(true);
                    productCb.setVisible(true);
                });

                sequence.play();
            }
        });

        toggleByCategory.setOnAction(event -> {
            if (toggleByCategory.isSelected()) {
                toggleByProduct.setSelected(false);

                // Transition between list views
                FadeTransition fadeOutProducts = new FadeTransition(Duration.millis(250));
                fadeOutProducts.setFromValue(productsListView.isVisible() ? 1 : 0);
                fadeOutProducts.setToValue(0);
                fadeOutProducts.setNode(productsListView);
                fadeOutProducts.setInterpolator(Interpolator.EASE_IN);

                // Fade out product search field and combo box
                FadeTransition fadeOutProductSearch = new FadeTransition(Duration.millis(250));
                fadeOutProductSearch.setFromValue(productSearchFld.isVisible() ? 1 : 0);
                fadeOutProductSearch.setToValue(0);
                fadeOutProductSearch.setNode(productSearchFld);
                fadeOutProductSearch.setInterpolator(Interpolator.EASE_IN);

                FadeTransition fadeOutProductCb = new FadeTransition(Duration.millis(250));
                fadeOutProductCb.setFromValue(productCb.isVisible() ? 1 : 0);
                fadeOutProductCb.setToValue(0);
                fadeOutProductCb.setNode(productCb);
                fadeOutProductCb.setInterpolator(Interpolator.EASE_IN);

                // Parallel fade out transitions
                ParallelTransition parallelFadeOut = new ParallelTransition(
                        fadeOutProducts, fadeOutProductSearch, fadeOutProductCb
                );

                // Fade in category list view and related elements
                FadeTransition fadeInCategory = new FadeTransition(Duration.millis(350));
                fadeInCategory.setFromValue(0);
                fadeInCategory.setToValue(1);
                fadeInCategory.setNode(categoryListView);
                fadeInCategory.setInterpolator(Interpolator.EASE_OUT);

                // Fade in category search field and combo box
                FadeTransition fadeInCategorySearch = new FadeTransition(Duration.millis(350));
                fadeInCategorySearch.setFromValue(0);
                fadeInCategorySearch.setToValue(1);
                fadeInCategorySearch.setNode(categorySearchFld);
                fadeInCategorySearch.setInterpolator(Interpolator.EASE_OUT);

                FadeTransition fadeInCategoryCb = new FadeTransition(Duration.millis(350));
                fadeInCategoryCb.setFromValue(0);
                fadeInCategoryCb.setToValue(1);
                fadeInCategoryCb.setNode(categoryCb);
                fadeInCategoryCb.setInterpolator(Interpolator.EASE_OUT);

                // Parallel fade in transitions
                ParallelTransition parallelFadeIn = new ParallelTransition(
                        fadeInCategory, fadeInCategorySearch, fadeInCategoryCb
                );

                // Create sequence of animations
                SequentialTransition sequence = new SequentialTransition(parallelFadeOut, parallelFadeIn);
                sequence.setOnFinished(e -> {
                    productsListView.setVisible(false);
                    productSearchFld.setVisible(false);
                    productCb.setVisible(false);

                    categoryListView.setVisible(true);
                    categorySearchFld.setVisible(true);
                    categoryCb.setVisible(true);
                });

                sequence.play();
            }
        });
    }

    private void showSuccessAlert(String title, String message) {
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

        // Get parent pane to add notification to
        Parent parent = discountMFrame.getScene().getRoot();
        if (parent instanceof Pane) {
            Pane parentPane = (Pane) parent;

            // Position the notification
            notification.setLayoutX(parentPane.getWidth() - notificationWidth - 20);
            notification.setLayoutY(parentPane.getHeight() - notificationHeight - 20);

            // Add to scene and animate
            parentPane.getChildren().add(notification);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(2.5), notification);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(1.5));
            fadeOut.setOnFinished(e -> parentPane.getChildren().remove(notification));
            fadeOut.play();
        }
    }
}
