package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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

    private volatile boolean isLoading = false; // Track loading state



    @FXML
    private void initialize() {
        // Add event handler for product button
        btnProduct.setOnAction(event -> loadProductsView());

        // Set up ProductLoadService handlers once and for all
        ProductLoadService productService = ProductLoadService.getInstance();

        // This listener ensures button state matches service state automatically
        productService.runningProperty().addListener((obs, wasRunning, isNowRunning) -> {
            Platform.runLater(() -> btnProduct.setDisable(isNowRunning));
        });

        // Set up permanent service handlers
        productService.setOnSucceeded(event -> {
            if (contentPane != null && !contentPane.getChildren().isEmpty()) {
                // Find the view to fade in
                contentPane.getChildren().stream()
                        .filter(node -> node instanceof StackPane)
                        .findFirst()
                        .ifPresent(view -> {
                            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), view);
                            fadeIn.setFromValue(0);
                            fadeIn.setToValue(1);
                            fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                            fadeIn.play();
                        });
            }
            isLoading = false;
        });

        productService.setOnFailed(event -> {
            isLoading = false;
        });
    }

    // Method to set the contentPane from DashboardController
    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    private void loadProductsView() {
        // Prevent multiple concurrent loading operations
        if (isLoading) {
            return;
        }

        isLoading = true;
        btnProduct.setDisable(true);

        // Show loading indicator
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(100, 100);

        if (contentPane != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(loadingIndicator);

            // Load FXML in background thread
            Thread loaderThread = new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/products.fxml"));
                    StackPane productsView = loader.load();
                    productsView.getStylesheets().add(getClass().getResource("/css/products.css").toExternalForm());

                    // Update UI on JavaFX thread when ready
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        productsView.setOpacity(0);
                        contentPane.getChildren().add(productsView);

                        // Just fade in the view immediately
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), productsView);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                        fadeIn.setOnFinished(e -> {
                            isLoading = false;
                            btnProduct.setDisable(false);
                        });
                        fadeIn.play();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        Label errorLabel = new Label("Error loading content: " + e.getMessage());
                        contentPane.getChildren().add(errorLabel);
                        isLoading = false;
                        btnProduct.setDisable(false);
                    });
                    e.printStackTrace();
                }
            });

            loaderThread.setDaemon(true);
            loaderThread.start();
        }
    }
}