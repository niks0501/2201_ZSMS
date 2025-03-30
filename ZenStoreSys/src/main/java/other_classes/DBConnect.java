package other_classes;

import javax.swing.*;
import java.sql.*;

import static java.sql.DriverManager.getConnection;

public class DBConnect {

    private static final String URL = "jdbc:mysql://localhost:3306/z_store_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        if (connection != null) {
            System.out.println("Connection successful!");
        } else {
            System.out.println("Failed to make connection!");
        }
        return connection;
    }

    public static boolean validateCredentials(String username, String password) {
        String query = "SELECT * FROM admin WHERE username = ? AND password = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
