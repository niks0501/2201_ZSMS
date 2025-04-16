package other_classes;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import table_models.Category;
import table_models.Product;

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

    public static int insertProduct(String name, int categoryId, double costPrice,
                                    double markup, int stock, double sellingPrice,
                                    String imagePath) throws SQLException {

        String sql = "{CALL sp_insert_product(?, ?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = DBConnect.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            // Set IN parameters
            cstmt.setString(1, name);
            cstmt.setInt(2, categoryId);
            cstmt.setDouble(3, costPrice);
            cstmt.setDouble(4, markup);
            cstmt.setInt(5, stock);
            cstmt.setDouble(6, sellingPrice);
            cstmt.setString(7, imagePath != null ? imagePath : "");

            // Register OUT parameter
            cstmt.registerOutParameter(8, java.sql.Types.INTEGER);

            // Execute procedure
            cstmt.execute();

            // Get the product ID from the OUT parameter
            return cstmt.getInt(8);
        }
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

    public static ObservableList<Product> getAllProducts() {
        ObservableList<Product> products = FXCollections.observableArrayList();

        try (Connection conn = DBConnect.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL GetAllProducts()}");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("image_path"),
                        rs.getString("name"),
                        rs.getString("category_name"),
                        rs.getBigDecimal("cost_price"),
                        rs.getBigDecimal("markup_percentage"),
                        rs.getInt("stock"),
                        rs.getBigDecimal("selling_price"),
                        rs.getString("barcode_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return products;
    }

    // Add this method to ProductDAO class
    public static Integer getCategoryIdByName(String categoryName) {
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT category_id FROM categories WHERE category_name = ?")) {
            stmt.setString(1, categoryName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("category_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}