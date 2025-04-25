package controllers;

import io.github.palexdev.materialfx.controls.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import other_classes.DBConnect;
import table_models.CreditTransaction;
import table_models.CustomerInfo;
import utils.EmailService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CreditsController implements Initializable {

    @FXML
    private TableColumn<CreditTransaction, String> amountColumn;

    @FXML
    private Pane creditContentPane;

    @FXML
    private MFXButton btnSendNotif;

    @FXML
    private StackPane creditMainFrame;

    @FXML
    private MFXTextField creditSearchFld;

    @FXML
    private TableColumn<CreditTransaction, String> creditStatisColumn;

    @FXML
    private TableView<CreditTransaction> creditTbl;

    @FXML
    private Pagination creditTblPage;

    @FXML
    private TableColumn<CreditTransaction, String> customerNameColumn;

    @FXML
    private TableColumn<CreditTransaction, Date> dueDateColumn;

    @FXML
    private MFXRadioButton rbNearDue;

    @FXML
    private MFXRadioButton rbUnpaid;

    @FXML
    private MFXRadioButton rbPaid;

    @FXML
    private MFXComboBox<String> sortCreditTbl;

    @FXML
    private TableColumn<CreditTransaction, Timestamp> transacDateColumn;

    @FXML
    private TableColumn<CreditTransaction, Integer> transacIdColumn;

    private ObservableList<CreditTransaction> creditTransactions;
    private FilteredList<CreditTransaction> filteredTransactions;
    private final int ROWS_PER_PAGE = 10;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private String currentSortOption = "Oldest to Newest";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        loadCreditTransactions();
        setupRadioButtons();
        setupSearchField();
        setupSortComboBox();

        // Configure Send Notification button
        btnSendNotif.setVisible(false); // Initially hidden
        btnSendNotif.setOnAction(event -> handleSendNotifications());

    }

    private void initializeTable() {
        // Set up table columns with proper cell factories
        transacIdColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));

        // Customer name column with double-click functionality
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerNameColumn.setCellFactory(column -> {
            TableCell<CreditTransaction, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                }
            };

            // Add double-click handler
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    CreditTransaction transaction = cell.getTableView().getItems().get(cell.getIndex());
                    openCustomerView(transaction);
                }
            });

            return cell;
        });

        amountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedAmount()));

        transacDateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        transacDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toLocalDateTime().format(dateFormatter));
                }
            }
        });

        // Add these two lines to set up the cell value factories
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        creditStatisColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Due date column with editable cells
        dueDateColumn.setCellFactory(column -> {
            TableCell<CreditTransaction, Date> cell = new TableCell<>() {
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.toLocalDate().format(dateFormatter));
                    }
                }
            };

            // Enable editing on double click
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    DatePicker datePicker = new DatePicker();
                    datePicker.setValue(cell.getItem().toLocalDate());
                    datePicker.setPrefWidth(cell.getWidth() - 20);
                    datePicker.setStyle("-fx-background-color: #81B29A; -fx-text-fill: white;");

                    // Handle key press events - specifically Enter key
                    datePicker.setOnKeyPressed(keyEvent -> {
                        if (keyEvent.getCode() == KeyCode.ENTER) {
                            if (datePicker.getValue() != null) {
                                Date newDate = Date.valueOf(datePicker.getValue());

                                // Update cell and model
                                cell.commitEdit(newDate);

                                // Update database
                                updateDueDateInDatabase(cell, newDate);

                                // Reset cell appearance
                                cell.setGraphic(null);
                                cell.setText(newDate.toLocalDate().format(dateFormatter));

                                // Prevent event bubbling
                                keyEvent.consume();

                                // Return focus to table
                                Platform.runLater(() -> cell.getTableView().requestFocus());
                            }
                        }
                    });

                    // Also handle date selection via click
                    datePicker.setOnAction(e -> {
                        if (datePicker.getValue() != null) {
                            Date newDate = Date.valueOf(datePicker.getValue());
                            cell.commitEdit(newDate);
                            updateDueDateInDatabase(cell, newDate);

                            // Reset cell display
                            cell.setGraphic(null);
                            cell.setText(newDate.toLocalDate().format(dateFormatter));
                            Platform.runLater(() -> cell.getTableView().requestFocus());
                        }
                    });

                    cell.setGraphic(datePicker);
                    cell.setText(null);
                    datePicker.show();
                }
            });

            return cell;
        });

// Status column with editable cells
        creditStatisColumn.setCellFactory(column -> {
            TableCell<CreditTransaction, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                    }
                }
            };

            // Enable editing on double click
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    ComboBox<String> comboBox = new ComboBox<>();
                    comboBox.getItems().addAll("PAID", "UNPAID");
                    comboBox.setValue(cell.getItem());
                    comboBox.setStyle("-fx-background-color: #81B29A; -fx-text-fill: white;");
                    comboBox.setPrefWidth(cell.getWidth() - 20);

                    // Handle key press events - specifically Enter key
                    comboBox.setOnKeyPressed(keyEvent -> {
                        if (keyEvent.getCode() == KeyCode.ENTER) {
                            String newStatus = comboBox.getValue();

                            // Update cell and model
                            cell.commitEdit(newStatus);

                            // Update database
                            updateStatusInDatabase(cell, newStatus);

                            // Reset cell appearance
                            cell.setGraphic(null);
                            cell.setText(newStatus);

                            // Prevent event bubbling
                            keyEvent.consume();

                            // Return focus to table
                            Platform.runLater(() -> cell.getTableView().requestFocus());
                        }
                    });

                    // Handle selection via click
                    comboBox.setOnAction(e -> {
                        String newStatus = comboBox.getValue();
                        String originalStatus = cell.getItem();

                        // If it's already PAID, prevent changes
                        if ("PAID".equals(originalStatus)) {
                            // Show error message
                            Alert errorDialog = new Alert(Alert.AlertType.ERROR);
                            errorDialog.setTitle("Cannot Modify Status");
                            errorDialog.setHeaderText("Paid transactions cannot be modified");
                            errorDialog.setContentText("This transaction has already been marked as PAID and cannot be changed.");
                            errorDialog.showAndWait();

                            // Reset to original value
                            comboBox.setValue(originalStatus);

                            // Reset cell display without updating
                            cell.setGraphic(null);
                            cell.setText(originalStatus);
                            Platform.runLater(() -> cell.getTableView().requestFocus());
                            return;
                        }

                        // Normal flow for non-PAID transactions
                        // Create confirmation dialog
                        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmDialog.setTitle("Confirm Status Change");
                        confirmDialog.setHeaderText("Update transaction status");
                        confirmDialog.setContentText("Are you sure you want to change status from " +
                                originalStatus + " to " + newStatus + "?");

                        // Show dialog and wait for response
                        confirmDialog.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                // User confirmed - update status
                                cell.commitEdit(newStatus);
                                updateStatusInDatabase(cell, newStatus);

                                // If changing to PAID, update sales total_price
                                if ("PAID".equals(newStatus) && !"PAID".equals(originalStatus)) {
                                    updateSalesTotalPrice(cell);
                                }

                                // Reset cell display
                                cell.setGraphic(null);
                                cell.setText(newStatus);
                            } else {
                                // User canceled - revert ComboBox value
                                comboBox.setValue(originalStatus);

                                // Reset cell display without updating
                                cell.setGraphic(null);
                                cell.setText(originalStatus);
                            }
                            Platform.runLater(() -> cell.getTableView().requestFocus());
                        });
                    });

                    cell.setGraphic(comboBox);
                    cell.setText(null);
                    comboBox.show();
                }
            });

            return cell;
        });
    }

    private void updateSalesTotalPrice(TableCell<CreditTransaction, String> cell) {
        CreditTransaction transaction = cell.getTableView().getItems().get(cell.getIndex());

        try (Connection conn = DBConnect.getConnection()) {
            // First, get the sale_id and amount from the credit transaction
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT sale_id, amount FROM credit_transactions WHERE transaction_id = ?")) {
                ps.setInt(1, transaction.getTransactionId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int saleId = rs.getInt("sale_id");
                        BigDecimal amount = rs.getBigDecimal("amount");

                        // If there's a valid sale_id, update the sales total_price
                        if (saleId > 0) {
                            try (PreparedStatement updatePs = conn.prepareStatement(
                                    "UPDATE sales SET total_price = total_price + ? WHERE sale_id = ?")) {
                                updatePs.setBigDecimal(1, amount);
                                updatePs.setInt(2, saleId);
                                updatePs.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to update sales total");
            alert.setContentText("An error occurred while updating the sales total.");
            alert.showAndWait();
        }
    }

    private void openCustomerView(CreditTransaction transaction) {
        try {
            // Show loading indicator first
            Stage loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.TRANSPARENT);
            loadingStage.initModality(Modality.APPLICATION_MODAL); // Fixed import issue
            loadingStage.initOwner(creditTbl.getScene().getWindow());

            ProgressIndicator progress = new ProgressIndicator();
            progress.setStyle("-fx-progress-color: #81B29A;");

            Label loadingLabel = new Label("Loading Customer Information...");
            loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

            VBox loadingBox = new VBox(10, progress, loadingLabel);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(20));
            loadingBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

            Scene loadingScene = new Scene(loadingBox);
            loadingScene.setFill(Color.TRANSPARENT);
            loadingStage.setScene(loadingScene);
            loadingStage.show();

            // Load the customer view in background thread
            new Thread(() -> {
                try {
                    // Get customer ID from transaction
                    int customerId = getCustomerIdFromTransaction(transaction);

                    // Load FXML
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/modals/customer-view.fxml"));
                    Parent root = loader.load();

                    // Get controller and load customer data
                    CustomerViewController controller = loader.getController();
                    controller.loadCustomerData(customerId);

                    // Update UI on JavaFX thread when ready
                    Platform.runLater(() -> {
                        try {
                            // Close the loading stage
                            loadingStage.close();

                            // Create stage with transparent style
                            Stage stage = new Stage();
                            stage.initStyle(StageStyle.TRANSPARENT);

                            // Create scene with transparent background
                            Scene scene = new Scene(root);
                            scene.setFill(Color.TRANSPARENT);
                            scene.getStylesheets().add(getClass().getResource("/css/customer-view.css").toExternalForm());

                            // Configure stage with scene
                            stage.setScene(scene);

                            // Apply rounded corners using rectangle clip
                            Rectangle clip = new Rectangle(root.getLayoutBounds().getWidth(), root.getLayoutBounds().getHeight()); // Fixed method usage
                            clip.setArcWidth(20);
                            clip.setArcHeight(20);
                            root.setClip(clip);

                            // Update clip size when window is resized
                            root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
                                clip.setWidth(newValue.getWidth());
                                clip.setHeight(newValue.getHeight());
                            });

                            // Set initial opacity for fade-in
                            root.setOpacity(0);

                            // Show the stage
                            stage.show();

                            // Apply fade-in animation
                            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
                            fadeIn.setFromValue(0);
                            fadeIn.setToValue(1);
                            fadeIn.play();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        loadingStage.close();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Could not load customer view");
                        alert.setContentText("An error occurred while opening the customer view.");
                        alert.showAndWait();
                    });
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not load customer view");
            alert.setContentText("An error occurred while opening the customer view.");
            alert.showAndWait();
        }
    }

    private int getCustomerIdFromTransaction(CreditTransaction transaction) {
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT customer_id FROM credit_transactions WHERE transaction_id = ?")) {
            ps.setInt(1, transaction.getTransactionId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("customer_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return invalid ID if not found
    }

    private void updateDueDateInDatabase(TableCell<CreditTransaction, Date> cell, Date newDate) {
        CreditTransaction transaction = cell.getTableView().getItems().get(cell.getIndex());
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE credit_transactions SET due_date = ? WHERE transaction_id = ?")) {
            ps.setDate(1, newDate);
            ps.setInt(2, transaction.getTransactionId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to update due date");
            alert.setContentText("An error occurred while updating the database.");
            alert.showAndWait();
        }
    }

    private void updateStatusInDatabase(TableCell<CreditTransaction, String> cell, String newStatus) {
        CreditTransaction transaction = cell.getTableView().getItems().get(cell.getIndex());
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE credit_transactions SET status = ? WHERE transaction_id = ?")) {
            ps.setString(1, newStatus);
            ps.setInt(2, transaction.getTransactionId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to update status");
            alert.setContentText("An error occurred while updating the database.");
            alert.showAndWait();
        }
    }

    private void setupRadioButtons() {
        ToggleGroup filterGroup = new ToggleGroup();
        rbNearDue.setToggleGroup(filterGroup);
        rbUnpaid.setToggleGroup(filterGroup);
        rbPaid.setToggleGroup(filterGroup);

        // Initially hide the notification button
        btnSendNotif.setVisible(false);

        // Track which radio button was last selected
        final MFXRadioButton[] lastSelected = { null };

        // Set initial state to show PAID transactions
        Platform.runLater(() -> {
            rbPaid.setSelected(true);
            lastSelected[0] = rbPaid;
            refreshCreditData();
            applyPaidFilter();
        });

        // Handle rbNearDue clicks
        rbNearDue.setOnAction(event -> {
            if (lastSelected[0] == rbNearDue) {
                // Same radio button clicked twice - deselect it
                Platform.runLater(() -> {
                    rbNearDue.setSelected(false);
                    filterGroup.selectToggle(null);
                    lastSelected[0] = null;
                    refreshCreditData();
                    resetFilters();
                    btnSendNotif.setVisible(false); // Hide button when deselected
                });
            } else {
                lastSelected[0] = rbNearDue;
                refreshCreditData();
                applyNearDueFilter();
                btnSendNotif.setVisible(true); // Show button only for near due
            }
        });

        // Handle rbUnpaid clicks
        rbUnpaid.setOnAction(event -> {
            if (lastSelected[0] == rbUnpaid) {
                // Same radio button clicked twice - deselect it
                Platform.runLater(() -> {
                    rbUnpaid.setSelected(false);
                    filterGroup.selectToggle(null);
                    lastSelected[0] = null;
                    refreshCreditData();
                    resetFilters();
                    btnSendNotif.setVisible(false);
                });
            } else {
                lastSelected[0] = rbUnpaid;
                refreshCreditData();
                applyUnpaidFilter();
                btnSendNotif.setVisible(false);
            }
        });

        // Handle rbPaid clicks
        rbPaid.setOnAction(event -> {
            if (lastSelected[0] == rbPaid) {
                // Same radio button clicked twice - deselect it
                Platform.runLater(() -> {
                    rbPaid.setSelected(false);
                    filterGroup.selectToggle(null);
                    lastSelected[0] = null;
                    refreshCreditData();
                    resetFilters();
                    btnSendNotif.setVisible(false);
                });
            } else {
                lastSelected[0] = rbPaid;
                refreshCreditData();
                applyPaidFilter();
                btnSendNotif.setVisible(false);
            }
        });
    }

    @FXML
    private void handleSendNotifications() {
        // Create a temporary list with all filtered transactions to ensure we process everything
        List<CreditTransaction> nearDueList = new ArrayList<>();

        // Explicitly iterate through the filtered transactions to capture all items
        for (CreditTransaction transaction : filteredTransactions) {
            nearDueList.add(transaction);
        }

        if (nearDueList.isEmpty()) {
            showNotification("No Customers Found", "There are no customers with payments due tomorrow.");
            return;
        }

        // Create a progress dialog
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initOwner(creditTbl.getScene().getWindow());
        progressStage.initStyle(StageStyle.UNDECORATED);

        // Progress UI setup
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-progress-color: #81B29A;");
        Label progressLabel = new Label("Sending notifications...");
        progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        VBox progressBox = new VBox(10, progressIndicator, progressLabel);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(20));
        progressBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; " +
                "-fx-border-color: #dddddd; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.2, 0, 0);");

        Scene progressScene = new Scene(progressBox, 300, 150);
        progressScene.setFill(Color.TRANSPARENT);
        progressStage.setScene(progressScene);
        progressStage.show();

        // Process emails in background thread
        new Thread(() -> {
            // Group transactions by customer ID
            Map<Integer, List<CreditTransaction>> customerTransactions = new HashMap<>();

            // Group all transactions by customer ID
            for (CreditTransaction transaction : nearDueList) {
                int customerId = getCustomerIdFromTransaction(transaction);
                if (customerId != -1) {
                    if (!customerTransactions.containsKey(customerId)) {
                        customerTransactions.put(customerId, new ArrayList<>());
                    }
                    customerTransactions.get(customerId).add(transaction);
                }
            }

            // Debug output - remove in production
            System.out.println("Found " + customerTransactions.size() + " unique customers");

            int successCount = 0;
            int failCount = 0;
            StringBuilder failedEmails = new StringBuilder();
            int current = 0;
            int total = customerTransactions.size();

            // Process each customer
            for (Map.Entry<Integer, List<CreditTransaction>> entry : customerTransactions.entrySet()) {
                int customerId = entry.getKey();
                List<CreditTransaction> transactions = entry.getValue();

                if (transactions.isEmpty()) continue;

                // Get customer information directly from database
                CustomerInfo customerInfo = getCustomerInfoById(customerId);
                if (customerInfo == null) {
                    failCount++;
                    failedEmails.append("• Customer ID ").append(customerId).append(" (customer not found)\n");
                    continue;
                }

                current++;
                int finalCurrent = current;
                Platform.runLater(() -> progressLabel.setText(
                        String.format("Sending notifications... (%d/%d)", finalCurrent, total)));

                if (customerInfo.email == null || customerInfo.email.isEmpty()) {
                    failCount++;
                    failedEmails.append("• ").append(customerInfo.name).append(" (no email)\n");
                    continue;
                }

                // Create email content
                StringBuilder transactionsHtml = new StringBuilder();
                BigDecimal totalAmount = BigDecimal.ZERO;

                for (CreditTransaction transaction : transactions) {
                    String dueDate = transaction.getDueDate().toLocalDate().format(dateFormatter);
                    transactionsHtml.append(String.format(
                            "<tr>" +
                                    "<td style='padding: 8px; border-bottom: 1px solid #ddd;'>$%.2f</td>" +
                                    "<td style='padding: 8px; border-bottom: 1px solid #ddd;'>%s</td>" +
                                    "</tr>",
                            transaction.getAmount().doubleValue(),
                            dueDate
                    ));
                    totalAmount = totalAmount.add(transaction.getAmount());
                }



                String subject = "Payment Reminder: Credit Due Tomorrow";
                String content = String.format(
                        "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                                "<h2 style='color: #81B29A;'>Payment Due Reminder</h2>" +
                                "<p>Dear %s,</p>" +
                                "<p>This is a friendly reminder that you have <strong>payment(s) due tomorrow</strong> " +
                                "totaling <strong>$%.2f</strong>.</p>" +
                                "<p>Here are the details of your pending payment(s):</p>" +
                                "<table style='width: 100%%; border-collapse: collapse;'>" +
                                "<tr style='background-color: #f2f2f2;'>" +
                                "<th style='text-align: left; padding: 8px;'>Amount</th>" +
                                "<th style='text-align: left; padding: 8px;'>Due Date</th>" +
                                "</tr>" +
                                "%s" +
                                "</table>" +
                                "<p>Please make arrangements to settle your account.</p>" +
                                "<p>If you have already made this payment, please disregard this notice.</p>" +
                                "<p>Thank you for your business!</p>" +
                                "<p>Best regards,<br>The Store Team</p>" +
                                "</body></html>",
                        customerInfo.name,
                        totalAmount.doubleValue(),
                        transactionsHtml.toString()
                );

                // Send email
                boolean success = EmailService.sendEmail(customerInfo.email, subject, content);

                if (success) {
                    successCount++;
                } else {
                    failCount++;
                    failedEmails.append("• ").append(customerInfo.name)
                            .append(" (").append(customerInfo.email).append(")\n");
                }

                // Small delay to prevent email server throttling
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            final int finalSuccessCount = successCount;
            final int finalFailCount = failCount;
            final String finalFailedList = failedEmails.toString();

            // Show results when complete
            Platform.runLater(() -> {
                progressStage.close();

                if (finalFailCount == 0) {
                    showNotification("Email Notifications Sent",
                            String.format("Successfully sent %d email notifications.", finalSuccessCount));
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Email Notification Results");
                    alert.setHeaderText("Email Sending Complete");

                    String contentText = String.format("Successfully sent: %d\nFailed: %d",
                            finalSuccessCount, finalFailCount);
                    if (finalFailedList.length() > 0) {
                        contentText += "\n\nFailed recipients:\n" + finalFailedList;
                    }

                    alert.setContentText(contentText);
                    alert.showAndWait();
                }
            });
        }).start();
    }

    // Get customer information directly from the database
    private CustomerInfo getCustomerInfoById(int customerId) {
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, email, credit_balance FROM customers WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CustomerInfo(
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getBigDecimal("credit_balance")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getCustomerEmail(CreditTransaction transaction) {
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c.email FROM customers c " +
                             "JOIN credit_transactions ct ON c.customer_id = ct.customer_id " +
                             "WHERE ct.transaction_id = ?")) {
            ps.setInt(1, transaction.getTransactionId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void applyPaidFilter() {
        filteredTransactions.setPredicate(transaction ->
                "PAID".equalsIgnoreCase(transaction.getStatus())
        );
        setupPagination();
        applySorting();
    }

    private void setupSearchField() {
        creditSearchFld.textProperty().addListener((observable, oldValue, newValue) -> {
            applySearchFilter(newValue);
        });
    }

    private void setupSortComboBox() {
        ObservableList<String> sortOptions = FXCollections.observableArrayList(
                "Newest to Oldest", "Oldest to Newest"
        );

        sortCreditTbl.setItems(sortOptions);

        // Set default selection to "Oldest to Newest"
        Platform.runLater(() -> {
            sortCreditTbl.selectItem("Oldest to Newest");
            currentSortOption = "Oldest to Newest";
        });

        sortCreditTbl.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentSortOption = newValue;
                applySorting();
            }
        });
    }

    private void loadCreditTransactions() {
        creditTransactions = FXCollections.observableArrayList();
        String query = "SELECT * FROM credit_transactions_view";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CreditTransaction transaction = new CreditTransaction(
                        rs.getInt("transaction_id"),
                        rs.getString("customer_name"),
                        rs.getBigDecimal("amount"),
                        rs.getTimestamp("transaction_date"),
                        rs.getDate("due_date"),
                        rs.getString("status")
                );
                creditTransactions.add(transaction);
            }

            filteredTransactions = new FilteredList<>(creditTransactions);
            setupPagination();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupPagination() {
        int totalPages = (filteredTransactions.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        creditTblPage.setPageCount(Math.max(1, totalPages));
        creditTblPage.setCurrentPageIndex(0);

        creditTblPage.setPageFactory(pageIndex -> {
            updateTableContent(pageIndex);
            return new Pane(); // Empty pane as pagination content
        });

        updateTableContent(0);
    }

    private void updateTableContent(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredTransactions.size());

        ObservableList<CreditTransaction> pageData = FXCollections.observableArrayList();
        if (fromIndex < toIndex) {
            pageData.addAll(filteredTransactions.subList(fromIndex, toIndex));
        }

        creditTbl.setItems(pageData);
    }

    private void applySearchFilter(String searchText) {
        filteredTransactions.setPredicate(transaction -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }

            String lowerCaseSearch = searchText.toLowerCase();
            return transaction.getCustomerName().toLowerCase().contains(lowerCaseSearch) ||
                    String.valueOf(transaction.getTransactionId()).contains(lowerCaseSearch) ||
                    transaction.getAmount().toString().contains(lowerCaseSearch) ||
                    transaction.getStatus().toLowerCase().contains(lowerCaseSearch);
        });

        setupPagination();
    }

    private void applyNearDueFilter() {
        filteredTransactions.setPredicate(transaction ->
                transaction.getDaysRemaining() == 1 && "UNPAID".equalsIgnoreCase(transaction.getStatus())
        );
        setupPagination();
        applySorting();
    }

    private void applyUnpaidFilter() {
        filteredTransactions.setPredicate(transaction ->
                "UNPAID".equalsIgnoreCase(transaction.getStatus())
        );
        setupPagination();
        applySorting();
    }

    public void refreshCreditData() {
        // Clear existing data
        creditTransactions.clear();

        // Reload from database
        loadCreditTransactions();
    }

    private void resetFilters() {
        filteredTransactions.setPredicate(transaction -> true);
        setupPagination();
        applySorting();
    }

    private void applySorting() {
        // Create a sortable copy of the filtered data
        ObservableList<CreditTransaction> sortedList = FXCollections.observableArrayList(filteredTransactions);

        // Sort the copy
        sortedList.sort((t1, t2) -> {
            if ("Newest to Oldest".equals(currentSortOption)) {
                // Descending order (higher transaction IDs first)
                return Integer.compare(t2.getTransactionId(), t1.getTransactionId());
            } else {
                // "Oldest to Newest" - Ascending order (lower transaction IDs first)
                return Integer.compare(t1.getTransactionId(), t2.getTransactionId());
            }
        });

        // Update table with the sorted data
        int pageIndex = creditTblPage.getCurrentPageIndex();
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, sortedList.size());

        ObservableList<CreditTransaction> pageData = FXCollections.observableArrayList();
        if (fromIndex < toIndex) {
            pageData.addAll(sortedList.subList(fromIndex, toIndex));
        }

        creditTbl.setItems(pageData);
    }
}