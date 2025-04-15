package table_models;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.math.BigDecimal;
import java.io.File;

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

        // Set up action button
        this.actionButton = new Button("Edit");
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
    public Button getActionButton() { return actionButton; }
}