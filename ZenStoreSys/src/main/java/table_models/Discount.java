package table_models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Discount {
    private int discountId;
    private Integer productId;
    private String productName;
    private Integer categoryId;
    private String categoryName;
    private String discountType;
    private BigDecimal discountValue;
    private int minQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;

    public Discount(int discountId, Integer productId, String productName, Integer categoryId,
                    String categoryName, String discountType, BigDecimal discountValue,
                    int minQuantity, LocalDate startDate, LocalDate endDate, boolean isActive) {
        this.discountId = discountId;
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minQuantity = minQuantity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    // Getters
    public int getDiscountId() { return discountId; }
    public Integer getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Integer getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public int getMinQuantity() { return minQuantity; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return isActive; }
    public String getStatusText() { return isActive ? "Active" : "Inactive"; }
}