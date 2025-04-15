package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import services.ProductLoadService;

import java.io.IOException;

public class TopBarController {

    @FXML
    private HBox navigationPane;

    @FXML
    private Button btnProduct;

    private StackPane contentPane;



    @FXML
    private void initialize() {
        // Add event handler for product button
        btnProduct.setOnAction(event -> loadProductsView());
    }

    // Method to set the contentPane from DashboardController
    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    private void loadProductsView() {
        try {
            // Restart the ProductLoadService to refresh data
            ProductLoadService.getInstance().reloadProducts();

            // Clear current content
            if (contentPane != null) {
                contentPane.getChildren().clear();

                // Load products FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/products.fxml"));
                StackPane productsView = loader.load();

                // Apply CSS
                productsView.getStylesheets().add(getClass().getResource("/css/products.css").toExternalForm());

                // Set initial opacity for animation
                productsView.setOpacity(0);

                // Add to contentPane
                contentPane.getChildren().add(productsView);

                // Create and play fade-in animation
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), productsView);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                fadeIn.play();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}