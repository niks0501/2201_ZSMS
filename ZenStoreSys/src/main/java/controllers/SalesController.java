package controllers;

import io.github.palexdev.materialfx.controls.*;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class SalesController {

    @FXML
    private MFXButton btnCheckout;

    @FXML
    private MFXButton btnDiscounts;

    @FXML
    private Pane cameraFrame;

    @FXML
    private TableColumn<?, ?> discountColumn;

    @FXML
    private Label discountPercentage;

    @FXML
    private MFXProgressBar discountedProdProgress;

    @FXML
    private TableColumn<?, ?> finalPriceColumn;

    @FXML
    private ImageView pictureDialog;

    @FXML
    private TableColumn<?, ?> productIdColumn;

    @FXML
    private TableColumn<?, ?> productNameColumn;

    @FXML
    private TableColumn<?, ?> productQtyColumn;

    @FXML
    private ListView<?> productsListView;

    @FXML
    private StackPane saleMainFrame;

    @FXML
    private Pane salesContentPane;

    @FXML
    private TableView<?> salesTbl;

    @FXML
    private MFXTextField searchFld;

    @FXML
    private TableColumn<?, ?> subtotalColumn;

    @FXML
    private MFXProgressSpinner insertionProgress;

    @FXML
    private MFXRadioButton applyDiscBtn;

    @FXML
    private MFXTextField totalAmountFld;

    @FXML
    private MFXToggleButton toggleMode;

    private boolean isCameraMode = false;

    @FXML
    private void initialize() {
        // Ensure initial visibility states
        productsListView.setVisible(true);
        productsListView.setOpacity(1.0);
        cameraFrame.setVisible(false);
        cameraFrame.setOpacity(0.0);
        searchFld.setDisable(false); // Search field enabled initially

        // Set up toggle action
        toggleMode.setOnAction(event -> toggleMode());
    }

    private void toggleMode() {
        isCameraMode = !isCameraMode;

        // Create fade transitions
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300));
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300));

        if (isCameraMode) {
            // Fade out productsListView, fade in cameraFrame
            fadeOut.setNode(productsListView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeIn.setNode(cameraFrame);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            // Set visibility and disable search after fade out
            fadeOut.setOnFinished(e -> {
                productsListView.setVisible(false);
                cameraFrame.setVisible(true);
                searchFld.setDisable(true); // Disable search in camera mode
            });
        } else {
            // Fade out cameraFrame, fade in productsListView
            fadeOut.setNode(cameraFrame);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeIn.setNode(productsListView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            // Set visibility and enable search after fade out
            fadeOut.setOnFinished(e -> {
                cameraFrame.setVisible(false);
                productsListView.setVisible(true);
                searchFld.setDisable(false); // Enable search in list mode
            });
        }

        // Play animations
        fadeOut.play();
        fadeIn.play();
    }
}