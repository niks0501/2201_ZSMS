package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXDatePicker;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import other_classes.DBConnect;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GenerateReportController {

    @FXML
    private MFXButton btnDaily;

    @FXML
    private MFXButton btnMonthly;

    @FXML
    private MFXButton btnWeekly;

    @FXML
    private AnchorPane genReportMainFrame;

    @FXML
    private Hyperlink customRangeHL;

    private MFXProgressSpinner spinner;

    private static class Sale {
        int saleId;
        String date;
        String productName;
        int quantity;
        double totalPrice;

        Sale(int saleId, String date, String productName, int quantity, double totalPrice) {
            this.saleId = saleId;
            this.date = date;
            this.productName = productName;
            this.quantity = quantity;
            this.totalPrice = totalPrice;
        }
    }

    private static class Summary {
        double totalRevenue;
        double profitMargin;
        String topProducts;

        Summary(double totalRevenue, double profitMargin, String topProducts) {
            this.totalRevenue = totalRevenue;
            this.profitMargin = profitMargin;
            this.topProducts = topProducts;
        }
    }

    private static class ProductSale {
        String productName;
        int quantity;

        ProductSale(String productName, int quantity) {
            this.productName = productName;
            this.quantity = quantity;
        }
    }

    @FXML
    private void initialize() {
        // Initialize spinner
        spinner = new MFXProgressSpinner();
        spinner.setVisible(false);
        AnchorPane.setTopAnchor(spinner, 10.0);
        AnchorPane.setRightAnchor(spinner, 10.0);
        genReportMainFrame.getChildren().add(spinner);

        // Set button actions
        btnDaily.setOnAction(event -> generateReport("daily", null, null));
        btnWeekly.setOnAction(event -> generateReport("weekly", null, null));
        btnMonthly.setOnAction(event -> generateReport("monthly", null, null));

        // Set custom range hyperlink action
        customRangeHL.setOnAction(event -> showCustomDateRangeDialog());
    }

    private void showCustomDateRangeDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);

        // Create layout
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        // Add components
        Label titleLabel = new Label("Select Date Range");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        MFXDatePicker startDatePicker = new MFXDatePicker();
        startDatePicker.setFloatingText("Start Date");
        startDatePicker.setPrefWidth(200);

        MFXDatePicker endDatePicker = new MFXDatePicker();
        endDatePicker.setFloatingText("End Date");
        endDatePicker.setPrefWidth(200);

        MFXButton confirmButton = new MFXButton("Generate Report");
        confirmButton.setStyle("-fx-background-color: #81B29A; -fx-text-fill: white;");
        confirmButton.setOnAction(event -> {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate == null || endDate == null) {
                showAlert("Error", "Please select both start and end dates.");
                return;
            }
            if (startDate.isAfter(endDate)) {
                showAlert("Error", "Start date must be before or equal to end date.");
                return;
            }
            dialogStage.close();
            generateReport("custom", startDate, endDate);
        });

        MFXButton cancelButton = new MFXButton("Cancel");
        cancelButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        cancelButton.setOnAction(event -> dialogStage.close());

        // Add components to layout
        root.getChildren().addAll(titleLabel, startDatePicker, endDatePicker, confirmButton, cancelButton);

        // Create scene and apply CSS
        Scene scene = new Scene(root, 300, 250);
        scene.getStylesheets().add(getClass().getResource("/css/customDateRange.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void generateReport(String type, LocalDate startDate, LocalDate endDate) {
        // Disable buttons and show spinner
        Platform.runLater(() -> {
            btnDaily.setDisable(true);
            btnWeekly.setDisable(true);
            btnMonthly.setDisable(true);
            customRangeHL.setDisable(true);
            spinner.setVisible(true);
        });

        // Run report generation in background
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Sale> sales = fetchSalesData(type, startDate, endDate);
                Summary summary = calculateSummary(sales, type, startDate, endDate);
                generatePDFReport(sales, summary, type, startDate, endDate);
                return null;
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            btnDaily.setDisable(false);
            btnWeekly.setDisable(false);
            btnMonthly.setDisable(false);
            customRangeHL.setDisable(false);
            spinner.setVisible(false);
            System.out.println(type + " report generated successfully!");
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            btnDaily.setDisable(false);
            btnWeekly.setDisable(false);
            btnMonthly.setDisable(false);
            customRangeHL.setDisable(false);
            spinner.setVisible(false);
            System.err.println("Error generating report: " + task.getException().getMessage());
            task.getException().printStackTrace();
        }));

        new Thread(task).start();
    }

    private List<Sale> fetchSalesData(String type, LocalDate startDate, LocalDate endDate) throws Exception {
        List<Sale> sales = new ArrayList<>();
        String query;
        if (type.equals("custom")) {
            query = "SELECT s.sale_id, DATE_FORMAT(s.sale_date, '%Y-%m-%d') AS sale_date, " +
                    "p.name AS product_name, si.quantity, s.total_price " +
                    "FROM sales s " +
                    "JOIN sales_items si ON s.sale_id = si.sale_id " +
                    "JOIN products p ON si.product_id = p.product_id " +
                    "WHERE s.sale_date >= ? AND s.sale_date < DATE_ADD(?, INTERVAL 1 DAY)";
        } else {
            switch (type) {
                case "daily":
                    query = "SELECT s.sale_id, DATE_FORMAT(s.sale_date, '%Y-%m-%d') AS sale_date, " +
                            "p.name AS product_name, si.quantity, s.total_price " +
                            "FROM sales s " +
                            "JOIN sales_items si ON s.sale_id = si.sale_id " +
                            "JOIN products p ON si.product_id = p.product_id " +
                            "WHERE DATE(s.sale_date) = CURDATE()";
                    break;
                case "weekly":
                    query = "SELECT s.sale_id, DATE_FORMAT(s.sale_date, '%Y-%m-%d') AS sale_date, " +
                            "p.name AS product_name, si.quantity, s.total_price " +
                            "FROM sales s " +
                            "JOIN sales_items si ON s.sale_id = si.sale_id " +
                            "JOIN products p ON si.product_id = p.product_id " +
                            "WHERE s.sale_date >= CURDATE() - INTERVAL 7 DAY";
                    break;
                case "monthly":
                    query = "SELECT s.sale_id, DATE_FORMAT(s.sale_date, '%Y-%m-%d') AS sale_date, " +
                            "p.name AS product_name, si.quantity, s.total_price " +
                            "FROM sales s " +
                            "JOIN sales_items si ON s.sale_id = si.sale_id " +
                            "JOIN products p ON si.product_id = p.product_id " +
                            "WHERE YEAR(s.sale_date) = YEAR(CURDATE()) AND MONTH(s.sale_date) = MONTH(CURDATE())";
                    break;
                default:
                    return sales;
            }
        }

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            if (type.equals("custom")) {
                stmt.setDate(1, java.sql.Date.valueOf(startDate));
                stmt.setDate(2, java.sql.Date.valueOf(endDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(new Sale(
                            rs.getInt("sale_id"),
                            rs.getString("sale_date"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("total_price")
                    ));
                }
            }
        }
        return sales;
    }

    private Summary calculateSummary(List<Sale> sales, String type, LocalDate startDate, LocalDate endDate) throws Exception {
        // Calculate total revenue
        double totalRevenue = sales.stream().mapToDouble(s -> s.totalPrice).sum();

        // Calculate profit margin
        double totalProfit = 0;
        double totalWeightedRevenue = 0;
        try (Connection conn = DBConnect.getConnection()) {
            for (Sale sale : sales) {
                String query = "SELECT si.final_price, p.cost_price, si.quantity " +
                        "FROM sales_items si " +
                        "JOIN products p ON si.product_id = p.product_id " +
                        "WHERE si.sale_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, sale.saleId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        double finalPrice = rs.getDouble("final_price");
                        double costPrice = rs.getDouble("cost_price");
                        int quantity = rs.getInt("quantity");
                        double itemProfit = finalPrice - (costPrice * quantity);
                        double itemMargin = finalPrice > 0 ? (itemProfit / finalPrice) * 100 : 0;
                        itemMargin = Math.max(0, Math.min(100, itemMargin)); // Cap between 0 and 100
                        totalProfit += itemMargin * finalPrice;
                        totalWeightedRevenue += finalPrice;
                    }
                }
            }
        }
        double profitMargin = totalWeightedRevenue > 0 ? (totalProfit / totalWeightedRevenue) : 0;

        // Calculate top 5 products
        String topProductsQuery;
        // After
        if (type.equals("custom")) {
            topProductsQuery = "SELECT p.name, SUM(si.quantity) AS total_quantity " +
                    "FROM sales s " +
                    "JOIN sales_items si ON s.sale_id = si.sale_id " +
                    "JOIN products p ON si.product_id = p.product_id " +
                    "WHERE s.sale_date >= ? AND s.sale_date < DATE_ADD(?, INTERVAL 1 DAY) " +
                    "GROUP BY p.name " +
                    "ORDER BY total_quantity DESC LIMIT 5";
        } else {
            switch (type) {
                case "daily":
                    topProductsQuery = "SELECT p.name, SUM(si.quantity) AS total_quantity " +
                            "FROM sales s " +
                            "JOIN sales_items si ON s.sale_id = si.sale_id " +
                            "JOIN products p ON si.product_id = p.product_id " +
                            "WHERE DATE(s.sale_date) = CURDATE() " +
                            "GROUP BY p.name " +
                            "ORDER BY total_quantity DESC LIMIT 5";
                    break;
                case "weekly":
                    topProductsQuery = "SELECT p.name, SUM(si.quantity) AS total_quantity " +
                            "FROM sales s " +
                            "JOIN sales_items si ON s.sale_id = si.sale_id " +
                            "JOIN products p ON si.product_id = p.product_id " +
                            "WHERE s.sale_date >= CURDATE() - INTERVAL 7 DAY " +
                            "GROUP BY p.name " +
                            "ORDER BY total_quantity DESC LIMIT 5";
                    break;
                case "monthly":
                    topProductsQuery = "SELECT p.name, SUM(si.quantity) AS total_quantity " +
                            "FROM sales s " +
                            "JOIN sales_items si ON s.sale_id = si.sale_id " +
                            "JOIN products p ON si.product_id = p.product_id " +
                            "WHERE YEAR(s.sale_date) = YEAR(CURDATE()) AND MONTH(s.sale_date) = MONTH(CURDATE()) " +
                            "GROUP BY p.name " +
                            "ORDER BY total_quantity DESC LIMIT 5";
                    break;
                default:
                    topProductsQuery = "";
            }
        }

        List<ProductSale> topProducts = new ArrayList<>();
        if (!topProductsQuery.isEmpty()) {
            try (Connection conn = DBConnect.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(topProductsQuery)) {
                if (type.equals("custom")) {
                    stmt.setDate(1, java.sql.Date.valueOf(startDate));
                    stmt.setDate(2, java.sql.Date.valueOf(endDate));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        topProducts.add(new ProductSale(
                                rs.getString("name"),
                                rs.getInt("total_quantity")
                        ));
                    }
                }
            }
        }
        String topProductsText = topProducts.isEmpty() ? "None" :
                topProducts.stream()
                        .map(p -> p.productName + " (" + p.quantity + ")")
                        .collect(java.util.stream.Collectors.joining(", "));

        return new Summary(totalRevenue, profitMargin, topProductsText);
    }

    private void generatePDFReport(List<Sale> sales, Summary summary, String type, LocalDate startDate, LocalDate endDate) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Define colors
            PDColor themeColor = new PDColor(new float[]{129f / 255f, 178f / 255f, 154f / 255f}, PDDeviceRGB.INSTANCE);
            PDColor white = new PDColor(new float[]{1f, 1f, 1f}, PDDeviceRGB.INSTANCE);
            PDColor black = new PDColor(new float[]{0f, 0f, 0f}, PDDeviceRGB.INSTANCE);

            // Add title
            String title = type.equals("custom") ? "Custom Sales Report" : type.substring(0, 1).toUpperCase() + type.substring(1) + " Sales Report";
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            contentStream.setNonStrokingColor(black);
            float titleWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD).getStringWidth(title) / 1000 * 18;
            contentStream.beginText();
            contentStream.newLineAtOffset((PDRectangle.A4.getWidth() - titleWidth) / 2, 760);
            contentStream.showText(title);
            contentStream.endText();

            // Add title underline
            contentStream.setStrokingColor(themeColor);
            contentStream.setLineWidth(2f);
            contentStream.moveTo(50, 750);
            contentStream.lineTo(PDRectangle.A4.getWidth() - 50, 750);
            contentStream.stroke();

            // Add sales entries in table-like style
            float yPosition = 720;
            float xStart = 50;
            float rowHeight = 20; // Height per row
            float[] columnWidths = {50, 100, 150, 50, 80}; // Sale ID, Date, Product, Qty, Total
            String[] headers = {"Sale ID", "Date", "Product", "Qty", "Total"};

            // Draw header
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            contentStream.setNonStrokingColor(black);
            float xPosition = xStart;
            for (int i = 0; i < headers.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, yPosition);
                contentStream.showText(headers[i]);
                contentStream.endText();
                xPosition += columnWidths[i];
            }
            yPosition -= rowHeight;

            // Draw header divider
            contentStream.setStrokingColor(themeColor);
            contentStream.setLineWidth(1f);
            contentStream.moveTo(xStart, yPosition + 5);
            contentStream.lineTo(xStart + 430, yPosition + 5);
            contentStream.stroke();
            yPosition -= 10;

            // Draw sales data
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            for (Sale sale : sales) {
                // Check if new page is needed
                if (yPosition < 50 + rowHeight) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);

                    // Add simplified header on new pages
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                    contentStream.setNonStrokingColor(black);
                    contentStream.beginText();
                    contentStream.newLineAtOffset((PDRectangle.A4.getWidth() - titleWidth) / 2, 780);
                    contentStream.showText(title);
                    contentStream.endText();
                    contentStream.setStrokingColor(themeColor);
                    contentStream.setLineWidth(1f);
                    contentStream.moveTo(50, 770);
                    contentStream.lineTo(PDRectangle.A4.getWidth() - 50, 770);
                    contentStream.stroke();

                    // Repeat table header
                    yPosition = 740;
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                    contentStream.setNonStrokingColor(black);
                    xPosition = xStart;
                    for (int i = 0; i < headers.length; i++) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(xPosition, yPosition);
                        contentStream.showText(headers[i]);
                        contentStream.endText();
                        xPosition += columnWidths[i];
                    }
                    yPosition -= rowHeight;
                    contentStream.setStrokingColor(themeColor);
                    contentStream.setLineWidth(1f);
                    contentStream.moveTo(xStart, yPosition + 5);
                    contentStream.lineTo(xStart + 430, yPosition + 5);
                    contentStream.stroke();
                    yPosition -= 10;
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                }

                // Draw sale row
                String productName = sale.productName.length() > 20 ? sale.productName.substring(0, 17) + "..." : sale.productName;
                String[] rowData = {
                        String.valueOf(sale.saleId),
                        sale.date,
                        productName,
                        String.valueOf(sale.quantity),
                        String.format("%.2f", sale.totalPrice)
                };
                xPosition = xStart;
                for (int i = 0; i < rowData.length; i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    contentStream.showText(rowData[i]);
                    contentStream.endText();
                    xPosition += columnWidths[i];
                }
                yPosition -= rowHeight;
            }

            // Add summary section
            if (yPosition < 150) {
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                yPosition = 750;
            }

            // Summary header
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            contentStream.setNonStrokingColor(black);
            contentStream.beginText();
            contentStream.newLineAtOffset(xStart, yPosition);
            contentStream.showText("Summary");
            contentStream.endText();
            contentStream.setStrokingColor(themeColor);
            contentStream.setLineWidth(1f);
            contentStream.moveTo(xStart, yPosition - 10);
            contentStream.lineTo(xStart + 200, yPosition - 10);
            contentStream.stroke();
            yPosition -= 30;

            // Summary metrics
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.setNonStrokingColor(black);
            String[] summaryLabels = {"Total Revenue:", "Profit Margin:", "Top 5 Products:"};
            String[] summaryValues = {
                    String.format("%.2f", summary.totalRevenue),
                    String.format("%.2f%%", summary.profitMargin),
                    summary.topProducts
            };
            for (int i = 0; i < summaryLabels.length; i++) {
                // Label
                contentStream.beginText();
                contentStream.newLineAtOffset(xStart, yPosition);
                contentStream.showText(summaryLabels[i]);
                contentStream.endText();
                // Value (with wrapping for Top 5 Products)
                if (i == 2 && summaryValues[i].length() > 40) {
                    String[] words = summaryValues[i].split(", ");
                    StringBuilder line = new StringBuilder();
                    float yValue = yPosition;
                    for (String word : words) {
                        if (line.length() + word.length() > 40) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(xStart + 120, yValue);
                            contentStream.showText(line.toString().trim());
                            contentStream.endText();
                            yValue -= 15;
                            line = new StringBuilder();
                        }
                        line.append(word).append(", ");
                    }
                    if (line.length() > 0) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(xStart + 120, yValue);
                        contentStream.showText(line.toString().trim().replaceAll(",$", ""));
                        contentStream.endText();
                    }
                    yPosition = yValue - 20;
                } else {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xStart + 120, yPosition);
                    contentStream.showText(summaryValues[i]);
                    contentStream.endText();
                    yPosition -= 20;
                }
                // Divider
                contentStream.setStrokingColor(themeColor);
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(xStart, yPosition + 10);
                contentStream.lineTo(xStart + 430, yPosition + 10);
                contentStream.stroke();
                yPosition -= 10;
            }

            contentStream.close();
            // Save PDF with timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String fileName;
            if (type.equals("custom")) {
                fileName = String.format("custom_sales_%s_%s.pdf",
                        sdf.format(java.sql.Date.valueOf(startDate)),
                        sdf.format(java.sql.Date.valueOf(endDate)));
            } else {
                String timestamp = sdf.format(new Date());
                fileName = type + "_sales_" + timestamp + ".pdf";
            }
            String reportsDir = "C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\reports";
            File reportDirectory = new File(reportsDir);
            if (!reportDirectory.exists()) {
                reportDirectory.mkdirs();
            }
            document.save(new File(reportDirectory, fileName));
        }
    }
}