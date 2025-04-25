package table_models;

import java.math.BigDecimal;

// Helper class to store customer information
public class CustomerInfo {
    public String name;
    public String email;
    BigDecimal creditBalance;

    public CustomerInfo(String name, String email, BigDecimal creditBalance) {
        this.name = name;
        this.email = email;
        this.creditBalance = creditBalance;
    }
}