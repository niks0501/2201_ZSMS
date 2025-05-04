package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import other_classes.DBConnect;

import java.sql.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class CustomerViewController {

    @FXML
    private Pane barPane;

    @FXML
    private MFXButton btnExit;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private Label createdFld;

    @FXML
    private Label creditBalanceFld;

    @FXML
    private Pane customerInfoPane;

    @FXML
    private Label customerNameFld;

    @FXML
    private Label emailFld;

    @FXML
    private Label phoneFld;

    // Variables for window dragging
    private double xOffset = 0;
    private double yOffset = 0;
    private int customerId;

    @FXML
    public void initialize() {
        setupBarDragging();
        setupWindowControls();
    }

    public void loadCustomerData(int customerId) {
        this.customerId = customerId;

        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, credit_balance, phone, email, created_at FROM customers WHERE customer_id = ?")) {
            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Display customer information
                    customerNameFld.setText(rs.getString("name"));
                    creditBalanceFld.setText("$" + rs.getBigDecimal("credit_balance").toPlainString());
                    phoneFld.setText(rs.getString("phone"));
                    emailFld.setText(rs.getString("email"));

                    // Format created date
                    Timestamp created = rs.getTimestamp("created_at");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                    createdFld.setText("Account created: " + created.toLocalDateTime().format(formatter));

                    // Apply rectangle clip to customer info pane for rounded corners
                    Rectangle clip = new Rectangle(
                            customerInfoPane.getPrefWidth(),
                            customerInfoPane.getPrefHeight()
                    );
                    clip.setArcWidth(20);
                    clip.setArcHeight(20);
                    customerInfoPane.setClip(clip);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupBarDragging() {
        barPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        barPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) barPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private void setupWindowControls() {
        btnMinimize.setOnAction(event -> {
            Stage stage = (Stage) btnMinimize.getScene().getWindow();
            stage.setIconified(true);
        });

        btnExit.setOnAction(event -> {
            Stage stage = (Stage) btnExit.getScene().getWindow();

            // Create fade out transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> stage.close());
            fadeOut.play();
        });
    }

}
