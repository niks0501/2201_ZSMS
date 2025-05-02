package other_classes;

import table_models.Discount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DiscountDAO {
    public static List<Discount> getActiveDiscounts() {
        List<Discount> discounts = new ArrayList<>();
        String query = "SELECT d.discount_id, d.product_id, p.name AS product_name, d.category_id, " +
                "c.category_name, d.discount_type, d.discount_value, d.min_quantity, " +
                "d.start_date, d.end_date, d.is_active " +
                "FROM discounts d " +
                "LEFT JOIN products p ON d.product_id = p.product_id " +
                "LEFT JOIN categories c ON d.category_id = c.category_id " +
                "WHERE d.is_active = 1";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                discounts.add(new Discount(
                        rs.getInt("discount_id"),
                        rs.getObject("product_id") != null ? rs.getInt("product_id") : null,
                        rs.getString("product_name"),
                        rs.getObject("category_id") != null ? rs.getInt("category_id") : null,
                        rs.getString("category_name"),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("discount_value"),
                        rs.getInt("min_quantity"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getBoolean("is_active")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return discounts;
    }
}