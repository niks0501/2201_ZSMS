package other_classes;

import table_models.SalesItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SalesDAO {

    public static List<SalesItem> getSalesItemsByPeriod(String period) {
        List<SalesItem> items = new ArrayList<>();
        String query = "SELECT si.product_id, p.name, si.final_price / si.quantity AS unit_price " +
                "FROM sales_items si " +
                "JOIN products p ON si.product_id = p.product_id " +
                "JOIN sales s ON si.sale_id = s.sale_id " +
                "WHERE s.sale_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(new SalesItem(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getBigDecimal("unit_price")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}