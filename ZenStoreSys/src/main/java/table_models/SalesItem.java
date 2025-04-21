package table_models;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SalesItem {
    private int productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal subtotal;
    private BigDecimal finalPrice;

    // Constructor
    public SalesItem(int productId, String productName, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = 1; // Default quantity
        this.unitPrice = unitPrice;
        this.discount = BigDecimal.ZERO;
        this.subtotal = unitPrice;
        this.finalPrice = unitPrice;
    }

    // Getters and setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updatePrices();
    }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        updatePrices();
    }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
        updatePrices();
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getFinalPrice() { return finalPrice; }

    // Update subtotal and final price when quantity or prices change
    private void updatePrices() {
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountAmount = subtotal.multiply(discount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.finalPrice = subtotal.subtract(discountAmount);
    }
}