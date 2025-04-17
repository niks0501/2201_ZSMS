package table_models;

import controllers.ProductController;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import other_classes.ProductDAO;

import java.io.IOException;
import java.math.BigDecimal;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Product {
    private int productId;
    private String imagePath;
    private ImageView productImage;
    private String name;
    private String category;
    private BigDecimal costPrice;
    private BigDecimal markupPercentage;
    private int stock;
    private BigDecimal sellingPrice;
    private String barcodePath;
    private ImageView barcodeImage;
    private Button actionButton;
    private HBox actionButtons;
    private Button editButton;
    private Button deleteButton;
    private String tempImagePath;
    private boolean editMode = false;

    public Product(int productId, String imagePath, String name, String category,
                   BigDecimal costPrice, BigDecimal markupPercentage, int stock,
                   BigDecimal sellingPrice, String barcodePath) {
        this.productId = productId;
        this.imagePath = imagePath;
        this.name = name;
        this.category = category;
        this.costPrice = costPrice;
        this.markupPercentage = markupPercentage;
        this.stock = stock;
        this.sellingPrice = sellingPrice;
        this.barcodePath = barcodePath;

        // Set up product image
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 50, 50, true, true);
                    this.productImage = new ImageView(image);
                }
            } catch (Exception e) {
                this.productImage = new ImageView();
            }
        } else {
            this.productImage = new ImageView();
        }

        // Set up barcode image
        if (barcodePath != null && !barcodePath.isEmpty()) {
            try {
                File file = new File(barcodePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 100, 50, true, true);
                    this.barcodeImage = new ImageView(image);
                } else {
                    this.barcodeImage = new ImageView();
                }
            } catch (Exception e) {
                this.barcodeImage = new ImageView();
            }
        } else {
            this.barcodeImage = new ImageView();
        }

        // Set up action buttons with images
        setupActionButtons();
    }

    private void setupActionButtons() {
        // Create edit button with icon
        editButton = new Button();
        editButton.getStyleClass().add("action-button");
        try {
            Image editImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/edit.png")), 20, 20, true, true);
            editButton.setGraphic(new ImageView(editImg));
        } catch (Exception e) {
            editButton.setText("Edit");
        }

        // Create delete button with icon
        deleteButton = new Button();
        deleteButton.getStyleClass().add("action-button");
        try {
            Image deleteImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/delete.png")), 20, 20, true, true);
            deleteButton.setGraphic(new ImageView(deleteImg));
        } catch (Exception e) {
            deleteButton.setText("Delete");
        }

// Add action handler for delete button
        deleteButton.setOnAction(e -> {
            TableView<Product> tableView = findTableView(deleteButton);
            if (tableView != null) {
                // Show confirmation alert
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Product");
                alert.setHeaderText(null);
                alert.setContentText("Are you sure you want to delete product: " + name + "?");

                // Style the alert
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.getStyleClass().add("alert-dialog");

                // Get the buttons
                Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
                okButton.setText("Delete");

                // Show the alert and wait for response
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Delete product from database
                        boolean deleted = deleteProduct(productId);
                        if (deleted) {
                            // Show success notification
                            showNotification(tableView, "Product deleted successfully!");

                            // Refresh the table
                            Object controller = tableView.getUserData();
                            if (controller instanceof ProductController) {
                                ((ProductController) controller).refreshProductTable();
                            }
                        } else {
                            // Show error notification
                            showNotification(tableView, "Failed to delete product!");
                        }
                    }
                });
            }
        });

        // Create context menu for edit button
        ContextMenu contextMenu = new ContextMenu();
        MenuItem restockItem = new MenuItem("Restock");
        MenuItem editDetailsItem = new MenuItem("Edit Details");
        contextMenu.getItems().addAll(restockItem, editDetailsItem);
        contextMenu.getStyleClass().add("edit-context-menu");

        // Add action for restock menu item
        restockItem.setOnAction(e -> {
            TableView<Product> tableView = findTableView(editButton);
            if (tableView != null) {
                // Find the index of this product in the table
                int rowIndex = -1;
                for (int i = 0; i < tableView.getItems().size(); i++) {
                    if (tableView.getItems().get(i).getProductId() == this.productId) {
                        rowIndex = i;
                        break;
                    }
                }

                if (rowIndex >= 0) {
                    // Find the stocks column
                    TableColumn<Product, Integer> stocksColumn = null;
                    for (TableColumn<Product, ?> column : tableView.getColumns()) {
                        if (column.getId() != null && column.getId().equals("stocksColumn")) {
                            stocksColumn = (TableColumn<Product, Integer>) column;
                            break;
                        }
                    }

                    if (stocksColumn != null) {
                        // Create an in-place text field
                        startInPlaceEdit(tableView, rowIndex, stocksColumn);
                    }
                }
            }
        });

        // Add action for edit details menu item
        editDetailsItem.setOnAction(e -> {
            TableView<Product> tableView = findTableView(editButton);
            if (tableView != null) {
                // Reset edit mode for all products first
                for (Product p : tableView.getItems()) {
                    p.setEditMode(false);
                }

                // Find the index of this product in the table
                int rowIndex = -1;
                for (int i = 0; i < tableView.getItems().size(); i++) {
                    if (tableView.getItems().get(i).getProductId() == this.productId) {
                        rowIndex = i;
                        break;
                    }
                }

                if (rowIndex >= 0) {
                    // Get controller to show hidden columns
                    Object controller = tableView.getUserData();
                    if (controller instanceof ProductController) {
                        ((ProductController) controller).showHiddenColumns(true);
                    }

                    // Set only this product to edit mode
                    this.setEditMode(true);

                    // Start edit mode for this row
                    startEditMode(tableView, rowIndex);

                    // Refresh the table to reflect changes
                    tableView.refresh();
                }
            }
        });

        // Show context menu on edit button click
        editButton.setOnAction(e -> {
            contextMenu.show(editButton, Side.BOTTOM, 0, 0);
            e.consume();
        });

        // Create HBox container for buttons
        actionButtons = new HBox(4);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.getChildren().addAll(editButton, deleteButton);
    }

    private boolean deleteProduct(int productId) {
        // First try to delete the product image if it exists
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Files.delete(imageFile.toPath());
                    System.out.println("Product image deleted: " + imagePath);
                }
            } catch (IOException e) {
                System.err.println("Could not delete product image: " + e.getMessage());
                // Continue with deletion even if image deletion fails
            }
        }

        // Delete the barcode image if it exists
        if (barcodePath != null && !barcodePath.isEmpty()) {
            try {
                File barcodeFile = new File(barcodePath);
                if (barcodeFile.exists()) {
                    Files.delete(barcodeFile.toPath());
                    System.out.println("Barcode image deleted: " + barcodePath);
                }
            } catch (IOException e) {
                System.err.println("Could not delete barcode image: " + e.getMessage());
                // Continue with deletion even if barcode deletion fails
            }
        }

        // Delete the product from database using ProductDAO
        return ProductDAO.deleteProduct(productId);
    }

    // Add this method to handle editing mode
    private void startEditMode(TableView<Product> tableView, int rowIndex) {
        // Store original cell factories for each column
        Map<TableColumn<Product, ?>, Callback<TableColumn<Product, ?>, TableCell<Product, ?>>> originalFactories = new HashMap<>();

        // Find editable columns
        TableColumn<Product, String> nameColumn = null;
        TableColumn<Product, String> categoryColumn = null;
        TableColumn<Product, BigDecimal> costPriceColumn = null;
        TableColumn<Product, BigDecimal> markupColumn = null;
        TableColumn<Product, ImageView> imageColumn = null;

        // Find columns by ID
        for (TableColumn<Product, ?> column : tableView.getColumns()) {
            if (column.getId() != null) {
                if (column.getId().equals("productNameColumn")) {
                    nameColumn = (TableColumn<Product, String>) column;
                } else if (column.getId().equals("categoryColumn")) {
                    categoryColumn = (TableColumn<Product, String>) column;
                } else if (column.getId().equals("costPriceColumn")) {
                    costPriceColumn = (TableColumn<Product, BigDecimal>) column;
                } else if (column.getId().equals("markupColumn")) {
                    markupColumn = (TableColumn<Product, BigDecimal>) column;
                } else if (column.getId().equals("productImgColumn")) {
                    imageColumn = (TableColumn<Product, ImageView>) column;
                }
            }
        }

        // Setup name column editor
        if (nameColumn != null) {
            originalFactories.put(nameColumn, (Callback) nameColumn.getCellFactory());
            setupTextFieldEditor(nameColumn, rowIndex);
        }

        // Setup category column editor
        if (categoryColumn != null) {
            originalFactories.put(categoryColumn, (Callback) categoryColumn.getCellFactory());
            setupCategoryEditor(categoryColumn, rowIndex);
        }

        // Setup cost price column editor
        if (costPriceColumn != null) {
            originalFactories.put(costPriceColumn, (Callback) costPriceColumn.getCellFactory());
            setupNumericEditor(costPriceColumn, rowIndex);
        }

        // Setup markup column editor
        if (markupColumn != null) {
            originalFactories.put(markupColumn, (Callback) markupColumn.getCellFactory());
            setupNumericEditor(markupColumn, rowIndex);
        }

        // Setup image column editor
        if (imageColumn != null) {
            originalFactories.put(imageColumn, (Callback) imageColumn.getCellFactory());
            setupImageEditor(imageColumn, rowIndex);
        }

        // Create save button in a separate column
        TableColumn<Product, ?> lastColumn = tableView.getColumns().get(tableView.getColumns().size() - 1);
        originalFactories.put(lastColumn, (Callback) lastColumn.getCellFactory());
        setupSaveButton(lastColumn, rowIndex, tableView, originalFactories);

        // Refresh table to show editors
        tableView.refresh();
    }

    // Setup text field editor for string columns
    private void setupTextFieldEditor(TableColumn<Product, String> column, int rowIndex) {
        column.setCellFactory(col -> new TableCell<Product, String>() {
            private final TextField textField = new TextField();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    // Check if this specific row is in edit mode
                    Product currentProduct = getTableView().getItems().get(getIndex());

                    if (currentProduct.isInEditMode() && getIndex() == rowIndex) {
                        // Edit mode styling for this specific row
                        textField.setText(item);
                        setGraphic(textField);
                        setStyle("-fx-background-color: #e6f2ff; -fx-border-color: #0078d7;");
                    } else {
                        // Normal display for other rows
                        setText(item);
                        setGraphic(null);
                        setStyle(null);
                    }
                }
            }
        });
    }

    // Setup category combobox editor
    private void setupCategoryEditor(TableColumn<Product, String> column, int rowIndex) {
        column.setCellFactory(col -> new TableCell<Product, String>() {
            private final ComboBox<Category> comboBox = new ComboBox<>();

            {
                // Load categories from database
                ObservableList<Category> categories = ProductDAO.getAllCategories();
                comboBox.setItems(categories);

                // Style the combo box
                comboBox.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: #81B29A;" +
                                "-fx-border-radius: 3px;" +
                                "-fx-padding: 3px;" +
                                "-fx-font-size: 12px;"
                );

                // Set converter to display category name
                comboBox.setConverter(new StringConverter<Category>() {
                    @Override
                    public String toString(Category category) {
                        return category != null ? category.getName() : "";
                    }

                    @Override
                    public Category fromString(String string) {
                        return null; // Not needed
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    // Check if this specific row is in edit mode
                    Product currentProduct = getTableView().getItems().get(getIndex());

                    // Check if this is a hidden column
                    boolean isHiddenColumn = column.getId() != null &&
                            (column.getId().equals("costPriceColumn") ||
                                    column.getId().equals("markupColumn"));

                    if (isHiddenColumn && (!currentProduct.isInEditMode() || getIndex() != rowIndex)) {
                        // Hide content for hidden columns in non-edited rows
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-opacity: 0;");
                    } else if (currentProduct.isInEditMode() && getIndex() == rowIndex) {
                        // Edit mode for the selected row
                        for (Category category : comboBox.getItems()) {
                            if (category.getName().equals(item)) {
                                comboBox.setValue(category);
                                break;
                            }
                        }
                        setGraphic(comboBox);
                        setStyle("-fx-background-color: #e6f2ff; -fx-border-color: #0078d7;");
                    } else {
                        // Normal display for non-hidden columns in other rows
                        setText(item);
                        setGraphic(null);
                        setStyle(null);
                    }
                }
            }
        });
    }

    // Setup numeric editor for BigDecimal columns
    private void setupNumericEditor(TableColumn<Product, BigDecimal> column, int rowIndex) {
        column.setCellFactory(col -> new TableCell<Product, BigDecimal>() {
            private final TextField textField = new TextField();

            {
                // Allow only numeric input with decimal point
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue == null) return;
                    if (newValue.isEmpty()) return;

                    // Only allow digits and at most one decimal point
                    if (!newValue.matches("\\d*(\\.\\d*)?")) {
                        textField.setText(oldValue);
                    }
                });
            }

            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    // Check if this specific row is in edit mode
                    Product currentProduct = getTableView().getItems().get(getIndex());

                    // Check if this is a hidden column
                    boolean isHiddenColumn = column.getId() != null &&
                            (column.getId().equals("costPriceColumn") ||
                                    column.getId().equals("markupColumn"));

                    if (isHiddenColumn && (!currentProduct.isInEditMode() || getIndex() != rowIndex)) {
                        // Hide content for hidden columns in non-edited rows
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-opacity: 0;");
                    } else if (currentProduct.isInEditMode() && getIndex() == rowIndex) {
                        // Edit mode for the selected row
                        textField.setText(item.toString());
                        setGraphic(textField);
                        setStyle("-fx-background-color: #e6f2ff; -fx-border-color: #0078d7;");
                    } else {
                        // Normal display for non-hidden columns in other rows
                        setText(item.toString());
                        setGraphic(null);
                        setStyle(null);
                    }
                }
            }
        });
    }

    // Setup image editor with context menu
    private void setupImageEditor(TableColumn<Product, ImageView> column, int rowIndex) {
        column.setCellFactory(col -> new TableCell<Product, ImageView>() {
            private final Button editImageBtn = new Button("Edit");

            {
                // Base style for the edit button
                editImageBtn.setStyle(
                        "-fx-background-color: #81B29A;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5px 10px;" +
                                "-fx-background-radius: 3px;" +
                                "-fx-cursor: hand;" +
                                "-fx-transition: all 0.2s ease;"
                );

                // Hover effect
                editImageBtn.setOnMouseEntered(e -> editImageBtn.setStyle(
                        "-fx-background-color: #6d9a86;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5px 10px;" +
                                "-fx-background-radius: 3px;" +
                                "-fx-cursor: hand;" +
                                "-fx-scale-x: 1.03;" +
                                "-fx-scale-y: 1.03;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0.1, 0, 1);"
                ));

                // Reset on mouse exit
                editImageBtn.setOnMouseExited(e -> editImageBtn.setStyle(
                        "-fx-background-color: #81B29A;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5px 10px;" +
                                "-fx-background-radius: 3px;" +
                                "-fx-cursor: hand;" +
                                "-fx-transition: all 0.2s ease;"
                ));

                // Create context menu for image options
                ContextMenu imageMenu = new ContextMenu();
                MenuItem importItem = new MenuItem("Import");
                MenuItem takePictureItem = new MenuItem("Take Picture");
                imageMenu.getItems().addAll(importItem, takePictureItem);

                // Handle import action
                importItem.setOnAction(e -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Select Product Image");
                    fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
                    );

                    Stage stage = (Stage) getScene().getWindow();
                    File file = fileChooser.showOpenDialog(stage);

                    if (file != null) {
                        try {
                            // Get the current product
                            Product currentProduct = getTableView().getItems().get(getIndex());

                            // Delete old image if it exists
                            if (currentProduct.getImagePath() != null && !currentProduct.getImagePath().isEmpty()) {
                                try {
                                    File oldImageFile = new File(currentProduct.getImagePath());
                                    if (oldImageFile.exists() && !oldImageFile.isDirectory()) {
                                        boolean deleted = oldImageFile.delete();
                                        if (!deleted) {
                                            System.out.println("Warning: Could not delete old image file: " + oldImageFile.getAbsolutePath());
                                        }
                                    }
                                } catch (Exception ex) {
                                    System.err.println("Error deleting old image: " + ex.getMessage());
                                }
                            }

                            // Create the destination directory if it doesn't exist
                            File destDir = new File("C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\productImage");
                            if (!destDir.exists()) {
                                destDir.mkdirs();
                            }

                            // Create new filename using product ID and timestamp
                            String fileExtension = file.getName().substring(file.getName().lastIndexOf("."));
                            String fileName = "product_" + currentProduct.getProductId() + "_" + System.currentTimeMillis() + fileExtension;
                            File destFile = new File(destDir, fileName);

                            // Copy the file
                            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            // Save the path directly to the product
                            String imagePath = destFile.getAbsolutePath();
                            currentProduct.setImagePath(imagePath);

                            // Update the product in the database
                            ProductDAO.updateProduct(
                                    currentProduct.getProductId(),
                                    null, null, null, null, null,
                                    imagePath
                            );

                            // Update the image view
                            Image image = new Image(destFile.toURI().toString(), 50, 50, true, true);
                            ImageView imageView = new ImageView(image);
                            imageView.setFitWidth(50);
                            imageView.setFitHeight(50);

                            // Update cell
                            HBox container = new HBox(5);
                            container.setAlignment(Pos.CENTER);
                            container.getChildren().addAll(imageView, editImageBtn);
                            setGraphic(container);

                        } catch (IOException ex) {
                            ex.printStackTrace();
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(null);
                            alert.setContentText("Failed to save image: " + ex.getMessage());
                            alert.showAndWait();
                        }
                    }
                });

                // Handle take picture action
                takePictureItem.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Camera");
                    alert.setHeaderText(null);
                    alert.setContentText("Camera functionality would be implemented here.");
                    alert.showAndWait();
                });

                // Show context menu on button click
                editImageBtn.setOnAction(e -> imageMenu.show(editImageBtn, Side.BOTTOM, 0, 0));
            }

            @Override
            protected void updateItem(ImageView item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    // Check if this specific row is in edit mode
                    Product currentProduct = getTableView().getItems().get(getIndex());

                    // Check if this is a hidden column
                    boolean isHiddenColumn = column.getId() != null &&
                            (column.getId().equals("costPriceColumn") ||
                                    column.getId().equals("markupColumn"));

                    if (isHiddenColumn && (!currentProduct.isInEditMode() || getIndex() != rowIndex)) {
                        // Hide content for hidden columns in non-edited rows
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-opacity: 0;");
                    } else if (currentProduct.isInEditMode() && getIndex() == rowIndex) {
                        // Edit mode for the selected row
                        HBox container = new HBox(5);
                        container.setAlignment(Pos.CENTER);
                        container.getChildren().addAll(item, editImageBtn);
                        setGraphic(container);
                        setStyle("-fx-background-color: #e6f2ff; -fx-border-color: #0078d7;");
                    } else {
                        // Normal display for other rows
                        setGraphic(item);
                        setStyle(null);
                    }
                }
            }
        });
    }

    // Fix the setupSaveButton method
    private void setupSaveButton(TableColumn<Product, ?> column, int rowIndex,
                                 TableView<Product> tableView,
                                 Map<TableColumn<Product, ?>, Callback<TableColumn<Product, ?>, TableCell<Product, ?>>> originalFactories) {
        column.setCellFactory(col -> {
            return new TableCell() {
                private final Button saveBtn = new Button("Save");
                private final Button cancelBtn = new Button("Cancel");

                {
                    saveBtn.setOnAction(e -> saveChanges(rowIndex, tableView, originalFactories));
                    cancelBtn.setOnAction(e -> cancelEditing(tableView, originalFactories));

                    saveBtn.getStyleClass().add("action-button");
                    cancelBtn.getStyleClass().add("action-button");
                }

                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    if (getIndex() == rowIndex) {
                        HBox box = new HBox(5);
                        box.setAlignment(Pos.CENTER);
                        box.getChildren().addAll(saveBtn, cancelBtn);
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            };
        });
    }

    // Add this method to Product class for saving changes
    private void saveChanges(int rowIndex, TableView<Product> tableView,
                             Map<TableColumn<Product, ?>, Callback<TableColumn<Product, ?>, TableCell<Product, ?>>> originalFactories) {
        Product product = tableView.getItems().get(rowIndex);

        // Get editor components from cells
        String newName = getTextFromNameEditor(tableView, rowIndex);
        Integer newCategoryId = getCategoryIdFromEditor(tableView, rowIndex);
        BigDecimal newCostPrice = getBigDecimalFromEditor(tableView, "costPriceColumn", rowIndex);
        BigDecimal newMarkup = getBigDecimalFromEditor(tableView, "markupColumn", rowIndex);
        String newImagePath = product.getImagePath();

        // Calculate new selling price if cost price or markup changed
        BigDecimal newSellingPrice = null;
        if (newCostPrice != null || newMarkup != null) {
            // Use existing values if not changed
            BigDecimal costPrice = newCostPrice != null ? newCostPrice : product.getCostPrice();
            BigDecimal markup = newMarkup != null ? newMarkup : product.getMarkupPercentage();

            // Formula: selling_price = cost_price * (1 + (markup_percentage / 100))
            BigDecimal markupFactor = BigDecimal.ONE.add(markup.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP));
            newSellingPrice = costPrice.multiply(markupFactor).setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        // Update database
        boolean success = ProductDAO.updateProduct(product.getProductId(), newName, newCategoryId,
                newCostPrice, newMarkup, newSellingPrice, newImagePath);

        // Display result
        if (success) {
            showNotification(tableView, "Product updated successfully");

            // Reset edit mode
            product.setEditMode(false);

            // Restore original view
            restoreOriginalFactories(tableView, originalFactories);

            // Hide columns
            Object controller = tableView.getUserData();
            if (controller instanceof ProductController) {
                ((ProductController) controller).showHiddenColumns(false);
                ((ProductController) controller).refreshProductTable();
            }
        } else {
            showNotification(tableView, "Failed to update product");
        }
    }

    // Cancel editing and restore original view
    private void cancelEditing(TableView<Product> tableView,
                               Map<TableColumn<Product, ?>, Callback<TableColumn<Product, ?>, TableCell<Product, ?>>> originalFactories) {
        // Restore original cell factories
        restoreOriginalFactories(tableView, originalFactories);

        // Hide the hidden columns again
        Object controller = tableView.getUserData();
        if (controller instanceof ProductController) {
            ((ProductController) controller).showHiddenColumns(false);
        }

        // Refresh the table
        tableView.refresh();
    }

    // Restore original cell factories
    private <S> void restoreOriginalFactories(TableView<Product> tableView,
                                              Map<TableColumn<Product, ?>, Callback<TableColumn<Product, ?>, TableCell<Product, ?>>> originalFactories) {
        for (Map.Entry<TableColumn<Product, ?>, Callback<TableColumn<Product, ?>, TableCell<Product, ?>>> entry : originalFactories.entrySet()) {
            TableColumn<Product, ?> column = entry.getKey();
            Callback<TableColumn<Product, ?>, TableCell<Product, ?>> factory = entry.getValue();
            column.setCellFactory((Callback) factory);
        }
    }

    private TableView<Product> findTableView(Node node) {
        Parent parent = node.getParent();
        while (parent != null && !(parent instanceof TableView)) {
            parent = parent.getParent();
        }
        return (TableView<Product>) parent;
    }

    // Add this method to handle in-place editing
    private void startInPlaceEdit(TableView<Product> tableView, int rowIndex, TableColumn<Product, Integer> stocksColumn) {
        // Save the original cell factory
        Callback<TableColumn<Product, Integer>, TableCell<Product, Integer>> originalCellFactory = stocksColumn.getCellFactory();

        // Create a temporary cell factory for editing
        stocksColumn.setCellFactory(column -> new TableCell<Product, Integer>() {
            private final TextField textField = new TextField();

            {
                // Add validation to only accept positive integers
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.startsWith("-")) {
                        showNotification(getTableView(), "Negative values are not allowed");
                        textField.setText(oldValue);
                    } else if (!newValue.matches("\\d*(\\.\\d*)?")) {
                        textField.setText(oldValue);
                    }
                });

                // Handle Enter key to complete editing
                textField.setOnAction(e -> commitEdit());

                // Handle focus loss to complete editing
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        commitEdit();
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (getIndex() == rowIndex) {
                    setText(null);
                    textField.setText("");
                    setGraphic(textField);
                    textField.requestFocus();
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }

            private void commitEdit() {
                try {
                    String text = textField.getText();
                    if (!text.isEmpty()) {
                        int addAmount = Integer.parseInt(text);
                        if (addAmount > 0) {
                            Product product = getTableView().getItems().get(rowIndex);
                            int newStock = product.getStock() + addAmount;

                            // Update database
                            boolean success = ProductDAO.updateProductStock(product.getProductId(), newStock);

                            if (success) {
                                // Show notification of success
                                showNotification(getTableView(), "🔔 Successfully added " + addAmount + " items");

                                // Refresh the table
                                Object controller = tableView.getUserData();
                                if (controller instanceof ProductController) {
                                    ((ProductController) controller).refreshProductTable();
                                } else {
                                    tableView.refresh();
                                }
                            } else {
                                showNotification(getTableView(), "Failed to update stock");
                            }
                        }
                    }
                } catch (NumberFormatException ex) {
                    showNotification(getTableView(), "Please enter a valid number");
                } finally {
                    // Restore original cell factory
                    Platform.runLater(() -> stocksColumn.setCellFactory(originalCellFactory != null ?
                            originalCellFactory : column -> new TableCell<>() {
                        @Override
                        protected void updateItem(Integer item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty ? null : item.toString());
                        }
                    }));
                }
            }
        });

        // Refresh the table to show the text field
        tableView.refresh();
    }


    private void showNotification(TableView<Product> tableView, String message) {
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
        Parent parent = tableView.getParent();
        while (!(parent instanceof Pane) || parent instanceof TableView) {
            parent = parent.getParent();
            if (parent == null) return;
        }

        Pane parentPane = (Pane) parent;

        // Position the notification
        notification.setLayoutX(parentPane.getWidth() - notificationWidth - 20);
        notification.setLayoutY(parentPane.getHeight() - notificationHeight - 80);

        // Add to scene and animate
        parentPane.getChildren().add(notification);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.setOnFinished(e -> parentPane.getChildren().remove(notification));
        fadeOut.play();
    }



    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImagePath() {
        return imagePath;
    }



    // Update getter for action column
    public HBox getActionButtons() {
        return actionButtons;
    }

    // Helper methods to extract values from editors
    private String getTextFromNameEditor(TableView<Product> tableView, int rowIndex) {
        TableColumn<Product, ?> column = findColumnById(tableView, "productNameColumn");
        if (column != null) {
            TableCell<Product, ?> cell = findCellInColumn(column, rowIndex);
            if (cell != null && cell.getGraphic() instanceof TextField) {
                return ((TextField) cell.getGraphic()).getText();
            }
        }
        return null;
    }

    private Integer getCategoryIdFromEditor(TableView<Product> tableView, int rowIndex) {
        TableColumn<Product, ?> column = findColumnById(tableView, "categoryColumn");
        if (column != null) {
            TableCell<Product, ?> cell = findCellInColumn(column, rowIndex);
            if (cell != null && cell.getGraphic() instanceof ComboBox) {
                ComboBox<Category> comboBox = (ComboBox<Category>) cell.getGraphic();
                Category selectedCategory = comboBox.getValue();
                return selectedCategory != null ? selectedCategory.getId() : null;
            }
        }
        return null;
    }

    private BigDecimal getBigDecimalFromEditor(TableView<Product> tableView, String columnId, int rowIndex) {
        TableColumn<Product, ?> column = findColumnById(tableView, columnId);
        if (column != null) {
            TableCell<Product, ?> cell = findCellInColumn(column, rowIndex);
            if (cell != null && cell.getGraphic() instanceof TextField) {
                String text = ((TextField) cell.getGraphic()).getText();
                try {
                    return new BigDecimal(text);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private TableColumn<Product, ?> findColumnById(TableView<Product> tableView, String id) {
        for (TableColumn<Product, ?> column : tableView.getColumns()) {
            if (column.getId() != null && column.getId().equals(id)) {
                return column;
            }
        }
        return null;
    }

    public boolean isInEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    private TableCell<Product, ?> findCellInColumn(TableColumn<Product, ?> column, int rowIndex) {
        for (Node node : column.getTableView().lookupAll(".table-cell")) {
            if (node instanceof TableCell) {
                TableCell<?, ?> cell = (TableCell<?, ?>) node;
                if (cell.getTableColumn() == column && cell.getIndex() == rowIndex) {
                    return (TableCell<Product, ?>) cell;
                }
            }
        }
        return null;
    }

    // Getters and setters
    public int getProductId() { return productId; }
    public ImageView getProductImage() { return productImage; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getMarkupPercentage() { return markupPercentage; }
    public int getStock() { return stock; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public String getBarcodePath() { return barcodePath; }
    public ImageView getBarcodeImage() { return barcodeImage; }

}