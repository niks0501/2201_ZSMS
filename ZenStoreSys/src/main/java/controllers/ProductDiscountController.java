package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import other_classes.DBConnect;
import table_models.Category;
import table_models.Discount;
import table_models.Product;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private MFXComboBox<Category> categoryCb;

    @FXML
    private TableColumn<Discount, String> categoryColumn;

    @FXML
    private ListView<?> categoryListView;

    @FXML
    private MFXTextField categorySearchFld;

    @FXML
    private TableColumn<Discount, Integer> discountIdColumn;

    @FXML
    private Pane discountPane;

    @FXML
    private TableView<Discount> discountTbl;

    @FXML
    private MFXComboBox<String> discountTypeCb;

    @FXML
    private TableColumn<Discount, String> discountTypeColumn;

    @FXML
    private TableColumn<Discount, BigDecimal> discountValColumn;

    @FXML
    private MFXDatePicker endDate;

    @FXML
    private TableColumn<Discount, LocalDate> endDateColumn;

    @FXML
    private TableColumn<Discount, Integer> minQtyColumn;

    @FXML
    private MFXTextField minQtyFld;

    @FXML
    private ImageView searchImg1;

    @FXML
    private TableColumn<Discount, String> productNameColumn;

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
    private TableColumn<Discount, LocalDate> startDateColumn;

    @FXML
    private TableColumn<Discount, String> statusColumn;

    @FXML
    private MFXComboBox<Product> productCb;

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
        setupShowColumnsBtn();

        // Set up Add Discount button functionality
        btnAddDisc.setOnAction(event -> addDiscount());

        // Initialize the discounts table
        setupDiscountsTable();
        loadDiscounts();
    }

    private void setupDiscountsTable() {
        discountIdColumn.setCellValueFactory(new PropertyValueFactory<>("discountId"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        discountTypeColumn.setCellValueFactory(new PropertyValueFactory<>("discountType"));
        discountValColumn.setCellValueFactory(new PropertyValueFactory<>("discountValue"));
        minQtyColumn.setCellValueFactory(new PropertyValueFactory<>("minQuantity"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusText"));

        // Format date columns
        startDateColumn.setCellFactory(column -> new TableCell<Discount, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                }
            }
        });

        endDateColumn.setCellFactory(column -> new TableCell<Discount, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                }
            }
        });

        // Style the status column
        statusColumn.setCellFactory(column -> new TableCell<Discount, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equals("Active")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void loadDiscounts() {
        ObservableList<Discount> discounts = FXCollections.observableArrayList();

        try (Connection conn = DBConnect.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "SELECT d.discount_id, d.product_id, p.name AS product_name, " +
                             "d.category_id, c.category_name, d.discount_type, d.discount_value, " +
                             "d.min_quantity, d.start_date, d.end_date, d.is_active " +
                             "FROM discounts d " +
                             "LEFT JOIN products p ON d.product_id = p.product_id " +
                             "LEFT JOIN categories c ON d.category_id = c.category_id " +
                             "ORDER BY d.discount_id ASC")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                discounts.add(new Discount(
                        rs.getInt("discount_id"),
                        rs.getObject("product_id") != null ? rs.getInt("product_id") : null,
                        rs.getString("product_name"),
                        rs.getObject("category_id") != null ? rs.getInt("category_id") : null,
                        rs.getString("category_name"),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("discount_value"),
                        rs.getInt("min_quantity"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getBoolean("is_active")
                ));
            }

            discountTbl.setItems(discounts);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load discounts: " + e.getMessage());
        }
    }

    private void addDiscount() {
        try {
            // Get discount type
            String discountType = discountTypeCb.getValue();
            if (discountType == null) {
                showAlert("Validation Error", "Please select a discount type");
                return;
            }

            // Validate discount value
            String valueText = valueFld.getText();
            if (valueText == null || valueText.trim().isEmpty()) {
                showAlert("Validation Error", "Please enter a discount value");
                return;
            }

            BigDecimal discountValue;
            try {
                discountValue = new BigDecimal(valueText);
                if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Validation Error", "Discount value must be greater than zero");
                    return;
                }

                // For percentage discounts, ensure value is <= 100
                if (discountType.equals("PERCENTAGE") && discountValue.compareTo(new BigDecimal("100")) > 0) {
                    showAlert("Validation Error", "Percentage discount cannot exceed 100%");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation Error", "Please enter a valid numeric discount value");
                return;
            }

            // Validate min quantity
            int minQuantity = 1; // Default value
            String minQtyText = minQtyFld.getText();
            if (minQtyText != null && !minQtyText.trim().isEmpty()) {
                try {
                    minQuantity = Integer.parseInt(minQtyText);
                    if (minQuantity < 1) {
                        showAlert("Validation Error", "Minimum quantity must be at least 1");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Validation Error", "Please enter a valid integer for minimum quantity");
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
                showAlert("Validation Error", "Start date cannot be after end date");
                return;
            }

            // Determine if we're using product or category discount
            Integer productId = null;
            Integer categoryId = null;

            if (toggleByProduct.isSelected()) {
                Product selectedProduct = productCb.getValue();
                if (selectedProduct == null) {
                    showAlert("Validation Error", "Please select a product");
                    return;
                }
                productId = selectedProduct.getProductId();
            } else if (toggleByCategory.isSelected()) {
                Category selectedCategory = categoryCb.getValue();
                if (selectedCategory == null) {
                    showAlert("Validation Error", "Please select a category");
                    return;
                }
                categoryId = selectedCategory.getId();
            } else {
                showAlert("Validation Error", "Please select either product or category mode");
                return;
            }

            // Convert LocalDate to java.sql.Date
            java.sql.Date sqlStartDate = java.sql.Date.valueOf(startDate.getValue());
            java.sql.Date sqlEndDate = java.sql.Date.valueOf(endDate.getValue());

            // Insert into database using stored procedure
            try (Connection conn = DBConnect.getConnection();
                 CallableStatement cstmt = conn.prepareCall("{CALL sp_insert_discount(?, ?, ?, ?, ?, ?, ?, ?)}")) {

                cstmt.setObject(1, productId);
                cstmt.setObject(2, categoryId);
                cstmt.setString(3, discountType);
                cstmt.setBigDecimal(4, discountValue);
                cstmt.setInt(5, minQuantity);
                cstmt.setDate(6, sqlStartDate);
                cstmt.setDate(7, sqlEndDate);
                cstmt.registerOutParameter(8, Types.BOOLEAN);

                cstmt.execute();

                boolean success = cstmt.getBoolean(8);

                if (success) {
                    // Show success notification
                    showSuccessAlert("", "Discount added successfully!");

                    // Clear form fields
                    clearFormFields();

                    // Reload discounts table
                    loadDiscounts();
                } else {
                    showAlert("Error", "Failed to add discount");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Database error: " + e.getMessage());
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

        // Add listener for the main search field
        searchFld.textProperty().addListener((observable, oldValue, newValue) -> {
            filterDiscountTable(newValue);
        });
    }

    private void filterDiscountTable(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            // If search is empty, reset the table and show all columns
            loadDiscounts();
            resetColumnVisibility();
            return;
        }

        // Handle special keywords for column visibility
        if (searchText.equalsIgnoreCase("Product")) {
            productNameColumn.setVisible(true);
            categoryColumn.setVisible(false);
            loadDiscounts(); // Reload all discounts but with modified columns
            return;
        } else if (searchText.equalsIgnoreCase("Category")) {
            productNameColumn.setVisible(false);
            categoryColumn.setVisible(true);
            loadDiscounts(); // Reload all discounts but with modified columns
            return;
        } else {
            // Reset columns for other searches
            resetColumnVisibility();
        }

        // Filter the table based on search text
        ObservableList<Discount> allDiscounts = FXCollections.observableArrayList();
        ObservableList<Discount> filteredDiscounts = FXCollections.observableArrayList();

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT d.discount_id, d.product_id, p.name AS product_name, " +
                             "d.category_id, c.category_name, d.discount_type, d.discount_value, " +
                             "d.min_quantity, d.start_date, d.end_date, d.is_active " +
                             "FROM discounts d " +
                             "LEFT JOIN products p ON d.product_id = p.product_id " +
                             "LEFT JOIN categories c ON d.category_id = c.category_id " +
                             "ORDER BY d.discount_id ASC")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Discount discount = new Discount(
                        rs.getInt("discount_id"),
                        rs.getObject("product_id") != null ? rs.getInt("product_id") : null,
                        rs.getString("product_name"),
                        rs.getObject("category_id") != null ? rs.getInt("category_id") : null,
                        rs.getString("category_name"),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("discount_value"),
                        rs.getInt("min_quantity"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getBoolean("is_active")
                );
                allDiscounts.add(discount);
            }

            // Filter the discounts based on search text
            String lowerCaseSearch = searchText.toLowerCase();
            for (Discount discount : allDiscounts) {
                // Check if any field matches the search text
                boolean matchesProduct = discount.getProductName() != null &&
                        discount.getProductName().toLowerCase().contains(lowerCaseSearch);
                boolean matchesCategory = discount.getCategoryName() != null &&
                        discount.getCategoryName().toLowerCase().contains(lowerCaseSearch);
                boolean matchesType = discount.getDiscountType() != null &&
                        discount.getDiscountType().toLowerCase().contains(lowerCaseSearch);

                if (matchesProduct || matchesCategory || matchesType) {
                    filteredDiscounts.add(discount);
                }
            }

            discountTbl.setItems(filteredDiscounts);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to filter discounts: " + e.getMessage());
        }
    }

    private void resetColumnVisibility() {
        // Reset all columns to their default visibility
        productNameColumn.setVisible(true);
        categoryColumn.setVisible(true);
    }

    private void setupShowColumnsBtn() {
        showColumnsBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
            discountValColumn.setVisible(newValue);
            minQtyColumn.setVisible(newValue);
            startDateColumn.setVisible(newValue);
            endDateColumn.setVisible(newValue);
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
                FadeTransition fadeOutCategory = new FadeTransition(Duration.millis(150));
                fadeOutCategory.setFromValue(1);
                fadeOutCategory.setToValue(0);
                fadeOutCategory.setNode(categoryListView);
                fadeOutCategory.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeOutCategorySearch = new FadeTransition(Duration.millis(150));
                fadeOutCategorySearch.setFromValue(1);
                fadeOutCategorySearch.setToValue(0);
                fadeOutCategorySearch.setNode(categorySearchFld);
                fadeOutCategorySearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeOutCategoryCb = new FadeTransition(Duration.millis(150));
                fadeOutCategoryCb.setFromValue(1);
                fadeOutCategoryCb.setToValue(0);
                fadeOutCategoryCb.setNode(categoryCb);
                fadeOutCategoryCb.setInterpolator(Interpolator.EASE_BOTH);

                // Create fade in transitions
                FadeTransition fadeInProducts = new FadeTransition(Duration.millis(150));
                fadeInProducts.setFromValue(0);
                fadeInProducts.setToValue(1);
                fadeInProducts.setNode(productsListView);
                fadeInProducts.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeInProductSearch = new FadeTransition(Duration.millis(150));
                fadeInProductSearch.setFromValue(0);
                fadeInProductSearch.setToValue(1);
                fadeInProductSearch.setNode(productSearchFld);
                fadeInProductSearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeInProductCb = new FadeTransition(Duration.millis(150));
                fadeInProductCb.setFromValue(0);
                fadeInProductCb.setToValue(1);
                fadeInProductCb.setNode(productCb);
                fadeInProductCb.setInterpolator(Interpolator.EASE_BOTH);

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
                FadeTransition fadeOutProducts = new FadeTransition(Duration.millis(150));
                fadeOutProducts.setFromValue(1);
                fadeOutProducts.setToValue(0);
                fadeOutProducts.setNode(productsListView);
                fadeOutProducts.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeOutProductSearch = new FadeTransition(Duration.millis(150));
                fadeOutProductSearch.setFromValue(1);
                fadeOutProductSearch.setToValue(0);
                fadeOutProductSearch.setNode(productSearchFld);
                fadeOutProductSearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeOutProductCb = new FadeTransition(Duration.millis(150));
                fadeOutProductCb.setFromValue(1);
                fadeOutProductCb.setToValue(0);
                fadeOutProductCb.setNode(productCb);
                fadeOutProductCb.setInterpolator(Interpolator.EASE_BOTH);

                // Create fade in transitions
                FadeTransition fadeInCategory = new FadeTransition(Duration.millis(150));
                fadeInCategory.setFromValue(0);
                fadeInCategory.setToValue(1);
                fadeInCategory.setNode(categoryListView);
                fadeInCategory.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeInCategorySearch = new FadeTransition(Duration.millis(150));
                fadeInCategorySearch.setFromValue(0);
                fadeInCategorySearch.setToValue(1);
                fadeInCategorySearch.setNode(categorySearchFld);
                fadeInCategorySearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeInCategoryCb = new FadeTransition(Duration.millis(150));
                fadeInCategoryCb.setFromValue(0);
                fadeInCategoryCb.setToValue(1);
                fadeInCategoryCb.setNode(categoryCb);
                fadeInCategoryCb.setInterpolator(Interpolator.EASE_BOTH);

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
                // Prepare to show discount pane
                discountPane.setVisible(true);

                // Reset opacities for fade-in elements
                toggleByProduct.setOpacity(0);
                toggleByCategory.setOpacity(0);
                productSearchFld.setOpacity(0);
                categorySearchFld.setOpacity(0);
                productsListView.setOpacity(0);
                categoryListView.setOpacity(0);
                searchImg2.setOpacity(0);

                // Setup slide-in animation
                TranslateTransition slideIn = new TranslateTransition(Duration.millis(150), discountPane);
                slideIn.setFromX(-discountPane.getPrefWidth());
                slideIn.setToX(0);
                slideIn.setInterpolator(Interpolator.EASE_BOTH);

                // Setup fade-out for table elements
                FadeTransition fadeOutTable = new FadeTransition(Duration.millis(150));
                fadeOutTable.setFromValue(1);
                fadeOutTable.setToValue(0);
                fadeOutTable.setNode(discountTbl);

                FadeTransition fadeOutSearchImg1 = new FadeTransition(Duration.millis(250));
                fadeOutSearchImg1.setFromValue(1);
                fadeOutSearchImg1.setToValue(0);
                fadeOutSearchImg1.setNode(searchImg1);

                FadeTransition fadeOutSearchImg2 = new FadeTransition(Duration.millis(250));
                fadeOutSearchImg2.setFromValue(1);
                fadeOutSearchImg2.setToValue(0);
                fadeOutSearchImg2.setNode(searchImg2);

                FadeTransition fadeOutSearchFld = new FadeTransition(Duration.millis(250));
                fadeOutSearchFld.setFromValue(1);
                fadeOutSearchFld.setToValue(0);
                fadeOutSearchFld.setNode(searchFld);

                FadeTransition fadeOutColumns = new FadeTransition(Duration.millis(250));
                fadeOutColumns.setFromValue(1);
                fadeOutColumns.setToValue(0);
                fadeOutColumns.setNode(showColumnsBtn);

                // Parallel fade-out for table elements
                ParallelTransition fadeOutAll = new ParallelTransition(
                        fadeOutTable, fadeOutSearchImg1, fadeOutSearchImg2, fadeOutSearchFld, fadeOutColumns
                );

                // Setup fade-in for discount pane elements
                FadeTransition fadeInToggleProduct = new FadeTransition(Duration.millis(150));
                fadeInToggleProduct.setFromValue(0);
                fadeInToggleProduct.setToValue(1);
                fadeInToggleProduct.setNode(toggleByProduct);

                FadeTransition fadeInToggleCategory = new FadeTransition(Duration.millis(150));
                fadeInToggleCategory.setFromValue(0);
                fadeInToggleCategory.setToValue(1);
                fadeInToggleCategory.setNode(toggleByCategory);

                FadeTransition fadeInProductSearch = new FadeTransition(Duration.millis(150));
                fadeInProductSearch.setFromValue(0);
                fadeInProductSearch.setToValue(1);
                fadeInProductSearch.setNode(productSearchFld);

                FadeTransition fadeInCategorySearch = new FadeTransition(Duration.millis(150));
                fadeInCategorySearch.setFromValue(0);
                fadeInCategorySearch.setToValue(1);
                fadeInCategorySearch.setNode(categorySearchFld);

                FadeTransition fadeInProductsList = new FadeTransition(Duration.millis(150));
                fadeInProductsList.setFromValue(0);
                fadeInProductsList.setToValue(1);
                fadeInProductsList.setNode(productsListView);

                FadeTransition fadeInCategoryList = new FadeTransition(Duration.millis(150));
                fadeInCategoryList.setFromValue(0);
                fadeInCategoryList.setToValue(1);
                fadeInCategoryList.setNode(categoryListView);

                FadeTransition fadeInSearchImg2 = new FadeTransition(Duration.millis(150));
                fadeInSearchImg2.setFromValue(0);
                fadeInSearchImg2.setToValue(1);
                fadeInSearchImg2.setNode(searchImg2);

                // Parallel fade-in for discount pane elements
                ParallelTransition fadeInAll = new ParallelTransition(
                        fadeInToggleProduct, fadeInToggleCategory, fadeInProductSearch,
                        fadeInCategorySearch, fadeInProductsList, fadeInCategoryList, fadeInSearchImg2
                );

                // Sequence: fade out table, slide in pane, fade in discount elements
                SequentialTransition sequence = new SequentialTransition(fadeOutAll, slideIn, fadeInAll);

                sequence.setOnFinished(e -> {
                    // Hide table elements
                    discountTbl.setVisible(false);
                    searchImg1.setVisible(false);
                    searchImg2.setVisible(false);
                    searchFld.setVisible(false);
                    showColumnsBtn.setVisible(false);

                    // Show discount pane elements
                    toggleByProduct.setVisible(true);
                    toggleByCategory.setVisible(true);
                    productSearchFld.setVisible(true);
                    categorySearchFld.setVisible(true);
                    productsListView.setVisible(true);
                    categoryListView.setVisible(true);
                    searchImg2.setVisible(true);

                    // Set initial toggle state
                    toggleByProduct.setSelected(true);
                    toggleByCategory.setSelected(false);
                });

                sequence.play();
            } else {
                // Reset opacities for fade-in elements
                discountTbl.setOpacity(0);
                searchImg1.setOpacity(0);
                searchImg2.setOpacity(0);
                searchFld.setOpacity(0);
                showColumnsBtn.setOpacity(0);

                // Setup slide-out animation
                TranslateTransition slideOut = new TranslateTransition(Duration.millis(150), discountPane);
                slideOut.setFromX(0);
                slideOut.setToX(-discountPane.getPrefWidth());
                slideOut.setInterpolator(Interpolator.EASE_BOTH);

                // Setup fade-out for discount pane elements
                FadeTransition fadeOutToggleProduct = new FadeTransition(Duration.millis(150));
                fadeOutToggleProduct.setFromValue(1);
                fadeOutToggleProduct.setToValue(0);
                fadeOutToggleProduct.setNode(toggleByProduct);

                FadeTransition fadeOutToggleCategory = new FadeTransition(Duration.millis(250));
                fadeOutToggleCategory.setFromValue(1);
                fadeOutToggleCategory.setToValue(0);
                fadeOutToggleCategory.setNode(toggleByCategory);

                FadeTransition fadeOutProductSearch = new FadeTransition(Duration.millis(250));
                fadeOutProductSearch.setFromValue(1);
                fadeOutProductSearch.setToValue(0);
                fadeOutProductSearch.setNode(productSearchFld);

                FadeTransition fadeOutCategorySearch = new FadeTransition(Duration.millis(250));
                fadeOutCategorySearch.setFromValue(1);
                fadeOutCategorySearch.setToValue(0);
                fadeOutCategorySearch.setNode(categorySearchFld);

                FadeTransition fadeOutProductsList = new FadeTransition(Duration.millis(250));
                fadeOutProductsList.setFromValue(1);
                fadeOutProductsList.setToValue(0);
                fadeOutProductsList.setNode(productsListView);

                FadeTransition fadeOutCategoryList = new FadeTransition(Duration.millis(250));
                fadeOutCategoryList.setFromValue(1);
                fadeOutCategoryList.setToValue(0);
                fadeOutCategoryList.setNode(categoryListView);

                FadeTransition fadeOutSearchImg2 = new FadeTransition(Duration.millis(250));
                fadeOutSearchImg2.setFromValue(1);
                fadeOutSearchImg2.setToValue(0);
                fadeOutSearchImg2.setNode(searchImg2);

                // Parallel fade-out for discount pane elements
                ParallelTransition fadeOutAll = new ParallelTransition(
                        fadeOutToggleProduct, fadeOutToggleCategory, fadeOutProductSearch,
                        fadeOutCategorySearch, fadeOutProductsList, fadeOutCategoryList, fadeOutSearchImg2
                );

                // Setup fade-in for table elements
                FadeTransition fadeInTable = new FadeTransition(Duration.millis(150));
                fadeInTable.setFromValue(0);
                fadeInTable.setToValue(1);
                fadeInTable.setNode(discountTbl);

                FadeTransition fadeInSearchImg1 = new FadeTransition(Duration.millis(150));
                fadeInSearchImg1.setFromValue(0);
                fadeInSearchImg1.setToValue(1);
                fadeInSearchImg1.setNode(searchImg1);

                FadeTransition fadeInSearchImg2 = new FadeTransition(Duration.millis(150));
                fadeInSearchImg2.setFromValue(0);
                fadeInSearchImg2.setToValue(1);
                fadeInSearchImg2.setNode(searchImg2);

                FadeTransition fadeInSearchFld = new FadeTransition(Duration.millis(150));
                fadeInSearchFld.setFromValue(0);
                fadeInSearchFld.setToValue(1);
                fadeInSearchFld.setNode(searchFld);

                FadeTransition fadeInColumns = new FadeTransition(Duration.millis(150));
                fadeInColumns.setFromValue(0);
                fadeInColumns.setToValue(1);
                fadeInColumns.setNode(showColumnsBtn);

                // Parallel fade-in for table elements
                ParallelTransition fadeInAll = new ParallelTransition(
                        fadeInTable, fadeInSearchImg1, fadeInSearchImg2, fadeInSearchFld, fadeInColumns
                );

                // Sequence: slide out pane, fade out discount elements, fade in table elements
                SequentialTransition sequence = new SequentialTransition(slideOut, fadeOutAll, fadeInAll);

                sequence.setOnFinished(e -> {
                    discountPane.setVisible(false);
                    // Ensure table elements are visible
                    discountTbl.setVisible(true);
                    searchImg1.setVisible(true);
                    searchImg2.setVisible(true);
                    searchFld.setVisible(true);
                    showColumnsBtn.setVisible(true);
                    // Ensure discount pane elements are hidden
                    toggleByProduct.setVisible(false);
                    toggleByCategory.setVisible(false);
                    productSearchFld.setVisible(false);
                    categorySearchFld.setVisible(false);
                    productsListView.setVisible(false);
                    categoryListView.setVisible(false);
                    searchImg2.setVisible(false);
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
                fadeOutCategory.setInterpolator(Interpolator.EASE_BOTH);

                // Fade out category search field and combo box
                FadeTransition fadeOutCategorySearch = new FadeTransition(Duration.millis(250));
                fadeOutCategorySearch.setFromValue(categorySearchFld.isVisible() ? 1 : 0);
                fadeOutCategorySearch.setToValue(0);
                fadeOutCategorySearch.setNode(categorySearchFld);
                fadeOutCategorySearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeOutCategoryCb = new FadeTransition(Duration.millis(250));
                fadeOutCategoryCb.setFromValue(categoryCb.isVisible() ? 1 : 0);
                fadeOutCategoryCb.setToValue(0);
                fadeOutCategoryCb.setNode(categoryCb);
                fadeOutCategoryCb.setInterpolator(Interpolator.EASE_BOTH);

                // Parallel fade out transitions
                ParallelTransition parallelFadeOut = new ParallelTransition(
                        fadeOutCategory, fadeOutCategorySearch, fadeOutCategoryCb
                );

                // Fade in products list view and related elements
                FadeTransition fadeInProducts = new FadeTransition(Duration.millis(150));
                fadeInProducts.setFromValue(0);
                fadeInProducts.setToValue(1);
                fadeInProducts.setNode(productsListView);
                fadeInProducts.setInterpolator(Interpolator.EASE_BOTH);

                // Fade in product search field and combo box
                FadeTransition fadeInProductSearch = new FadeTransition(Duration.millis(150));
                fadeInProductSearch.setFromValue(0);
                fadeInProductSearch.setToValue(1);
                fadeInProductSearch.setNode(productSearchFld);
                fadeInProductSearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeInProductCb = new FadeTransition(Duration.millis(150));
                fadeInProductCb.setFromValue(0);
                fadeInProductCb.setToValue(1);
                fadeInProductCb.setNode(productCb);
                fadeInProductCb.setInterpolator(Interpolator.EASE_BOTH);

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
                fadeOutProducts.setInterpolator(Interpolator.EASE_BOTH);

                // Fade out product search field and combo box
                FadeTransition fadeOutProductSearch = new FadeTransition(Duration.millis(250));
                fadeOutProductSearch.setFromValue(productSearchFld.isVisible() ? 1 : 0);
                fadeOutProductSearch.setToValue(0);
                fadeOutProductSearch.setNode(productSearchFld);
                fadeOutProductSearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeOutProductCb = new FadeTransition(Duration.millis(250));
                fadeOutProductCb.setFromValue(productCb.isVisible() ? 1 : 0);
                fadeOutProductCb.setToValue(0);
                fadeOutProductCb.setNode(productCb);
                fadeOutProductCb.setInterpolator(Interpolator.EASE_BOTH);

                // Parallel fade out transitions
                ParallelTransition parallelFadeOut = new ParallelTransition(
                        fadeOutProducts, fadeOutProductSearch, fadeOutProductCb
                );

                // Fade in category list view and related elements
                FadeTransition fadeInCategory = new FadeTransition(Duration.millis(150));
                fadeInCategory.setFromValue(0);
                fadeInCategory.setToValue(1);
                fadeInCategory.setNode(categoryListView);
                fadeInCategory.setInterpolator(Interpolator.EASE_BOTH);

                // Fade in category search field and combo box
                FadeTransition fadeInCategorySearch = new FadeTransition(Duration.millis(150));
                fadeInCategorySearch.setFromValue(0);
                fadeInCategorySearch.setToValue(1);
                fadeInCategorySearch.setNode(categorySearchFld);
                fadeInCategorySearch.setInterpolator(Interpolator.EASE_BOTH);

                FadeTransition fadeInCategoryCb = new FadeTransition(Duration.millis(150));
                fadeInCategoryCb.setFromValue(0);
                fadeInCategoryCb.setToValue(1);
                fadeInCategoryCb.setNode(categoryCb);
                fadeInCategoryCb.setInterpolator(Interpolator.EASE_BOTH);

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
