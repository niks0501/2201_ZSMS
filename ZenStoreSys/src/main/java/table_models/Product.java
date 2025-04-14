package table_models;

import javafx.beans.property.*;

public class Product {
    private final IntegerProperty productId;
    private final StringProperty imagePath;
    private final StringProperty name;
    private final IntegerProperty categoryId;
    private final DoubleProperty costPrice;
    private final DoubleProperty markupPercentage;
    private final IntegerProperty stock;
    private final DoubleProperty sellingPrice;
    private final StringProperty barcodePath;
    private final ObjectProperty<java.time.LocalDateTime> lastRestock;

    public Product(int productId, String imagePath, String name, int categoryId,
                   double costPrice, double markupPercentage, int stock,
                   double sellingPrice, String barcodePath,
                   java.time.LocalDateTime lastRestock) {
        this.productId       = new SimpleIntegerProperty(productId);
        this.imagePath       = new SimpleStringProperty(imagePath);
        this.name            = new SimpleStringProperty(name);
        this.categoryId      = new SimpleIntegerProperty(categoryId);
        this.costPrice       = new SimpleDoubleProperty(costPrice);
        this.markupPercentage= new SimpleDoubleProperty(markupPercentage);
        this.stock           = new SimpleIntegerProperty(stock);
        this.sellingPrice    = new SimpleDoubleProperty(sellingPrice);
        this.barcodePath     = new SimpleStringProperty(barcodePath);
        this.lastRestock     = new SimpleObjectProperty<>(lastRestock);
    }

    // Getters for properties
    public IntegerProperty productIdProperty()    { return productId; }
    public StringProperty  imagePathProperty()    { return imagePath; }
    public StringProperty  nameProperty()         { return name; }
    public IntegerProperty categoryIdProperty()   { return categoryId; }
    public DoubleProperty  costPriceProperty()    { return costPrice; }
    public DoubleProperty  markupPercentageProperty(){ return markupPercentage; }
    public IntegerProperty stockProperty()        { return stock; }
    public DoubleProperty  sellingPriceProperty() { return sellingPrice; }
    public StringProperty  barcodePathProperty()  { return barcodePath; }
    public ObjectProperty<java.time.LocalDateTime> lastRestockProperty() { return lastRestock; }
}
