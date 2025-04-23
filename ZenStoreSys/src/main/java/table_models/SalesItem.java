package table_models;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SalesItem {
    private final IntegerProperty productId;
    private final StringProperty productName;
    private final IntegerProperty quantity;
    private final ObjectProperty<BigDecimal> unitPrice;
    private final ObjectProperty<BigDecimal> discount;
    private final ObjectProperty<BigDecimal> subtotal;
    private final ObjectProperty<BigDecimal> finalPrice;
    // Add to SalesItem class
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    // Constructor
    public SalesItem(int productId, String productName, BigDecimal unitPrice) {
        this.productId = new SimpleIntegerProperty(productId);
        this.productName = new SimpleStringProperty(productName);
        this.quantity = new SimpleIntegerProperty(1); // Default quantity
        this.unitPrice = new SimpleObjectProperty<>(unitPrice);
        this.discount = new SimpleObjectProperty<>(BigDecimal.ZERO);
        this.subtotal = new SimpleObjectProperty<>(unitPrice);
        this.finalPrice = new SimpleObjectProperty<>(unitPrice);

        // Listen for changes to update prices
        this.quantity.addListener((obs, oldVal, newVal) -> updatePrices());
        this.unitPrice.addListener((obs, oldVal, newVal) -> updatePrices());
        this.discount.addListener((obs, oldVal, newVal) -> updatePrices());
    }

    // Getters and setters
    public int getProductId() { return productId.get(); }
    public IntegerProperty productIdProperty() { return productId; }
    public void setProductId(int productId) { this.productId.set(productId); }

    public String getProductName() { return productName.get(); }
    public StringProperty productNameProperty() { return productName; }
    public void setProductName(String productName) { this.productName.set(productName); }

    public int getQuantity() { return quantity.get(); }
    public IntegerProperty quantityProperty() { return quantity; }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }

    public BigDecimal getUnitPrice() { return unitPrice.get(); }
    public ObjectProperty<BigDecimal> unitPriceProperty() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice.set(unitPrice); }

    public BigDecimal getDiscount() { return discount.get(); }
    public ObjectProperty<BigDecimal> discountProperty() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount.set(discount); }

    public BigDecimal getSubtotal() { return subtotal.get(); }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subtotal; }

    public BigDecimal getFinalPrice() { return finalPrice.get(); }
    public ObjectProperty<BigDecimal> finalPriceProperty() { return finalPrice; }

    public boolean isSelected() { return selected.get(); }
    public BooleanProperty selectedProperty() { return selected; }
    public void setSelected(boolean selected) { this.selected.set(selected); }


    // Update subtotal and final price when quantity, unitPrice, or discount change
    private void updatePrices() {
        BigDecimal qty = BigDecimal.valueOf(quantity.get());
        BigDecimal price = unitPrice.get() != null ? unitPrice.get() : BigDecimal.ZERO;
        BigDecimal disc = discount.get() != null ? discount.get() : BigDecimal.ZERO;

        // Calculate subtotal
        BigDecimal sub = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        this.subtotal.set(sub);

        // Calculate final price with discount (discount is a percentage)
        BigDecimal discountAmount = sub.multiply(disc)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalP = sub.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        this.finalPrice.set(finalP);
    }
}