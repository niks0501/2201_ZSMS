package other_classes;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import table_models.Category;
import table_models.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public static boolean updateProductStock(int productId, int newStockAmount) {
        String sql = "UPDATE products SET stock = ? WHERE product_id = ?";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newStockAmount);
            stmt.setInt(2, productId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateProduct(int productId, String name, Integer categoryId,
                                        BigDecimal costPrice, BigDecimal markupPercentage,
                                        BigDecimal sellingPrice, String imagePath) {
        StringBuilder sql = new StringBuilder("UPDATE products SET ");
        List<String> updateFields = new ArrayList<>();

        if (name != null) updateFields.add("name = ?");
        if (categoryId != null) updateFields.add("category_id = ?");
        if (costPrice != null) updateFields.add("cost_price = ?");
        if (markupPercentage != null) updateFields.add("markup_percentage = ?");
        if (sellingPrice != null) updateFields.add("selling_price = ?");
        if (imagePath != null) updateFields.add("image_path = ?");

        // Check if there are any fields to update
        if (updateFields.isEmpty()) {
            return true; // Nothing to update, consider it successful
        }

        sql.append(String.join(", ", updateFields));
        sql.append(" WHERE product_id = ?");

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (name != null) stmt.setString(paramIndex++, name);
            if (categoryId != null) stmt.setInt(paramIndex++, categoryId);
            if (costPrice != null) stmt.setBigDecimal(paramIndex++, costPrice);
            if (markupPercentage != null) stmt.setBigDecimal(paramIndex++, markupPercentage);
            if (sellingPrice != null) stmt.setBigDecimal(paramIndex++, sellingPrice);
            if (imagePath != null) stmt.setString(paramIndex++, imagePath);

            stmt.setInt(paramIndex, productId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);

            // Execute the delete statement
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}