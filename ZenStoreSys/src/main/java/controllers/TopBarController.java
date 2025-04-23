package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnSales;

    private volatile boolean isLoading = false; // Track loading state



    @FXML
    private void initialize() {
        // Add event handler for product button
        btnProduct.setOnAction(event -> loadProductsView());
        btnSales.setOnAction(event -> loadSalesView());

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

    @FXML
    private void loadSalesView() {
        try {
            // Create and configure progress indicator
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setMaxSize(100, 100);

            // Add the progress indicator to center of content pane
            contentPane.getChildren().clear();
            StackPane loadingPane = new StackPane(progressIndicator);
            loadingPane.setStyle("-fx-background-color: white;");
            contentPane.getChildren().add(loadingPane);

            // Create loading animation
            Timeline loadingAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progressIndicator.progressProperty(), 0)),
                    new KeyFrame(Duration.seconds(2), new KeyValue(progressIndicator.progressProperty(), 1, Interpolator.EASE_BOTH))
            );
            loadingAnimation.play();

            // Load the sales view in background thread to prevent UI freezing
            Task<Parent> loadTask = new Task<>() {
                @Override
                protected Parent call() throws Exception {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/sales.fxml"));
                    Parent salesView = loader.load();
                    salesView.getStylesheets().add(getClass().getResource("/css/sales.css").toExternalForm());

                    // Apply initial state for animation
                    salesView.setOpacity(0);
                    return salesView;
                }
            };

            loadTask.setOnSucceeded(event -> {
                Parent salesView = loadTask.getValue();

                // Create fade-in transition
                FadeTransition fadeIn = new FadeTransition(Duration.millis(600), salesView);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.setInterpolator(Interpolator.EASE_IN);

                // Switch from loading indicator to the actual view with animation
                fadeIn.setOnFinished(e -> {
                    // Ensure any resources are properly managed
                });

                // Replace loading indicator with sales view and start animation
                contentPane.getChildren().clear();
                contentPane.getChildren().add(salesView);
                fadeIn.play();
            });

            loadTask.setOnFailed(event -> {
                Throwable exception = loadTask.getException();
                exception.printStackTrace();

                // Show error message
                Label errorLabel = new Label("Error loading Sales view: " + exception.getMessage());
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                contentPane.getChildren().clear();
                contentPane.getChildren().add(new StackPane(errorLabel));
            });

            // Start loading in background
            new Thread(loadTask).start();

        } catch (Exception e) {
            e.printStackTrace();

            // Show error in UI
            Label errorLabel = new Label("Error: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            contentPane.getChildren().clear();
            contentPane.getChildren().add(errorLabel);
        }
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