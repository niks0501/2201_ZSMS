package services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import table_models.Product;
import other_classes.ProductDAO;

public class ProductLoadService extends Service<ObservableList<Product>> {

    // Singleton instance
    private static ProductLoadService instance;

    // Private constructor for singleton pattern
    private ProductLoadService() {
        // Register restart listener to reset service when it's done
        setOnSucceeded(event -> {
            // Ready to be restarted when needed
        });
    }

    // Get singleton instance
    public static synchronized ProductLoadService getInstance() {
        if (instance == null) {
            instance = new ProductLoadService();
        }
        return instance;
    }

    @Override
    protected Task<ObservableList<Product>> createTask() {
        return new Task<>() {
            @Override
            protected ObservableList<Product> call() {
                // Load products in background thread
                return ProductDAO.getAllProducts();
            }
        };
    }

    // Method to restart the service
    public void reloadProducts() {
        if (getState() == State.RUNNING) {
            // If already running, wait for it to complete
            return;
        }

        // Reset if in succeeded or failed state
        if (getState() == State.SUCCEEDED || getState() == State.FAILED) {
            reset();
        }

        // Start loading data
        restart();
    }
}