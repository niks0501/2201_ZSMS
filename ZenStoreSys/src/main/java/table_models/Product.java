package table_models;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.io.File;
import java.util.Date;
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

        // Create context menu for edit button
        ContextMenu contextMenu = new ContextMenu();
        MenuItem restockItem = new MenuItem("Restock");
        MenuItem editDetailsItem = new MenuItem("Edit Details");
        contextMenu.getItems().addAll(restockItem, editDetailsItem);
        contextMenu.getStyleClass().add("edit-context-menu");

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

    // Update getter for action column
    public HBox getActionButtons() {
        return actionButtons;
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