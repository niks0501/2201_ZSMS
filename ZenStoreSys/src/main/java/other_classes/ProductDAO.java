package other_classes;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import table_models.Category;

import java.sql.*;

public class ProductDAO {

    // Get all categories for the ComboBox
    public static ObservableList<Category> getAllCategories() {
        ObservableList<Category> categories = FXCollections.observableArrayList();
        String query = "SELECT category_id, category_name FROM categories";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("category_id"),
                        rs.getString("category_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categories;
    }

    // Insert a new product
    public static int insertProduct(String name, int categoryId, double costPrice,
                                    double markup, int stock, double sellingPrice,
                                    String imagePath) throws SQLException {

        String sql = "INSERT INTO products (name, category_id, cost_price, " +
                "markup_percentage, stock, selling_price, image_path, last_restock) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setInt(2, categoryId);
            stmt.setDouble(3, costPrice);
            stmt.setDouble(4, markup);
            stmt.setInt(5, stock);
            stmt.setDouble(6, sellingPrice);
            stmt.setString(7, imagePath != null ? imagePath : "");

            stmt.executeUpdate();

            // Get generated product_id
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return -1; // Failed to get product ID
    }

    

    // Update barcode path after generation
    public static boolean updateBarcodePath(int productId, String barcodePath) {
        String sql = "UPDATE products SET barcode_path = ? WHERE product_id = ?";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, barcodePath);
            stmt.setInt(2, productId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}