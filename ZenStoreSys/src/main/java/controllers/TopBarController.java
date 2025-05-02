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
    private Button btnCredits;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnReports;

    @FXML
    private Button btnSales;

    private volatile boolean isLoading = false; // Track loading state



    @FXML
    private void initialize() {
        // Add event handler for product button
        btnProduct.setOnAction(event -> loadProductsView());
        btnSales.setOnAction(event -> loadSalesView());
        btnCredits.setOnAction(event -> loadCreditsView());
        btnReports.setOnAction(event -> loadReportsView());

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
    private void loadReportsView() {
        // Prevent multiple concurrent loading operations
        if (isLoading) {
            return;
        }

        isLoading = true;
        btnReports.setDisable(true);

        // Show loading indicator
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(100, 100);

        if (contentPane != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(loadingIndicator);

            // Load FXML in background thread
            Thread loaderThread = new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/reports.fxml"));
                    StackPane reportsView = loader.load();
                    reportsView.getStylesheets().add(getClass().getResource("/css/reports.css").toExternalForm());
                    reportsView.getStylesheets().add(getClass().getResource("/css/customDateRange.css").toExternalForm());

                    // Update UI on JavaFX thread when ready
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        reportsView.setOpacity(0);
                        contentPane.getChildren().add(reportsView);

                        // Fade in the view immediately
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), reportsView);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                        fadeIn.setOnFinished(e -> {
                            isLoading = false;
                            btnReports.setDisable(false);
                        });
                        fadeIn.play();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        Label errorLabel = new Label("Error loading content: " + e.getMessage());
                        contentPane.getChildren().add(errorLabel);
                        isLoading = false;
                        btnReports.setDisable(false);
                    });
                    e.printStackTrace();
                }
            });

            loaderThread.setDaemon(true);
            loaderThread.start();
        }
    }

    @FXML
    private void loadCreditsView() {
        // Prevent multiple concurrent loading operations
        if (isLoading) {
            return;
        }

        isLoading = true;
        btnCredits.setDisable(true);

        // Show loading indicator
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(100, 100);

        if (contentPane != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(loadingIndicator);

            // Load FXML in background thread
            Thread loaderThread = new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/credits.fxml"));
                    StackPane creditsView = loader.load();
                    creditsView.getStylesheets().add(getClass().getResource("/css/credits.css").toExternalForm());

                    // Update UI on JavaFX thread when ready
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        creditsView.setOpacity(0);
                        contentPane.getChildren().add(creditsView);

                        // Fade in the view immediately
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), creditsView);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                        fadeIn.setOnFinished(e -> {
                            isLoading = false;
                            btnCredits.setDisable(false);
                        });
                        fadeIn.play();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        Label errorLabel = new Label("Error loading content: " + e.getMessage());
                        contentPane.getChildren().add(errorLabel);
                        isLoading = false;
                        btnCredits.setDisable(false);
                    });
                    e.printStackTrace();
                }
            });

            loaderThread.setDaemon(true);
            loaderThread.start();
        }
    }

    @FXML
    private void loadSalesView() {
        // Prevent multiple concurrent loading operations
        if (isLoading) {
            return;
        }

        isLoading = true;
        btnSales.setDisable(true);

        // Show loading indicator
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(100, 100);

        if (contentPane != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(loadingIndicator);

            // Load FXML in background thread
            Thread loaderThread = new Thread(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/sales.fxml"));
                    StackPane salesView = loader.load();
                    salesView.getStylesheets().add(getClass().getResource("/css/sales.css").toExternalForm());

                    // Update UI on JavaFX thread when ready
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        salesView.setOpacity(0);
                        contentPane.getChildren().add(salesView);

                        // Fade in the view immediately
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), salesView);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                        fadeIn.setOnFinished(e -> {
                            isLoading = false;
                            btnSales.setDisable(false);
                        });
                        fadeIn.play();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        Label errorLabel = new Label("Error loading content: " + e.getMessage());
                        contentPane.getChildren().add(errorLabel);
                        isLoading = false;
                        btnSales.setDisable(false);
                    });
                    e.printStackTrace();
                }
            });

            loaderThread.setDaemon(true);
            loaderThread.start();
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

                        // Fade in the view immediately
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