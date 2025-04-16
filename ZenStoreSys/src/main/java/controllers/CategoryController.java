package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
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
import javafx.stage.Stage;
import other_classes.DBConnect;
import table_models.Category;

import java.sql.*;

public class CategoryController {

    @FXML
    private TableColumn<Category, Void> actionsColumn;

    @FXML
    Pane barPane;

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

    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        btnExit.setOnAction(e -> ((Stage) categoryManeFrame.getScene().getWindow()).close());
        btnMinimize.setOnAction(e -> ((Stage) categoryManeFrame.getScene().getWindow()).setIconified(true));

        setUpColumns();

        // Configure actions column with edit and delete buttons
        setupActionsColumn();

        // Load categories data
        loadCategories();

        // Set up add category button
        btnAddCategory.setOnAction(e -> addCategory());
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
            showAlert(Alert.AlertType.WARNING, "Please enter a category name");
            return;
        }

        try (Connection conn = DBConnect.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL sp_add_category(?, ?)}")) {

            stmt.setString(1, categoryName);
            stmt.registerOutParameter(2, Types.BOOLEAN);
            stmt.execute();

            boolean success = stmt.getBoolean(2);

            if (success) {
                categoryNameFld.clear();

                showAlert(Alert.AlertType.INFORMATION, "Category added successfully");
                loadCategories();

            } else {
                showAlert(Alert.AlertType.ERROR, "Category name already exists");
            }


        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error adding category: " + e.getMessage());
        }
    }

    private void editCategory(Category category) {
        categoryNameFld.setText(category.getName());
        btnAddCategory.setText("Update");

        // Change button action temporarily
        btnAddCategory.setOnAction(e -> {
            updateCategory(category.getId(), categoryNameFld.getText().trim());
            btnAddCategory.setText("Add Category");
            btnAddCategory.setOnAction(ev -> addCategory());
            categoryNameFld.clear();
        });
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
                showAlert(Alert.AlertType.INFORMATION, "Category updated successfully");
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
                        showAlert(Alert.AlertType.INFORMATION, "Category deleted successfully");
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
}
