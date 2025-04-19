package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import other_classes.DBConnect;
import table_models.Category;

import java.sql.*;

public class CategoryController {

    @FXML
    private TableColumn<Category, Void> actionsColumn;

    @FXML
    public Pane barPane;

    @FXML
    private MFXButton btnAddCategory;

    @FXML
    public MFXButton btnExit;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private TableColumn<Category, Integer> categoryIdColumn;

    @FXML
    private AnchorPane categoryManeFrame;

    @FXML
    private TableColumn<Category, String> categoryNameColumn;

    @FXML
    private MFXTextField categoryNameFld;

    @FXML
    private TableView<Category> categoryTbl;

    @FXML
    private MFXToggleButton btnToggleSearch;

    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initial setup - focus on UI components only
        setupToggleSearch();
        setUpColumns();
        setupActionsColumn();

        btnAddCategory.setOnAction(e -> addCategory());

        // Show loading indicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-progress-color: #81B29A;");
        progressIndicator.setPrefSize(40, 40);
        VBox loadingBox = new VBox(progressIndicator, new Label("Loading categories..."));
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setSpacing(10);
        categoryManeFrame.getChildren().add(loadingBox);
        AnchorPane.setTopAnchor(loadingBox, 150.0);
        AnchorPane.setLeftAnchor(loadingBox, 0.0);
        AnchorPane.setRightAnchor(loadingBox, 0.0);

        // Load categories in background thread
        loadCategoriesAsync(loadingBox);
    }

    private void loadCategoriesAsync(VBox loadingBox) {
        new Thread(() -> {
            try {
                // Do database work in background thread
                ObservableList<Category> categories = FXCollections.observableArrayList();

                try (Connection conn = DBConnect.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM categories ORDER BY category_id")) {

                    while (rs.next()) {
                        int id = rs.getInt("category_id");
                        String name = rs.getString("category_name");
                        categories.add(new Category(id, name));
                    }
                }

                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    categoryList.setAll(categories);
                    categoryTbl.setItems(categoryList);
                    categoryManeFrame.getChildren().remove(loadingBox);
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    categoryManeFrame.getChildren().remove(loadingBox);
                    showAlert(Alert.AlertType.ERROR, "Failed to load categories: " + e.getMessage());
                });
            }
        }).start();
    }

    private void setupToggleSearch() {
        // Create the search text field using standard JavaFX TextField
        TextField searchField = new TextField();
        searchField.setPromptText("Search Categories");
        searchField.setPrefWidth(categoryNameColumn.getWidth() - 10);
        searchField.setStyle("-fx-border-color: #81b29a; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        // Store the original header text
        String originalHeaderText = categoryNameColumn.getText();

        // Initially use just the column text (no graphic)
        categoryNameColumn.setGraphic(null);

        // Set up toggle button listener
        btnToggleSearch.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // When toggled on, set the search field as the graphic and clear the text
                categoryNameColumn.setGraphic(searchField);
                categoryNameColumn.setText(""); // Clear the text to avoid duplication
                searchField.setPromptText("Search Categories");
            } else {
                // When toggled off, restore original text and remove graphic
                categoryNameColumn.setText(originalHeaderText);
                categoryNameColumn.setGraphic(null);
                searchField.clear();
                loadCategories();
            }
        });

        // Set up real-time filtering as user types
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterCategories(newVal);
        });

        // Add listener to adjust width of search field when column resizes
        categoryNameColumn.widthProperty().addListener((obs, old, newWidth) -> {
            searchField.setPrefWidth(newWidth.doubleValue() - 10);
        });
    }

    private void filterCategories(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            // Show all categories if search is empty
            categoryTbl.setItems(categoryList);
            return;
        }

        // Create filtered list based on search text
        ObservableList<Category> filteredList = FXCollections.observableArrayList();
        String lowerCaseFilter = searchText.toLowerCase();

        for (Category category : categoryList) {
            if (category.getName().toLowerCase().contains(lowerCaseFilter)) {
                filteredList.add(category);
            }
        }

        // Update the table with filtered results
        categoryTbl.setItems(filteredList);
    }

    private void setUpColumns() {
        // Set up table columns
        categoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Make the table editable
        categoryTbl.setEditable(true);

        // Configure the name column to be editable
        categoryNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        categoryNameColumn.setOnEditCommit(event -> {
            Category category = event.getRowValue();
            String newName = event.getNewValue();

            if (newName != null && !newName.trim().isEmpty()) {
                // Update database with new name
                updateCategory(category.getId(), newName.trim());
            } else {
                // If empty name, refresh table to discard changes
                loadCategories();
            }
        });
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                // Create container for buttons
                HBox buttonsBox = new HBox(5);
                buttonsBox.setAlignment(Pos.CENTER);

                // Edit button
                Button editBtn = createActionButton("/img/edit.png");
                editBtn.setOnAction(event -> {
                    // Get the current row
                    TableRow<?> row = getTableRow();

                    // Start editing the name cell in this row
                    categoryTbl.edit(row.getIndex(), categoryNameColumn);
                });

                // Delete button
                Button deleteBtn = createActionButton("/img/delete.png");
                deleteBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    deleteCategory(category);
                });

                // Add buttons to container
                buttonsBox.getChildren().addAll(editBtn, deleteBtn);
                setGraphic(buttonsBox);
            }
        });
    }

    private Button createActionButton(String imagePath) {
        Button button = new Button();

        // Load image
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);

        // Configure button appearance
        button.setGraphic(imageView);
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #81B29A;" +
                        "-fx-border-radius: 5;" +
                        "-fx-cursor: hand;"
        );

        return button;
    }

    private void loadCategories() {
        categoryList.clear();

        try (Connection conn = DBConnect.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL sp_get_all_categories()}");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("category_id");
                String name = rs.getString("category_name");
                categoryList.add(new Category(id, name));
            }

            categoryTbl.setItems(categoryList);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error loading categories: " + e.getMessage());
        }
    }

    private void addCategory() {
        String categoryName = categoryNameFld.getText().trim();

        if (categoryName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Category name cannot be empty");
            return;
        }

        // Show progress indicator
        ProgressIndicator progress = new ProgressIndicator();
        progress.setStyle("-fx-progress-color: #81B29A;");

        VBox progressBox = new VBox(progress, new Label("Adding category..."));
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setStyle("-fx-background-color: transparent; -fx-padding: 10;");
        categoryManeFrame.getChildren().add(progressBox);
        AnchorPane.setTopAnchor(progressBox, 100.0);
        AnchorPane.setLeftAnchor(progressBox, 100.0);
        AnchorPane.setRightAnchor(progressBox, 100.0);

        new Thread(() -> {
            try (Connection conn = DBConnect.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO categories (category_name) VALUES (?)",
                         Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, categoryName);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int newCategoryId = generatedKeys.getInt(1);
                        Category newCategory = new Category(newCategoryId, categoryName);

                        javafx.application.Platform.runLater(() -> {
                            categoryList.add(newCategory);
                            categoryTbl.refresh();
                            categoryNameFld.clear();
                            showNotification("Category added successfully!");
                            categoryManeFrame.getChildren().remove(progressBox);
                        });
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error adding category: " + e.getMessage());
                    categoryManeFrame.getChildren().remove(progressBox);
                });
            }
        }).start();
    }

    private void updateCategory(int categoryId, String newName) {
        if (newName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please enter a category name");
            return;
        }

        try (Connection conn = DBConnect.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL sp_update_category(?, ?, ?)}")) {

            stmt.setInt(1, categoryId);
            stmt.setString(2, newName);
            stmt.registerOutParameter(3, Types.BOOLEAN);
            stmt.execute();

            boolean success = stmt.getBoolean(3);

            if (success) {
                loadCategories();
                showNotification("Category updated successfully");
            } else {
                showAlert(Alert.AlertType.ERROR, "Category name already exists or not found");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error updating category: " + e.getMessage());
        }
    }

    private void deleteCategory(Category category) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete category '" + category.getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Confirm Delete");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DBConnect.getConnection();
                     CallableStatement stmt = conn.prepareCall("{CALL sp_delete_category(?, ?)}")) {

                    stmt.setInt(1, category.getId());
                    stmt.registerOutParameter(2, Types.BOOLEAN);
                    stmt.execute();

                    boolean success = stmt.getBoolean(2);

                    if (success) {
                        loadCategories();
                        showNotification("Category deleted successfully");
                    } else {
                        showAlert(Alert.AlertType.ERROR,
                                "Cannot delete category because it is used by existing products");
                    }
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error deleting category: " + e.getMessage());
                }
            }
        });
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
        notification.setLayoutX(categoryManeFrame.getWidth() - notificationWidth - 20);
        notification.setLayoutY(categoryManeFrame.getHeight() - notificationHeight - 20);

        // Add to scene and animate
        categoryManeFrame.getChildren().add(notification);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.setOnFinished(e -> categoryManeFrame.getChildren().remove(notification));
        fadeOut.play();
    }
}
