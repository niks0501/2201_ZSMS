package table_models;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class CreditTransaction {
    private int transactionId;
    private String customerName;
    private BigDecimal amount;
    private Timestamp transactionDate;
    private Date dueDate;
    private String status;
    private int daysRemaining;

    public CreditTransaction(int transactionId, String customerName, BigDecimal amount,
                             Timestamp transactionDate, Date dueDate, String status) {
        this.transactionId = transactionId;
        this.customerName = customerName;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.dueDate = dueDate;
        this.status = status;

        // Calculate days remaining for due date filtering
        if (dueDate != null) {
            this.daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), dueDate.toLocalDate());
        } else {
            this.daysRemaining = -1;
        }
    }

    // Getters
    public int getTransactionId() { return transactionId; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getAmount() { return amount; }
    public String getFormattedAmount() { return "₱" + amount.toString(); }
    public Timestamp getTransactionDate() { return transactionDate; }
    public Date getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public int getDaysRemaining() { return daysRemaining; }
    public boolean isNearDue() { return daysRemaining >= 0 && daysRemaining <= 7; }
}