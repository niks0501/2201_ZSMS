package controllers;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import other_classes.DBConnect;
import table_models.Category;
import table_models.Discount;
import table_models.Product;
import table_models.SalesItem;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardCompController implements Initializable {

    @FXML
    private PieChart categoryPerformance;

    @FXML
    private MFXComboBox<String> chartMode;

    @FXML
    private Label customerWithCreditsLbl;

    @FXML
    private StackPane dashCompMainFrame;

    @FXML
    private Label dateLbl;

    @FXML
    private AreaChart<String, Number> discountEffectiveness;

    @FXML
    private LineChart<String, Number> salesOverview;

    @FXML
    private Label timeLbl;

    @FXML
    private BarChart<String, Number> topSellingProducts;

    @FXML
    private Label totalProductsLbl;

    @FXML
    private Label totalSalesLbl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize chartMode ComboBox
        chartMode.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly"));
        chartMode.getSelectionModel().selectFirst();
        chartMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateCharts(newVal));

        // Initialize charts
        setupSalesOverviewChart();
        setupTopSellingProductsChart();
        setupDiscountEffectivenessChart();
        setupCategoryPerformanceChart();

        // Update labels
        updateSummaryLabels();

        // Set up real-time date and time updates
        setupDateTimeUpdates();

        // Initial chart update
        updateCharts(chartMode.getSelectionModel().getSelectedItem());
    }

    private void setupDateTimeUpdates() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            dateLbl.setText(now.format(DATE_FORMATTER));
            timeLbl.setText(now.format(TIME_FORMATTER));
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void setupSalesOverviewChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Total Sales ($)");
        salesOverview.setTitle("Sales Overview");
        salesOverview.setCreateSymbols(true);
        salesOverview.getXAxis().setAutoRanging(true);
        salesOverview.getYAxis().setAutoRanging(true);
    }

    private void setupTopSellingProductsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Product");
        yAxis.setLabel("Quantity Sold");
        topSellingProducts.setTitle("Top Selling Products");
        topSellingProducts.setBarGap(5);
        topSellingProducts.setCategoryGap(20);
    }

    private void setupDiscountEffectivenessChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Discount Savings ($)");
        discountEffectiveness.setTitle("Discount Effectiveness");
        discountEffectiveness.setCreateSymbols(false);
    }

    private void setupCategoryPerformanceChart() {
        categoryPerformance.setTitle("Category Performance");
        categoryPerformance.setLegendVisible(true);
        categoryPerformance.setLabelsVisible(true);
    }

    private void updateCharts(String mode) {
        updateSalesOverviewChart(mode);
        updateTopSellingProductsChart(mode);
        updateDiscountEffectivenessChart(mode);
        updateCategoryPerformanceChart(mode);
    }

    private void updateSalesOverviewChart(String mode) {
        salesOverview.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Total Sales");

        String groupByClause;
        switch (mode.toLowerCase()) {
            case "daily":
                groupByClause = "DATE(sale_date)";
                break;
            case "weekly":
                groupByClause = "YEARWEEK(sale_date)";
                break;
            case "monthly":
                groupByClause = "DATE_FORMAT(sale_date, '%Y-%m')";
                break;
            default:
                groupByClause = "DATE(sale_date)";
        }

        String query = "SELECT " + groupByClause + " AS period, SUM(total_price) AS total " +
                "FROM sales WHERE sale_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) " +
                "GROUP BY period ORDER BY period";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String period = rs.getString("period");
                double total = rs.getDouble("total");
                series.getData().add(new XYChart.Data<>(period, total));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        salesOverview.getData().add(series);
    }

    private void updateTopSellingProductsChart(String mode) {
        topSellingProducts.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Quantity Sold");

        String dateFilter;
        switch (mode.toLowerCase()) {
            case "daily":
                dateFilter = "DATE(s.sale_date) = CURDATE()";
                break;
            case "weekly":
                dateFilter = "YEARWEEK(s.sale_date) = YEARWEEK(CURDATE())";
                break;
            case "monthly":
                dateFilter = "DATE_FORMAT(s.sale_date, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')";
                break;
            default:
                dateFilter = "s.sale_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)";
        }

        String query = "SELECT p.name, SUM(si.quantity) AS total_quantity " +
                "FROM sales_items si " +
                "JOIN products p ON si.product_id = p.product_id " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "WHERE " + dateFilter + " " +
                "GROUP BY p.product_id, p.name " +
                "ORDER BY total_quantity DESC LIMIT 5";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String productName = rs.getString("name");
                int quantity = rs.getInt("total_quantity");
                series.getData().add(new XYChart.Data<>(productName, quantity));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        topSellingProducts.getData().add(series);
    }

    private void updateDiscountEffectivenessChart(String mode) {
        discountEffectiveness.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Discount Savings");

        String groupByClause;
        switch (mode.toLowerCase()) {
            case "daily":
                groupByClause = "DATE(s.sale_date)";
                break;
            case "weekly":
                groupByClause = "YEARWEEK(s.sale_date)";
                break;
            case "monthly":
                groupByClause = "DATE_FORMAT(s.sale_date, '%Y-%m')";
                break;
            default:
                groupByClause = "DATE(s.sale_date)";
        }

        String query = "SELECT " + groupByClause + " AS period, SUM(si.subtotal - si.final_price) AS savings " +
                "FROM sales_items si " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "WHERE si.subtotal > si.final_price " +
                "AND s.sale_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) " +
                "GROUP BY period ORDER BY period";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String period = rs.getString("period");
                double savings = rs.getDouble("savings");
                series.getData().add(new XYChart.Data<>(period, savings));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        discountEffectiveness.getData().add(series);
    }

    private void updateCategoryPerformanceChart(String mode) {
        categoryPerformance.getData().clear();

        String dateFilter;
        switch (mode.toLowerCase()) {
            case "daily":
                dateFilter = "DATE(s.sale_date) = CURDATE()";
                break;
            case "weekly":
                dateFilter = "YEARWEEK(s.sale_date) = YEARWEEK(CURDATE())";
                break;
            case "monthly":
                dateFilter = "DATE_FORMAT(s.sale_date, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')";
                break;
            default:
                dateFilter = "s.sale_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)";
        }

        String query = "SELECT c.category_name, SUM(si.quantity) AS total_quantity " +
                "FROM sales_items si " +
                "JOIN products p ON si.product_id = p.product_id " +
                "JOIN categories c ON p.category_id = c.category_id " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "WHERE " + dateFilter + " " +
                "GROUP BY c.category_id, c.category_name";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String categoryName = rs.getString("category_name");
                int quantity = rs.getInt("total_quantity");
                categoryPerformance.getData().add(new PieChart.Data(categoryName, quantity));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSummaryLabels() {
        // Total Products
        String productQuery = "SELECT COUNT(*) AS total FROM products";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(productQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                totalProductsLbl.setText(String.valueOf(rs.getInt("total")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Total Sales
        String salesQuery = "SELECT SUM(total_price) AS total FROM sales";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(salesQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                totalSalesLbl.setText(String.format("₱%.2f", rs.getDouble("total")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Customers with Credits
        String creditQuery = "SELECT COUNT(*) AS total FROM customers WHERE credit_balance > 0";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(creditQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                customerWithCreditsLbl.setText(String.valueOf(rs.getInt("total")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}