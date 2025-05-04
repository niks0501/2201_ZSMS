package controllers;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.utils.SwingFXUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import other_classes.DBConnect;
import table_models.SalesItem;
import javafx.collections.FXCollections;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SalesController {
    // Existing FXML fields remain unchanged
    @FXML private MFXButton btnCheckout;
    @FXML private MFXButton btnDiscounts;
    @FXML private Pane cameraFrame;
    @FXML private TableColumn<SalesItem, BigDecimal> discountColumn;
    @FXML private Label discountPercentage;
    @FXML private MFXProgressBar discountedProdProgress;
    @FXML private TableColumn<SalesItem, BigDecimal> finalPriceColumn;
    @FXML private ImageView pictureDialog;
    @FXML private TableColumn<SalesItem, Integer> productIdColumn;
    @FXML private TableColumn<SalesItem, String> productNameColumn;
    @FXML private TableColumn<SalesItem, Integer> productQtyColumn;
    @FXML private ListView<String> productsListView;
    @FXML private StackPane saleMainFrame;
    @FXML private Pane salesContentPane;
    @FXML private TableView<SalesItem> salesTbl;
    @FXML private MFXTextField searchFld;
    @FXML private TableColumn<SalesItem, BigDecimal> subtotalColumn;
    @FXML private MFXProgressSpinner insertionProgress;
    @FXML private MFXRadioButton applyDiscBtn;
    @FXML private MFXTextField totalAmountFld;
    @FXML private MFXToggleButton toggleMode;
    @FXML private MFXProgressSpinner cameraLoadingSpinner;
    @FXML private MFXButton btnClearTable;

    private boolean isCameraMode = false;
    private Webcam webcam;
    private Thread captureThread;
    private final AtomicBoolean webcamActive = new AtomicBoolean(false);
    private Rectangle barcodeOverlay;
    private String lastScannedCode = "";
    private long lastScanTime = 0;
    private boolean isScanning = false;
    private ScheduledExecutorService scannerExecutor;
    private double lastProgress = 0.0;

    @FXML
    private void initialize() {
        productsListView.setVisible(true);
        productsListView.setOpacity(1.0);
        cameraFrame.setVisible(false);
        cameraFrame.setOpacity(0.0);
        searchFld.setDisable(false);

        toggleMode.setOnAction(event -> toggleMode());
        btnDiscounts.setOnAction(event -> openDiscountsManager());
        btnCheckout.setOnAction(event -> processCheckout());

        saleMainFrame.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                closeWebcam();
            }
        });

        // Add this clear table button handler
        btnClearTable.setOnAction(event -> {
            showConfirmationDialog("Are you sure you want to empty the table?", confirmed -> {
                if (confirmed) {
                    // Clear the table
                    salesTbl.getItems().clear();

                    // Reset the total amount field
                    totalAmountFld.setText("0.00");

                    // Show notification
                    showNotification("🔔 Sales table has been cleared");
                }
            });
        });

        setupSalesTable();
        setupProductSelectionHandler();
        setupSearchFilter();
        loadProductsList();
        setupDiscountButton();
        setupTableContextMenu();
        setupProductSelectionHandler();
        // Add discount column binding
        discountColumn.setCellValueFactory(cellData ->
                cellData.getValue().discountProperty());

        // Attach quantity listeners to existing SalesItems
        for (SalesItem item : salesTbl.getItems()) {
            item.quantityProperty().addListener((obs, oldVal, newVal) -> {
                if (applyDiscBtn.isSelected()) {
                    applyDiscountsToSales();
                }
            });
        }

        // Handle new SalesItems and quantity changes
        salesTbl.getItems().addListener((javafx.collections.ListChangeListener<SalesItem>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (SalesItem item : c.getAddedSubList()) {
                        // Attach quantity listener for future changes
                        item.quantityProperty().addListener((obs, oldVal, newVal) -> {
                            if (applyDiscBtn.isSelected()) {
                                applyDiscountsToSales();
                            }
                        });
                    }
                    // Apply discounts immediately if applyDiscBtn is selected
                    if (applyDiscBtn.isSelected() && !c.getAddedSubList().isEmpty()) {
                        applyDiscountsToSales();
                    }
                }
            }
        });
    }

    private void processCheckout() {
        if (salesTbl.getItems().isEmpty()) {
            showNotification("❌ No items to checkout");
            return;
        }

        // Update the total amount to ensure it reflects current quantities
        updateTotalAmount();

        List<SalesItem> items = new ArrayList<>(salesTbl.getItems());
        BigDecimal totalAmount = new BigDecimal(totalAmountFld.getText());

        insertionProgress.setVisible(true);
        insertionProgress.setProgress(0);

        new Thread(() -> {
            try (Connection conn = DBConnect.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    int saleId = -1;
                    try (CallableStatement stmt = conn.prepareCall("{CALL process_sale(?, ?)}")) {
                        stmt.setBigDecimal(1, totalAmount);
                        stmt.registerOutParameter(2, Types.INTEGER);
                        stmt.execute();
                        saleId = stmt.getInt(2);
                        if (saleId <= 0) {
                            throw new SQLException("Failed to get valid sale ID");
                        }
                    }

                    updateProgress(0.2);

                    try (CallableStatement stmt = conn.prepareCall("{CALL add_sale_item(?, ?, ?, ?, ?)}")) {
                        double progressStep = 0.8 / items.size();
                        double currentProgress = 0.2;

                        for (int i = 0; i < items.size(); i++) {
                            SalesItem item = items.get(i);
                            stmt.setInt(1, saleId);
                            stmt.setInt(2, item.getProductId());
                            stmt.setInt(3, item.getQuantity());
                            stmt.setBigDecimal(4, item.getSubtotal());
                            stmt.setBigDecimal(5, item.getFinalPrice());
                            stmt.execute();
                            currentProgress += progressStep;
                            updateProgress(currentProgress);
                        }
                    }

                    conn.commit();
                    updateProgress(1.0);

                    int finalSaleId = saleId;
                    Platform.runLater(() -> {
                        showPaymentDialog(totalAmount, finalSaleId);
                        insertionProgress.setVisible(false);
                    });

                } catch (SQLException e) {
                    conn.rollback();
                    e.printStackTrace();

                    // Check for the specific error message from the trigger
                    if (e.getMessage().contains("Insufficient stock")) {
                        Platform.runLater(() -> {
                            showNotification("❌ Insufficient stock for one or more products");
                            insertionProgress.setVisible(false);
                        });
                    } else {
                        Platform.runLater(() -> {
                            showNotification("❌ Error processing sale: " + e.getMessage());
                            insertionProgress.setVisible(false);
                        });
                    }
                } catch (Exception e) {
                    conn.rollback();
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        showNotification("❌ Error: " + e.getMessage());
                        insertionProgress.setVisible(false);
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showNotification("❌ Database connection error");
                    insertionProgress.setVisible(false);
                });
            }
        }).start();
    }

    private void showPaymentDialog(BigDecimal initialTotal, int saleId) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initOwner(btnCheckout.getScene().getWindow());

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #81B29A; -fx-border-width: 2; -fx-border-radius: 10;");

        Label titleLabel = new Label("Payment Details");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #81B29A;");

        MFXTextField totalAmountField = new MFXTextField();
        totalAmountField.setPromptText("Total Amount");
        totalAmountField.setText(initialTotal.setScale(2, RoundingMode.HALF_UP).toString());
        totalAmountField.setEditable(false);
        totalAmountField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        totalAmountField.setPrefWidth(200);

        MFXTextField paymentField = new MFXTextField();
        paymentField.setPromptText("Payment Amount");
        paymentField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        paymentField.setPrefWidth(200);
        paymentField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*(\\.\\d{0,2})?")) {
                return change;
            }
            return null;
        }));

        MFXTextField changeField = new MFXTextField();
        changeField.setPromptText("Change");
        changeField.setEditable(false);
        changeField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        changeField.setPrefWidth(200);

        MFXTextField customerNameField = new MFXTextField();
        customerNameField.setPromptText("Customer Name (for Credit)");
        customerNameField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        customerNameField.setPrefWidth(200);

        MFXDatePicker dueDatePicker = new MFXDatePicker();
        dueDatePicker.setPromptText("Due Date");
        dueDatePicker.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        dueDatePicker.setValue(LocalDate.now().plusDays(7));

        MFXButton payButton = new MFXButton("Pay");
        payButton.setStyle("-fx-background-color: #81B29A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px;");
        payButton.setDisable(true);

        MFXButton creditButton = new MFXButton("Credit");
        creditButton.setStyle("-fx-background-color: #81B29A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px;");

        HBox buttonBox = new HBox(10, payButton, creditButton);
        buttonBox.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(titleLabel, totalAmountField, paymentField, changeField, customerNameField, dueDatePicker, buttonBox);

        // Payment validation and button enabling
        paymentField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                BigDecimal payment = newValue.isEmpty() ? BigDecimal.ZERO : new BigDecimal(newValue);
                BigDecimal total = new BigDecimal(totalAmountField.getText());
                if (payment.compareTo(BigDecimal.ZERO) > 0) {
                    // Enable pay button for any positive payment
                    payButton.setDisable(false);

                    // Calculate change
                    BigDecimal change = payment.subtract(total);
                    if (change.compareTo(BigDecimal.ZERO) >= 0) {
                        changeField.setText(change.setScale(2, RoundingMode.HALF_UP).toString());
                    } else {
                        changeField.setText("0.00");
                    }
                } else {
                    // Disable pay button if no payment entered
                    payButton.setDisable(true);
                    changeField.setText("0.00");
                }

            } catch (NumberFormatException e) {
                payButton.setDisable(true);
                changeField.setText("0.00");

            }
        });

        payButton.setOnAction(e -> {
            try {
                BigDecimal payment = new BigDecimal(paymentField.getText());
                BigDecimal total = new BigDecimal(totalAmountField.getText());

                // Calculate remaining balance
                BigDecimal remainingBalance = total.subtract(payment);

                try (Connection conn = DBConnect.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE sales SET total_price = ? WHERE sale_id = ?")) {

                    if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                        // For full payment or overpayment, record the exact total price
                        stmt.setBigDecimal(1, total);
                    } else {
                        // For partial payment, record only the amount paid
                        stmt.setBigDecimal(1, payment);
                    }

                    stmt.setInt(2, saleId);
                    stmt.executeUpdate();
                }

                // If this is a full payment or overpayment
                if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                    dialog.close();
                    salesTbl.getItems().clear();
                    updateTotalAmount();

                    // Show appropriate message for exact payment vs overpayment
                    if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                        showNotification("✅ Payment completed with change: " +
                                remainingBalance.abs().setScale(2, RoundingMode.HALF_UP));
                    } else {
                        showNotification("✅ Payment completed successfully");
                    }
                } else {
                    // Partial payment - inform user but keep dialog open
                    showNotification("✅ Partial payment of " + payment + " recorded");

                    // Update the remaining amount to be credited
                    totalAmountField.setText(remainingBalance.toString());

                    // Disable pay button until new payment amount is entered
                    payButton.setDisable(true);

                    // Highlight the credit button as next action
                    creditButton.setStyle("-fx-background-color: #48c51d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px;");

                    // Update label to indicate remaining balance
                    titleLabel.setText("Remaining Balance: " + remainingBalance);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showNotification("❌ Error recording payment");
            } catch (NumberFormatException ex) {
                showNotification("❌ Invalid payment amount");
            }
        });

        creditButton.setOnAction(e -> {
            String customerName = customerNameField.getText().trim();
            if (customerName.isEmpty()) {
                showNotification("❌ Customer name is required for credit");
                return;
            }

            // Check if any payment has been made
            String paymentText = paymentField.getText().trim();
            boolean isFullCredit = paymentText.isEmpty() || new BigDecimal(paymentText.isEmpty() ? "0" : paymentText).compareTo(BigDecimal.ZERO) == 0;

            // Only update total_price to 0 if no payment was made (full credit)
            if (isFullCredit) {
                try (Connection conn = DBConnect.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE sales SET total_price = ? WHERE sale_id = ?")) {
                    stmt.setBigDecimal(1, BigDecimal.ZERO);
                    stmt.setInt(2, saleId);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showNotification("❌ Error updating sale record");
                    return;
                }
            }

            // Check if customer already exists and has contact information
            String existingPhone = "";
            String existingEmail = "";
            boolean customerExists = false;

            try (Connection conn = DBConnect.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT phone, email FROM customers WHERE name = ?")) {
                stmt.setString(1, customerName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    customerExists = true;
                    existingPhone = rs.getString("phone") != null ? rs.getString("phone") : "";
                    existingEmail = rs.getString("email") != null ? rs.getString("email") : "";
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                showNotification("❌ Error checking customer information");
            }

            // Skip contact dialog if customer exists with complete information
            if (customerExists && !existingPhone.isEmpty() && !existingEmail.isEmpty()) {
                BigDecimal amount = new BigDecimal(totalAmountField.getText());
                LocalDate dueDate = dueDatePicker.getValue();

                // Process credit directly with existing contact info
                processCreditWithContactInfo(customerName, existingPhone, existingEmail,
                        amount, dueDate, saleId, dialog);
                return;
            }

            // Create a dialog for phone and email
            Stage contactDialog = new Stage();
            contactDialog.initModality(Modality.APPLICATION_MODAL);
            contactDialog.initOwner(dialog.getScene().getWindow());
            contactDialog.initStyle(StageStyle.UNDECORATED);

            // Create dialog content
            VBox dialogVbox = new VBox(10);
            dialogVbox.setPadding(new Insets(20));
            dialogVbox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #81B29A; -fx-border-width: 2; -fx-border-radius: 10;");

            // FIXED: Renamed to avoid conflict with outer titleLabel
            Label contactTitleLabel = new Label("Customer Contact Information");
            contactTitleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #81B29A;");

            // Phone field with validation (numbers only)
            MFXTextField phoneField = new MFXTextField();
            phoneField.setPrefWidth(100);
            phoneField.setPromptText("Phone Number");
            phoneField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
            phoneField.setTextFormatter(new TextFormatter<>(change -> {
                if (change.getControlNewText().matches("\\d*")) {
                    return change;
                }
                return null;
            }));

            // Email field
            MFXTextField emailField = new MFXTextField();
            emailField.setPrefWidth(100);
            emailField.setPromptText("Email Address");
            emailField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);

            // Buttons
            MFXButton saveButton = new MFXButton("Save");
            saveButton.setStyle("-fx-background-color: #81B29A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px;");

            MFXButton skipButton = new MFXButton("Skip");
            skipButton.setStyle("-fx-background-color: #808080; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px;");

            // FIXED: Renamed to avoid conflict with outer buttonBox
            HBox contactButtonBox = new HBox(10, saveButton, skipButton);
            contactButtonBox.setAlignment(Pos.CENTER);

            dialogVbox.getChildren().addAll(contactTitleLabel, phoneField, emailField, contactButtonBox);

            // Save button action
            saveButton.setOnAction(event -> {
                // Validate email if provided
                String email = emailField.getText().trim();
                if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    showNotification("❌ Invalid email format");
                    return;
                }

                contactDialog.close();
                processCreditWithContactInfo(customerName, phoneField.getText(), email,
                        new BigDecimal(totalAmountField.getText()), dueDatePicker.getValue(), saleId, dialog);
            });

            // Skip button action
            skipButton.setOnAction(event -> {
                contactDialog.close();
                processCreditWithContactInfo(customerName, "", "",
                        new BigDecimal(totalAmountField.getText()), dueDatePicker.getValue(), saleId, dialog);
            });

            // Set up dialog visuals
            setupDialogVisuals(dialogVbox, contactDialog);
            contactDialog.showAndWait();
        });

        setupDialogVisuals(vbox, dialog);
        dialog.show();
    }

    // Helper method to process credit with contact info
    private void processCreditWithContactInfo(String customerName, String phone, String email,
                                              BigDecimal amount, LocalDate dueDate, int saleId, Stage parentDialog) {
        try {
            // Record the credit and update customer contact info
            try (Connection conn = DBConnect.getConnection()) {
                // First check if customer exists
                int customerId;
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT customer_id FROM customers WHERE name = ?")) {
                    checkStmt.setString(1, customerName);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next()) {
                        // Customer exists, update contact info
                        customerId = rs.getInt("customer_id");
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE customers SET phone = ?, email = ? WHERE customer_id = ?")) {
                            updateStmt.setString(1, phone);
                            updateStmt.setString(2, email);
                            updateStmt.setInt(3, customerId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Customer doesn't exist, create new
                        try (PreparedStatement insertStmt = conn.prepareStatement(
                                "INSERT INTO customers (name, phone, email) VALUES (?, ?, ?)",
                                Statement.RETURN_GENERATED_KEYS)) {
                            insertStmt.setString(1, customerName);
                            insertStmt.setString(2, phone);
                            insertStmt.setString(3, email);
                            insertStmt.executeUpdate();

                            ResultSet keys = insertStmt.getGeneratedKeys();
                            if (keys.next()) {
                                customerId = keys.getInt(1);
                            } else {
                                throw new SQLException("Creating customer failed, no ID obtained.");
                            }
                        }
                    }
                }

                // Record the credit transaction
                try (PreparedStatement creditStmt = conn.prepareStatement(
                        "INSERT INTO credit_transactions (customer_id, amount, status, due_date, sale_id) VALUES (?, ?, ?, ?, ?)")) {
                    creditStmt.setInt(1, customerId);
                    creditStmt.setBigDecimal(2, amount);
                    creditStmt.setString(3, "UNPAID");
                    creditStmt.setDate(4, dueDate != null ? java.sql.Date.valueOf(dueDate) : null);
                    creditStmt.setInt(5, saleId);
                    creditStmt.executeUpdate();
                }

                // Update customer's credit balance
                try (PreparedStatement balanceStmt = conn.prepareStatement(
                        "UPDATE customers SET credit_balance = credit_balance + ? WHERE customer_id = ?")) {
                    balanceStmt.setBigDecimal(1, amount);
                    balanceStmt.setInt(2, customerId);
                    balanceStmt.executeUpdate();
                }
            }

            parentDialog.close();
            salesTbl.getItems().clear();
            updateTotalAmount();
            showNotification("✅ Credit recorded successfully");
        } catch (SQLException ex) {
            ex.printStackTrace();
            showNotification("❌ Error recording credit");
        }
    }

    private void recordCreditSale(String customerName, BigDecimal amount, LocalDate dueDate, int saleId) {
        try (Connection conn = DBConnect.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int customerId;
                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT customer_id FROM customers WHERE name = ?");
                checkStmt.setString(1, customerName);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    customerId = rs.getInt("customer_id");
                } else {
                    PreparedStatement insertStmt = conn.prepareStatement(
                            "INSERT INTO customers (name, credit_balance) VALUES (?, 0.00)",
                            Statement.RETURN_GENERATED_KEYS);
                    insertStmt.setString(1, customerName);
                    insertStmt.executeUpdate();
                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        customerId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to create customer");
                    }
                }

                PreparedStatement txStmt = conn.prepareStatement(
                        "INSERT INTO credit_transactions (customer_id, amount, status, due_date, sale_id) VALUES (?, ?, 'UNPAID', ?, ?)");
                txStmt.setInt(1, customerId);
                txStmt.setBigDecimal(2, amount);
                txStmt.setDate(3, java.sql.Date.valueOf(dueDate));
                txStmt.setInt(4, saleId);
                txStmt.executeUpdate();

                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE customers SET credit_balance = credit_balance + ? WHERE customer_id = ?");
                updateStmt.setBigDecimal(1, amount);
                updateStmt.setInt(2, customerId);
                updateStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showNotification("❌ Error recording credit sale: " + e.getMessage());
        }
    }

    private void updateProgress(double progress) {
        // Create timeline for smooth animation
        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(
                insertionProgress.progressProperty(),
                progress,
                Interpolator.EASE_BOTH // Smooth acceleration and deceleration
        );

        // Duration based on the progress difference for consistent speed
        double progressDifference = Math.abs(progress - lastProgress);
        Duration duration = Duration.millis(300 * progressDifference);

        KeyFrame keyFrame = new KeyFrame(duration, keyValue);
        timeline.getKeyFrames().add(keyFrame);

        // Play the animation
        Platform.runLater(() -> {
            timeline.play();
            lastProgress = progress;
        });
    }

    private void setupDiscountButton() {
        applyDiscBtn.setOnAction(event -> {
            if (applyDiscBtn.isSelected()) {
                applyDiscountsToSales();
            } else {
                for (SalesItem item : salesTbl.getItems()) {
                    item.setDiscount(BigDecimal.ZERO);
                }
                updateDiscountProgress(0.0);
                salesTbl.refresh();
                updateTotalAmount();
                showNotification("🔔 Discounts removed");
            }
        });
    }

    private void applyDiscountsToSales() {
        try (Connection conn = DBConnect.getConnection()) {
            // Set to store category IDs with active discounts
            Set<Integer> discountedCategoryIds = new HashSet<>();

            // Fetch category IDs with active discounts
            try (PreparedStatement catStmt = conn.prepareStatement(
                    "SELECT DISTINCT category_id FROM discounts " +
                            "WHERE is_active = 1 AND CURRENT_TIMESTAMP BETWEEN start_date AND end_date " +
                            "AND category_id IS NOT NULL")) {
                ResultSet catRs = catStmt.executeQuery();
                while (catRs.next()) {
                    discountedCategoryIds.add(catRs.getInt("category_id"));
                }
            }

            // Map to store product ID to category ID
            Map<Integer, Integer> productCategoryMap = new HashMap<>();
            try (PreparedStatement prodStmt = conn.prepareStatement(
                    "SELECT product_id, category_id FROM products")) {
                ResultSet prodRs = prodStmt.executeQuery();
                while (prodRs.next()) {
                    productCategoryMap.put(prodRs.getInt("product_id"), prodRs.getInt("category_id"));
                }
            }

            // Fetch discounts
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT d.product_id, d.category_id, d.discount_type, d.discount_value, d.min_quantity, " +
                            "p.selling_price, p.category_id AS product_category_id " +
                            "FROM discounts d " +
                            "LEFT JOIN products p ON d.product_id = p.product_id OR d.category_id = p.category_id " +
                            "WHERE d.is_active = 1 AND CURRENT_TIMESTAMP BETWEEN d.start_date AND d.end_date")) {

                ResultSet rs = stmt.executeQuery();
                Map<Integer, Map<String, BigDecimal>> discountMap = new HashMap<>();
                Map<Integer, Integer> minQuantityMap = new HashMap<>();

                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    int categoryId = rs.getInt("category_id");
                    String discountType = rs.getString("discount_type");
                    BigDecimal discountValue = rs.getBigDecimal("discount_value");
                    int minQuantity = rs.getInt("min_quantity");
                    BigDecimal sellingPrice = rs.getBigDecimal("selling_price");
                    int productCategoryId = rs.getInt("product_category_id");

                    boolean isProductIdNull = rs.getObject("product_id") == null;
                    boolean isCategoryIdNull = rs.getObject("category_id") == null;

                    if ((productId == 0 || isProductIdNull) && (categoryId == 0 || isCategoryIdNull)) {
                        continue;
                    }

                    if (productId != 0 && !isProductIdNull) {
                        // Check if the product's category has a discount
                        Integer prodCategoryId = productCategoryMap.get(productId);
                        if (prodCategoryId != null && discountedCategoryIds.contains(prodCategoryId)) {
                            // Skip product-specific discount if category has a discount
                            continue;
                        }
                        applyDiscountToProduct(productId, discountType, discountValue, minQuantity, sellingPrice, discountMap, minQuantityMap);
                    }
                    if (categoryId != 0 && !isCategoryIdNull) {
                        try (PreparedStatement catStmt = conn.prepareStatement(
                                "SELECT product_id, selling_price FROM products WHERE category_id = ?")) {
                            catStmt.setInt(1, categoryId);
                            ResultSet catRs = catStmt.executeQuery();
                            while (catRs.next()) {
                                int catProductId = catRs.getInt("product_id");
                                BigDecimal catSellingPrice = catRs.getBigDecimal("selling_price");
                                applyDiscountToProduct(catProductId, discountType, discountValue, minQuantity, catSellingPrice, discountMap, minQuantityMap);
                            }
                        }
                    }
                }

                int discountedItems = 0;
                int totalItems = salesTbl.getItems().size();

                for (SalesItem item : salesTbl.getItems()) {
                    Map<String, BigDecimal> typeDiscounts = discountMap.getOrDefault(item.getProductId(), new HashMap<>());
                    Integer minQuantity = minQuantityMap.getOrDefault(item.getProductId(), 1);

                    if (item.getQuantity() < minQuantity) {
                        item.setDiscount(BigDecimal.ZERO);
                        continue;
                    }

                    BigDecimal totalDiscount = BigDecimal.ZERO;
                    int qty = item.getQuantity();

                    BigDecimal percentageDiscount = typeDiscounts.getOrDefault("PERCENTAGE", BigDecimal.ZERO);
                    BigDecimal fixedDiscount = typeDiscounts.getOrDefault("FIXED", BigDecimal.ZERO);
                    BigDecimal bogoDiscount = typeDiscounts.getOrDefault("BOGO", BigDecimal.ZERO);
                    BigDecimal bulkDiscount = typeDiscounts.getOrDefault("BULK", BigDecimal.ZERO);

                    totalDiscount = totalDiscount.add(percentageDiscount).add(fixedDiscount);

                    if (bogoDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        int discountedItemsCount = qty / 2;
                        if (discountedItemsCount > 0) {
                            BigDecimal effectiveBogo = bogoDiscount.multiply(BigDecimal.valueOf(discountedItemsCount))
                                    .divide(BigDecimal.valueOf(qty), 4, RoundingMode.HALF_UP);
                            effectiveBogo = effectiveBogo.max(BigDecimal.valueOf(50)); // Cap at 50%
                            totalDiscount = totalDiscount.add(effectiveBogo);
                        }
                    }

                    totalDiscount = totalDiscount.add(bulkDiscount);

                    item.setDiscount(totalDiscount.setScale(2, RoundingMode.HALF_UP));
                    if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        discountedItems++;
                    }
                }

                double discountPercentageValue = totalItems > 0 ? (double) discountedItems / totalItems : 0;
                updateDiscountProgress(discountPercentageValue);

                salesTbl.refresh();
                updateTotalAmount();

                showNotification("🔔 Discounts applied successfully");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showNotification("❌ Error applying discounts: " + e.getMessage());
        }
    }

    private void applyDiscountToProduct(int productId, String discountType, BigDecimal discountValue,
                                        int minQuantity, BigDecimal sellingPrice,
                                        Map<Integer, Map<String, BigDecimal>> discountMap,
                                        Map<Integer, Integer> minQuantityMap) {
        discountMap.computeIfAbsent(productId, k -> new HashMap<>());
        Map<String, BigDecimal> typeDiscounts = discountMap.get(productId);

        BigDecimal currentDiscount = typeDiscounts.getOrDefault(discountType, BigDecimal.ZERO);
        int currentMinQuantity = minQuantityMap.getOrDefault(productId, 1);

        BigDecimal discountPercentage = BigDecimal.ZERO;
        if ("PERCENTAGE".equals(discountType)) {
            discountPercentage = discountValue;
        } else if ("FIXED".equals(discountType) && sellingPrice != null && sellingPrice.compareTo(BigDecimal.ZERO) > 0) {
            discountPercentage = discountValue.divide(sellingPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else if ("BOGO".equals(discountType) || "BULK".equals(discountType)) {
            discountPercentage = discountValue;
        }

        typeDiscounts.put(discountType, currentDiscount.add(discountPercentage).setScale(2, RoundingMode.HALF_UP));
        minQuantityMap.put(productId, Math.max(currentMinQuantity, minQuantity));
    }

    private void updateDiscountProgress(double progress) {
        String percentageText = String.format("%.1f%% of product/s discounted", progress * 100);
        Platform.runLater(() -> {
            discountPercentage.setText(percentageText);

            Timeline timeline = new Timeline();
            double currentProgress = discountedProdProgress.getProgress();
            double targetProgress = progress;

            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(500),
                    new javafx.animation.KeyValue(discountedProdProgress.progressProperty(), targetProgress, javafx.animation.Interpolator.EASE_BOTH)
            );

            timeline.getKeyFrames().add(keyFrame);
            timeline.play();
        });
    }

    private void showConfirmationDialog(String message, Consumer<Boolean> resultHandler) {
        // Create custom alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/sales.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 10px;" +
                "-fx-border-radius: 10px;" +
                "-fx-border-color: #81B29A;" +
                "-fx-border-width: 2px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.2, 0.1, 0);");

        // Style the buttons
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        String buttonStyle = "-fx-background-color: #81B29A;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 8px 20px;";

        okButton.setStyle(buttonStyle);
        cancelButton.setStyle(buttonStyle);

        // Set result handler
        alert.showAndWait().ifPresent(result -> {
            resultHandler.accept(result == ButtonType.OK);
        });
    }

    private void showNotification(String message) {
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
        double notificationWidth = 240;
        double notificationHeight = 40;

        label.setPrefWidth(notificationWidth);
        label.setPrefHeight(notificationHeight);
        label.setAlignment(Pos.CENTER);

        notification.setPrefWidth(notificationWidth);
        notification.setPrefHeight(notificationHeight);
        notification.setLayoutX(salesContentPane.getWidth() - notificationWidth - 20);
        notification.setLayoutY(salesContentPane.getHeight() - notificationHeight - 20);

        // Add to scene and animate
        salesContentPane.getChildren().add(notification);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.setOnFinished(e -> salesContentPane.getChildren().remove(notification));
        fadeOut.play();
    }

    private void openDiscountsManager() {
        try {
            // Show loading indicator first
            Stage loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.TRANSPARENT);
            loadingStage.initModality(Modality.APPLICATION_MODAL);
            loadingStage.initOwner(btnDiscounts.getScene().getWindow());

            ProgressIndicator progress = new ProgressIndicator();
            progress.setStyle("-fx-progress-color: #81B29A;");

            Label loadingLabel = new Label("Opening Discounts Manager...");
            loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

            VBox loadingBox = new VBox(10, progress, loadingLabel);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(20));
            loadingBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

            Scene loadingScene = new Scene(loadingBox);
            loadingScene.setFill(Color.TRANSPARENT);
            loadingStage.setScene(loadingScene);
            loadingStage.show();

            // Load the dialog in background thread
            new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/product-discount.fxml"));
                    Parent root = loader.load();
                    root.getStylesheets().add(getClass().getResource("/css/product-discount.css").toExternalForm());

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            loadingStage.close();

                            // Create stage
                            Stage stage = new Stage();
                            stage.initModality(Modality.APPLICATION_MODAL);
                            stage.initOwner(btnDiscounts.getScene().getWindow());

                            // Apply visual styling using helper method
                            setupDialogVisuals(root, stage);

                            // Show the stage
                            stage.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        loadingStage.close();
                        // Show error alert if needed
                    });
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupDialogVisuals(Parent root, Stage stage) {
        // Set stage style
        stage.initStyle(StageStyle.UNDECORATED);

        // Configure scene with transparent background
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/sales.css").toExternalForm());
        stage.setScene(scene);

        // Apply rounded corners
        Rectangle clip = new Rectangle(root.prefWidth(-1), root.prefHeight(-1));
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        root.setClip(clip);

        // Add shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(15);
        dropShadow.setSpread(0.05);
        dropShadow.setOffsetY(3);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));

        // Create background panel to receive shadow
        AnchorPane shadowReceiver = new AnchorPane();
        shadowReceiver.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        shadowReceiver.setPrefSize(root.prefWidth(-1) - 2, root.prefHeight(-1) - 2);
        shadowReceiver.setEffect(dropShadow);

        // Insert shadow receiver behind other content based on layout type
        if (root instanceof AnchorPane) {
            ((AnchorPane)root).getChildren().add(0, shadowReceiver);
        } else if (root instanceof VBox) {
            ((VBox)root).getChildren().add(0, shadowReceiver);
        } else if (root instanceof Pane) {
            ((Pane)root).getChildren().add(0, shadowReceiver);
        }

        // Adjust clip and shadow receiver on resize
        root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            clip.setWidth(newValue.getWidth());
            clip.setHeight(newValue.getHeight());
            shadowReceiver.setPrefSize(newValue.getWidth() - 2, newValue.getHeight() - 2);
        });

        // Make window draggable
        AtomicReference<Double> xOffset = new AtomicReference<>((double) 0);
        AtomicReference<Double> yOffset = new AtomicReference<>((double) 0);

        root.setOnMousePressed(event -> {
            xOffset.set(event.getSceneX());
            yOffset.set(event.getSceneY());
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset.get());
            stage.setY(event.getScreenY() - yOffset.get());
        });

        // Add fade-in animation
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void setupSalesTable() {
        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("productId"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discount"));
        finalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));

        // Custom cell factory for productQtyColumn to show context menu on double-click
        productQtyColumn.setCellFactory(col -> new QuantityContextMenuCell());

        if (salesTbl.getItems() == null) {
            salesTbl.setItems(FXCollections.observableArrayList());
        }
    }

    private void setupTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #81B29A;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 5px;" +
                "-fx-background-radius: 5px;");

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

        MenuItem deleteMultipleItem = new MenuItem("Delete Multiple");
        deleteMultipleItem.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

        // Delete single item action
        deleteItem.setOnAction(event -> {
            SalesItem selectedItem = salesTbl.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                salesTbl.getItems().remove(selectedItem);
                updateTotalAmount();
                showNotification("🔔 Item removed from sale");
            }
        });

        // Delete multiple items action
        deleteMultipleItem.setOnAction(event -> {
            enableMultipleSelection();
        });

        contextMenu.getItems().addAll(deleteItem, deleteMultipleItem);

        // Attach context menu to table rows
        salesTbl.setRowFactory(tv -> {
            TableRow<SalesItem> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    private void enableMultipleSelection() {
        // Create a selection dialog
        Stage selectionStage = new Stage();
        selectionStage.initModality(Modality.APPLICATION_MODAL);
        selectionStage.initOwner(salesTbl.getScene().getWindow());
        selectionStage.setTitle("Select Items to Delete");
        selectionStage.initStyle(StageStyle.TRANSPARENT); // Make stage transparent for rounded corners

        // Clone current table with checkboxes
        TableView<SalesItem> selectionTable = new TableView<>();
        selectionTable.setPrefHeight(400);
        selectionTable.setPrefWidth(600);

        // Apply the same styling as salesTbl
        selectionTable.setId("salesTbl");
        selectionTable.setEditable(true);

        // Selection column with styled checkbox
        TableColumn<SalesItem, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(column -> {
            CheckBoxTableCell<SalesItem, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            cell.setStyle("-fx-background-color: transparent; -fx-padding: 5px;");
            return cell;
        });

        // Load checkbox styling with CSS
        selectionTable.getStylesheets().add(SalesController.class.getResource("/css/sales.css").toExternalForm());
        selectionTable.getStylesheets().add(SalesController.class.getResource("/css/checkbox-style.css").toExternalForm());

        selectCol.setEditable(true);
        selectCol.setPrefWidth(70);

        // Add ID, name and quantity columns
        TableColumn<SalesItem, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("productId"));

        TableColumn<SalesItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setPrefWidth(380);

        TableColumn<SalesItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(70);

        selectionTable.getColumns().addAll(selectCol, idCol, nameCol, qtyCol);

        // Reset selected state
        for (SalesItem item : salesTbl.getItems()) {
            item.setSelected(false);
        }
        selectionTable.getItems().addAll(salesTbl.getItems());

        // Add select all option with styled checkbox
        MFXCheckbox selectAllCheckbox = new MFXCheckbox("Select All");
        selectAllCheckbox.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        selectAllCheckbox.setOnAction(e -> {
            boolean selectAll = selectAllCheckbox.isSelected();
            for (SalesItem item : selectionTable.getItems()) {
                item.setSelected(selectAll);
            }
            selectionTable.refresh();
        });

        // Styled buttons
        MFXButton deleteBtn = new MFXButton("Delete Selected");
        deleteBtn.setPrefHeight(45);
        deleteBtn.setPrefWidth(160);
        deleteBtn.setStyle("-fx-background-color: #81B29A; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 8px; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10px 20px;");

        MFXButton cancelBtn = new MFXButton("Cancel");
        cancelBtn.setPrefHeight(45);
        cancelBtn.setPrefWidth(120);
        cancelBtn.setStyle("-fx-background-color: #E07A5F; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 8px; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10px 20px;");

        // Add hover and pressed effects
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteBtn.getStyle() + "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(deleteBtn.getStyle().replace("-fx-scale-x: 1.05; -fx-scale-y: 1.05;", "")));
        deleteBtn.setOnMousePressed(e -> deleteBtn.setStyle(deleteBtn.getStyle() + "-fx-scale-x: 0.95; -fx-scale-y: 0.95;"));
        deleteBtn.setOnMouseReleased(e -> deleteBtn.setStyle(deleteBtn.getStyle().replace("-fx-scale-x: 0.95; -fx-scale-y: 0.95;", "")));

        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelBtn.getStyle() + "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(cancelBtn.getStyle().replace("-fx-scale-x: 1.05; -fx-scale-y: 1.05;", "")));
        cancelBtn.setOnMousePressed(e -> cancelBtn.setStyle(cancelBtn.getStyle() + "-fx-scale-x: 0.95; -fx-scale-y: 0.95;"));
        cancelBtn.setOnMouseReleased(e -> cancelBtn.setStyle(cancelBtn.getStyle().replace("-fx-scale-x: 0.95; -fx-scale-y: 0.95;", "")));

        HBox buttonsBox = new HBox(15, deleteBtn, cancelBtn);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(15));

        // Delete button action
        deleteBtn.setOnAction(event -> {
            java.util.List<SalesItem> itemsToRemove = new ArrayList<>();

            for (SalesItem item : selectionTable.getItems()) {
                if (item.isSelected()) {
                    itemsToRemove.add(item);
                }
            }

            salesTbl.getItems().removeAll(itemsToRemove);
            updateTotalAmount();

            if (!itemsToRemove.isEmpty()) {
                showNotification("🔔 " + itemsToRemove.size() + " item(s) removed");
            }

            selectionStage.close();
        });

        cancelBtn.setOnAction(event -> selectionStage.close());

        // Add a title label
        Label titleLabel = new Label("Select Items to Delete");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #81B29A;");
        titleLabel.setPadding(new Insets(0, 0, 10, 0));

        // Create a styled layout
        VBox layout = new VBox(10, titleLabel, selectAllCheckbox, selectionTable, buttonsBox);
        layout.setStyle("-fx-background-color: #FAF9F6; -fx-padding: 20;");
        layout.setPadding(new Insets(20));

        // Add drop shadow
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(15);
        dropShadow.setSpread(0.05);
        dropShadow.setOffsetY(3);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        layout.setEffect(dropShadow);

        // Set the scene with proper styling
        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); // Make scene background transparent
        scene.getStylesheets().add(getClass().getResource("/css/sales.css").toExternalForm());

        // Apply rounded corners with clip
        Rectangle clip = new Rectangle(layout.getPrefWidth(), layout.getPrefHeight());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        layout.setClip(clip);

        // Make the clip resize with the layout
        layout.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            clip.setWidth(newValue.getWidth());
            clip.setHeight(newValue.getHeight());
        });

        // Make window draggable
        AtomicReference<Double> xOffset = new AtomicReference<>((double) 0);
        AtomicReference<Double> yOffset = new AtomicReference<>((double) 0);

        layout.setOnMousePressed(event -> {
            xOffset.set(event.getSceneX());
            yOffset.set(event.getSceneY());
        });

        layout.setOnMouseDragged(event -> {
            selectionStage.setX(event.getScreenX() - xOffset.get());
            selectionStage.setY(event.getScreenY() - yOffset.get());
        });

        selectionStage.setScene(scene);
        selectionStage.showAndWait();
    }

    // Custom TableCell with ContextMenu for quantity adjustments
    private class QuantityContextMenuCell extends TableCell<SalesItem, Integer> {
        private final ContextMenu contextMenu;
        private final TextField textField;
        private boolean isAdding = true; // Tracks whether adding or deducting

        public QuantityContextMenuCell() {
            // Initialize styled ContextMenu
            contextMenu = new ContextMenu();
            contextMenu.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #81B29A;" +
                            "-fx-border-width: 2px;" +
                            "-fx-border-radius: 5px;" +
                            "-fx-background-radius: 5px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.2, 0, 1);"
            );

            // Menu items
            MenuItem addQtyItem = new MenuItem("Add Qty");
            MenuItem deductQtyItem = new MenuItem("Deduct Qty");

            // Style menu items
            String menuItemStyle =
                    "-fx-padding: 5px 10px;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #333333;";
            addQtyItem.setStyle(menuItemStyle);
            deductQtyItem.setStyle(menuItemStyle);

            contextMenu.getItems().addAll(addQtyItem, deductQtyItem);

            // Initialize styled TextField
            textField = new TextField();
            textField.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #81B29A;" +
                            "-fx-border-radius: 5px;" +
                            "-fx-background-radius: 5px;" +
                            "-fx-text-fill: #333333;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 5px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.2, 0, 1);"
            );
            textField.setPrefWidth(80);

            // Restrict input to positive integers
            textField.setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.matches("\\d*") && !newText.isEmpty()) {
                    try {
                        int value = Integer.parseInt(newText);
                        if (value > 0) {
                            return change;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid numbers
                    }
                }
                return null;
            }));

            // Commit on Enter
            textField.setOnAction(event -> {
                try {
                    int value = Integer.parseInt(textField.getText());
                    if (value > 0) {
                        SalesItem item = getTableRow().getItem();
                        if (isAdding) {
                            item.setQuantity(value);
                        } else {
                            // For deducting, ensure we don't go below 1
                            int newQty = Math.max(1, item.getQuantity() - value);
                            item.setQuantity(newQty);

                        }
                        commitEdit(item.getQuantity());
                    }
                    cancelEdit();
                } catch (NumberFormatException e) {
                    cancelEdit();
                }
            });

            // Cancel on focus loss
            textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    try {
                        int value = Integer.parseInt(textField.getText());
                        if (value > 0) {
                            SalesItem item = getTableRow().getItem();
                            if (isAdding) {
                                item.setQuantity(value);
                            } else {
                                // For deducting, ensure we don't go below 1
                                int newQty = Math.max(1, item.getQuantity() - value);
                                item.setQuantity(newQty);
                            }
                            commitEdit(item.getQuantity());
                        }
                        cancelEdit();
                    } catch (NumberFormatException e) {
                        cancelEdit();
                    }
                }
            });

            // ContextMenu actions
            addQtyItem.setOnAction(event -> {
                isAdding = true;
                startEditing();
            });

            deductQtyItem.setOnAction(event -> {
                isAdding = false;
                startEditing();
            });

            // Show ContextMenu on double-click
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !isEmpty() && !isEditing()) {
                    contextMenu.show(this, event.getScreenX(), event.getScreenY());
                }
            });
        }

        private void startEditing() {
            startEdit();
        }

        @Override
        public void startEdit() {
            super.startEdit();
            SalesItem item = getTableRow().getItem();
            if (item != null) {
                textField.setText("");
                setText(null);
                setGraphic(textField);
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            SalesItem item = getTableRow().getItem();
            if (item != null) {
                setText(String.valueOf(item.getQuantity()));
                setGraphic(null);
            }
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        }

        @Override
        public void commitEdit(Integer newValue) {
            super.commitEdit(newValue);
            setText(String.valueOf(newValue));
            setGraphic(null);

            updateTotalAmount();
        }
    }

    private void setupProductSelectionHandler() {
        productsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = productsListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    createFlyingAnimation(selectedItem);
                }
            }
        });
    }

    private void createFlyingAnimation(String selectedItem) {
        try {
            int productId = Integer.parseInt(selectedItem.split(" - ")[0]);
            String productName = selectedItem.substring(selectedItem.indexOf(" - ") + 3);
            // Use dynamic table position
            double endX = salesTbl.getBoundsInParent().getMinX() + 100;
            double endY = salesTbl.getBoundsInParent().getMinY() + 30;
            createFlyingAnimation(selectedItem, 400, 30, endX, endY, productId, productName, "#81B29A", "white");
        } catch (Exception e) {
            e.printStackTrace();
            addProductToSalesTable(
                    Integer.parseInt(selectedItem.split(" - ")[0]),
                    selectedItem.substring(selectedItem.indexOf(" - ") + 3)
            );
        }
    }

    private void createFlyingAnimation(String text, double startX, double startY,
                                       double endX, double endY, int productId, String productName,
                                       String bgColor, String textColor) {
        try {
            Label flyingLabel = new Label(text);
            flyingLabel.getStyleClass().add("flying-item");
            flyingLabel.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                            "-fx-text-fill: " + textColor + ";" +
                            "-fx-padding: 10px 20px;" +
                            "-fx-background-radius: 12px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 14px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.2, 0, 3);"
            );

            // Validate coordinates
            if (startX > 1030 || startY > 583 || endX > 1030 || endY > 583 ||
                    startX < 0 || startY < 0 || endX < 0 || endY < 0) {
                System.err.println("Coordinates out of bounds: startX=" + startX + ", startY=" + startY +
                        ", endX=" + endX + ", endY=" + endY);
            }

            // Create curved path
            Path path = new Path();
            path.getElements().add(new MoveTo(startX, startY));
            double controlX = (startX + endX) / 2;
            double controlY = Math.min(startY, endY) - 150;
            path.getElements().add(new QuadCurveTo(controlX, controlY, endX, endY));

            // Path transition
            PathTransition pathTransition = new PathTransition();
            pathTransition.setDuration(Duration.seconds(0.8));
            pathTransition.setPath(path);
            pathTransition.setNode(flyingLabel);
            pathTransition.setOrientation(PathTransition.OrientationType.NONE);
            pathTransition.setCycleCount(1);

            // Scale transition
            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(1.0), flyingLabel);
            scaleTransition.setFromX(0.8);
            scaleTransition.setFromY(0.8);
            scaleTransition.setToX(1.2);
            scaleTransition.setToY(1.2);
            scaleTransition.setAutoReverse(true);
            scaleTransition.setCycleCount(2);

            // Rotation transition
            RotateTransition rotateTransition = new RotateTransition(Duration.seconds(1.0), flyingLabel);
            rotateTransition.setFromAngle(-10);
            rotateTransition.setToAngle(10);
            rotateTransition.setCycleCount(2);
            rotateTransition.setAutoReverse(true);

            // Fade transition
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(350), flyingLabel);
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.0);
            fadeTransition.setDelay(Duration.millis(350));

            // Combine animations
            ParallelTransition parallelTransition = new ParallelTransition(
                    pathTransition, scaleTransition, rotateTransition, fadeTransition
            );

            // Add label to scene
            saleMainFrame.getChildren().add(flyingLabel);
            flyingLabel.setLayoutX(startX - flyingLabel.getBoundsInLocal().getWidth() / 2);
            flyingLabel.setLayoutY(startY - flyingLabel.getBoundsInLocal().getHeight() / 2);
            flyingLabel.toFront();

            // Handle completion
            parallelTransition.setOnFinished(e -> {
                saleMainFrame.getChildren().remove(flyingLabel);
                addProductToSalesTable(productId, productName);
            });

            parallelTransition.play();
        } catch (Exception e) {
            e.printStackTrace();
            addProductToSalesTable(productId, productName);
        }
    }

    private void addProductToSalesTable(int productId, String productName) {
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT selling_price FROM products WHERE product_id = ?")) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal price = rs.getBigDecimal("selling_price");

                boolean productExists = false;
                for (SalesItem item : salesTbl.getItems()) {
                    if (item.getProductId() == productId) {
                        item.setQuantity(item.getQuantity() + 1);
                        productExists = true;
                        break;
                    }
                }

                if (!productExists) {
                    SalesItem newItem = new SalesItem(productId, productName, price);
                    salesTbl.getItems().add(newItem);
                }

                salesTbl.refresh();
                updateTotalAmount();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (SalesItem item : salesTbl.getItems()) {
            total = total.add(item.getFinalPrice());
        }
        totalAmountFld.setText(total.setScale(2, RoundingMode.HALF_UP).toString());
    }

    private void loadProductsList() {
        productsListView.getItems().clear();
        cameraLoadingSpinner.setVisible(true);

        new Thread(() -> {
            try (Connection conn = DBConnect.getConnection();
                 CallableStatement stmt = conn.prepareCall("{CALL GetAllProducts()}")) {

                ResultSet rs = stmt.executeQuery();
                java.util.List<String> items = new java.util.ArrayList<>();

                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");
                    items.add(id + " - " + name);
                }

                items.sort((item1, item2) -> {
                    int id1 = Integer.parseInt(item1.split(" - ")[0]);
                    int id2 = Integer.parseInt(item2.split(" - ")[0]);
                    return Integer.compare(id1, id2);
                });

                Platform.runLater(() -> {
                    productsListView.getItems().addAll(items);
                    cameraLoadingSpinner.setVisible(false);
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> cameraLoadingSpinner.setVisible(false));
            }
        }).start();
    }

    private void setupSearchFilter() {
        searchFld.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                loadProductsList();
            } else {
                String searchTerm = newValue.toLowerCase();
                productsListView.getItems().clear();

                try (Connection conn = DBConnect.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT product_id, name FROM products WHERE LOWER(name) LIKE ? ORDER BY product_id")) {

                    stmt.setString(1, "%" + searchTerm + "%");
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("product_id");
                            String name = rs.getString("name");
                            productsListView.getItems().add(id + " - " + name);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void animateProgress() {
        Thread progressThread = new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 5) {
                    final int progress = i;
                    Platform.runLater(() -> cameraLoadingSpinner.setProgress(progress / 100.0));
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();
    }

    private void initWebcam() {
        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(new Dimension(320, 240));
                webcam.open(true);

                pictureDialog.setPreserveRatio(false);
                pictureDialog.setFitWidth(325);
                pictureDialog.setFitHeight(234);

                pictureDialog.setLayoutX((cameraFrame.getWidth() - 325) / 2);
                pictureDialog.setLayoutY((cameraFrame.getHeight() - 234) / 2);

                cameraFrame.setStyle("-fx-background-color: transparent;");
                startCaptureThread();
            } else {
                System.err.println("No webcam detected");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCaptureThread() {
        webcamActive.set(true);

        barcodeOverlay = new Rectangle(0, 0, 200, 80);
        barcodeOverlay.setStroke(javafx.scene.paint.Color.GREEN);
        barcodeOverlay.setStrokeWidth(3);
        barcodeOverlay.setFill(javafx.scene.paint.Color.TRANSPARENT);
        barcodeOverlay.setVisible(false);

        Platform.runLater(() -> cameraFrame.getChildren().add(barcodeOverlay));

        scannerExecutor = Executors.newSingleThreadScheduledExecutor();

        captureThread = new Thread(() -> {
            while (webcamActive.get() && !Thread.interrupted()) {
                try {
                    if (webcam != null && webcam.isOpen()) {
                        BufferedImage currentFrame = webcam.getImage();
                        if (currentFrame != null) {
                            final javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(currentFrame, null);

                            if (!isScanning) {
                                isScanning = true;
                                final BufferedImage frameForScanning = currentFrame;

                                scannerExecutor.schedule(() -> {
                                    try {
                                        scanForBarcode(frameForScanning);
                                    } finally {
                                        isScanning = false;
                                    }
                                }, 0, TimeUnit.MILLISECONDS);
                            }

                            Platform.runLater(() -> {
                                if (pictureDialog != null) {
                                    pictureDialog.setImage(fxImage);
                                }
                            });
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void scanForBarcode(BufferedImage image) {
        try {
            // Enforce 3-second interval
            if (System.currentTimeMillis() - lastScanTime < 3000) {
                isScanning = false; // Reset scanning flag
                return;
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            try {
                Result result = reader.decode(bitmap);
                String scannedCode = result.getText();

                if (scannedCode.equals(lastScannedCode)) {
                    isScanning = false; // Reset scanning flag when duplicate code found
                    return;
                }

                lastScannedCode = scannedCode;
                lastScanTime = System.currentTimeMillis();

                ResultPoint[] points = result.getResultPoints();
                if (points != null && points.length >= 2) {
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = 0, maxY = 0;

                    for (ResultPoint point : points) {
                        minX = Math.min(minX, point.getX());
                        minY = Math.min(minY, point.getY());
                        maxX = Math.max(maxX, point.getX());
                        maxY = Math.max(maxY, point.getY());
                    }

                    // Calculate original barcode dimensions with padding
                    float padding = 20; // Reduced padding to prevent overflow
                    float width = maxX - minX + 2 * padding;
                    float height = maxY - minY + 2 * padding;

                    // Scale coordinates to match pictureDialog (325x234) from webcam (320x240)
                    double scaleX = pictureDialog.getFitWidth() / 320.0; // 325 / 320
                    double scaleY = pictureDialog.getFitHeight() / 240.0; // 234 / 240
                    double scaledMinX = minX * scaleX;
                    double scaledMinY = minY * scaleY;
                    double scaledWidth = width * scaleX;
                    double scaledHeight = height * scaleY;

                    // Adjust for pictureDialog's position within cameraFrame
                    double offsetX = pictureDialog.getLayoutX();
                    double offsetY = pictureDialog.getLayoutY();

                    // Clamp coordinates to stay within pictureDialog bounds
                    double finalMinX = Math.max(0, Math.min(scaledMinX + offsetX, pictureDialog.getFitWidth() - scaledWidth));
                    double finalMinY = Math.max(0, Math.min(scaledMinY + offsetY, pictureDialog.getFitHeight() - scaledHeight));
                    double finalWidth = Math.min(scaledWidth, pictureDialog.getFitWidth() - finalMinX + offsetX);
                    double finalHeight = Math.min(scaledHeight, pictureDialog.getFitHeight() - finalMinY + offsetY);

                    Platform.runLater(() -> {
                        barcodeOverlay.setX(finalMinX);
                        barcodeOverlay.setY(finalMinY);
                        barcodeOverlay.setWidth(finalWidth);
                        barcodeOverlay.setHeight(finalHeight);
                        barcodeOverlay.setVisible(true);

                        FadeTransition fade = new FadeTransition(Duration.millis(500), barcodeOverlay);
                        fade.setFromValue(1.0);
                        fade.setToValue(0.0);
                        fade.setDelay(Duration.millis(500));
                        fade.setCycleCount(2);
                        fade.setAutoReverse(true);
                        fade.play();
                    });

                    lookupProductByBarcode(scannedCode);
                }
            } catch (NotFoundException e) {
                Platform.runLater(() -> barcodeOverlay.setVisible(false));
            } finally {
                isScanning = false; // Always reset scanning flag when done
            }
        } catch (Exception e) {
            isScanning = false; // Reset scanning flag on error
            e.printStackTrace();
        }
    }

    private void lookupProductByBarcode(String barcode) {
        new Thread(() -> {
            try (Connection conn = DBConnect.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT product_id, name, selling_price FROM products WHERE product_id = ?")) {

                // Try to parse barcode as product ID
                try {
                    int productId = Integer.parseInt(barcode);
                    stmt.setInt(1, productId);

                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        int retrievedProductId = rs.getInt("product_id");
                        String productName = rs.getString("name");
                        BigDecimal sellingPrice = rs.getBigDecimal("selling_price");

                        Platform.runLater(() -> {
                            // Use same start and end positions as productListView
                            double startX = 400; // Match productListView start
                            double startY = 30;  // Match productListView start
                            double endX = salesTbl.getBoundsInParent().getMinX() + 100; // Match productListView end
                            double endY = salesTbl.getBoundsInParent().getMinY() + 30;  // Match productListView end

                            String displayText = retrievedProductId + " - " + productName;

                            // Create animation with same style as productListView
                            createFlyingAnimation(displayText, startX, startY, endX, endY,
                                    retrievedProductId, productName, "#81B29A", "white");

                            System.out.println("Barcode scanned: " + barcode + " -> " + productName);
                        });
                    } else {
                        System.out.println("No product found for barcode: " + barcode);
                        Platform.runLater(() -> showBarcodeNotFoundNotification(barcode));
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid barcode format: " + barcode);
                    Platform.runLater(() -> showBarcodeNotFoundNotification(barcode));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showBarcodeNotFoundNotification(String barcode) {
        Platform.runLater(() -> {
            // Create notification label
            Label errorLabel = new Label("Unknown barcode: " + barcode);
            errorLabel.getStyleClass().add("flying-item");
            errorLabel.setStyle("-fx-background-color: #E07A5F; -fx-padding: 5px 10px; " +
                    "-fx-background-radius: 5px; -fx-text-fill: white; " +
                    "-fx-font-weight: bold;");

            // Position at barcode overlay location
            double startX = barcodeOverlay.getX() + barcodeOverlay.getWidth()/2;
            double startY = barcodeOverlay.getY() + barcodeOverlay.getHeight()/2;

            // Add to parent pane
            Pane parent = (Pane) salesTbl.getParent();
            parent.getChildren().add(errorLabel);
            errorLabel.setLayoutX(startX - errorLabel.getPrefWidth()/2);
            errorLabel.setLayoutY(startY);

            // Create fade animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(2000), errorLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.millis(1000));
            fadeOut.setOnFinished(event -> parent.getChildren().remove(errorLabel));
            fadeOut.play();
        });
    }

    private void closeWebcam() {
        if (webcamActive.getAndSet(false) && webcam != null) {
            if (captureThread != null) {
                captureThread.interrupt();
                try {
                    captureThread.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (scannerExecutor != null) {
                scannerExecutor.shutdown();
                try {
                    scannerExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            webcam.close();
        }
    }

    private void toggleMode() {
        isCameraMode = !isCameraMode;

        if (isCameraMode) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), productsListView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            cameraLoadingSpinner.setProgress(0);
            cameraLoadingSpinner.setVisible(true);
            searchFld.setDisable(true);

            fadeOut.setOnFinished(e -> {
                productsListView.setVisible(false);
                cameraFrame.setVisible(true);

                animateProgress();

                Thread initThread = new Thread(() -> {
                    initWebcam();

                    Platform.runLater(() -> {
                        cameraLoadingSpinner.setVisible(false);
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), cameraFrame);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();
                    });
                });

                initThread.setDaemon(true);
                initThread.start();
            });

            fadeOut.play();
        } else {
            closeWebcam();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), cameraFrame);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), productsListView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            fadeOut.setOnFinished(e -> {
                cameraFrame.setVisible(false);
                productsListView.setVisible(true);
                searchFld.setDisable(false);
                fadeIn.play();
            });

            fadeOut.play();
        }
    }
}