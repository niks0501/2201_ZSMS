package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import other_classes.DBConnect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML
    private Pane barPane;

    @FXML
    private MFXButton btnExit;

    @FXML
    private Button btnNotif;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private Button btnNav;

    @FXML
    private HBox navigationPane;

    @FXML
    public StackPane contentPane;

    @FXML
    private AnchorPane mainF;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean topBarVisible = false;

    // Notification components
    private Popup notifPopup;
    private ListView<String> notifList;
    private Circle notifDot;
    private List<Notification> notifications;

    private static class Notification {
        int id;
        String type; // "price_change" or "low_stock"
        String message;
        boolean isRead;

        Notification(int id, String type, String message, boolean isRead) {
            this.id = id;
            this.type = type;
            this.message = message;
            this.isRead = isRead;
        }
    }

    @FXML
    private void initialize() {
        // Set up event handlers for exit and minimize buttons
        btnExit.setOnAction(event -> handleExit());
        btnMinimize.setOnAction(event -> handleMinimize());
        btnNav.setOnAction(event -> toggleNavigation());

        // Set up window dragging
        barPane.setOnMousePressed(this::handleMousePressed);
        barPane.setOnMouseDragged(this::handleMouseDragged);

        // Hide navigation pane initially and position it
        navigationPane.setVisible(false);
        navigationPane.setTranslateY(-navigationPane.getPrefHeight());

        // Initialize notification system
        setupNotificationSystem();

        // Load TopBarController and pass contentPane reference
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/company/zenstoresys/topbar.fxml"));
            Parent topBar = loader.load();

            // Get the controller and pass the contentPane reference
            TopBarController topBarController = loader.getController();
            topBarController.setContentPane(contentPane);

            // Add click listeners to all button in the topbar
            addButtonClickListeners(topBar);

            // Clear and add the newly loaded topbar to navigationPane
            navigationPane.getChildren().clear();
            navigationPane.getChildren().add(topBar);

            // Initialize with dashboard view automatically
            topBarController.loadDashCompView();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load TopBarController: " + e.getMessage());
        }
    }

    private void setupNotificationSystem() {
        // Log parent for debugging
        System.out.println("btnNotif parent: " + (btnNotif.getParent() != null ? btnNotif.getParent().getClass().getName() : "null"));

        // Initialize notifications list
        notifications = new ArrayList<>();
        loadNotificationsFromDatabase();

        // Create notification dot
        notifDot = new Circle(5, Color.RED);
        notifDot.setVisible(hasUnreadNotifications());

        // Add notifDot to btnNotif's parent (Pane) directly
        Parent parent = btnNotif.getParent();
        if (parent instanceof Pane pane) {
            pane.getChildren().add(notifDot);
            // Position notifDot relative to btnNotif (top-right corner)
            notifDot.setLayoutX(btnNotif.getLayoutX() + btnNotif.getPrefWidth() - 5);
            notifDot.setLayoutY(btnNotif.getLayoutY() - 5);
        } else {
            System.err.println("Unexpected parent type for btnNotif: " + (parent != null ? parent.getClass().getName() : "null"));
            return; // Exit to avoid further errors
        }

        // Create notification popup
        notifPopup = new Popup();
        notifList = new ListView<>();
        notifList.setPrefWidth(300);
        notifList.setPrefHeight(200);
        notifList.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-border-radius: 5px; -fx-padding: 10px;");

        // Populate ListView with notifications
        updateNotificationList();

        // Handle notification clicks
        notifList.setOnMouseClicked(event -> {
            String selected = notifList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                for (Notification notif : notifications) {
                    if (notif.message.equals(selected)) {
                        markNotificationAsRead(notif.id, notif.type);
                        notif.isRead = true;
                        break;
                    }
                }
                updateNotificationList();
                notifDot.setVisible(hasUnreadNotifications());
                System.out.println("Clicked: " + selected);
            }
        });

        // Wrap ListView in a VBox for styling
        VBox notifBox = new VBox(notifList);
        notifBox.setPadding(new Insets(10));
        notifBox.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-border-radius: 5px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        notifPopup.getContent().add(notifBox);

        // Toggle popup on btnNotif click
        btnNotif.setOnMouseClicked(e -> {
            if (notifPopup.isShowing()) {
                notifPopup.hide();
            } else {
                double x = btnNotif.localToScreen(btnNotif.getBoundsInLocal()).getMinX();
                double y = btnNotif.localToScreen(btnNotif.getBoundsInLocal()).getMaxY();
                notifPopup.show(btnNotif, x, y);
            }
            e.consume(); // Prevent navigation pane from closing
        });

        // Periodically check for new notifications (every 10 seconds)
        Timeline notifChecker = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            int oldSize = notifications.size();
            loadNotificationsFromDatabase();
            if (notifications.size() > oldSize) {
                updateNotificationList();
                notifDot.setVisible(hasUnreadNotifications());
                animateBell();
            }
        }));
        notifChecker.setCycleCount(Timeline.INDEFINITE);
        notifChecker.play();
    }

    private void loadNotificationsFromDatabase() {
        notifications.clear();

        // Load price change notifications
        String priceQuery = "SELECT pp.price_id, p.name, pp.old_price, pp.new_price, pp.change_date, pp.is_read " +
                "FROM product_prices pp " +
                "JOIN products p ON pp.product_id = p.product_id " +
                "ORDER BY pp.change_date DESC LIMIT 5";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(priceQuery);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String message = String.format("💰 Price Change: %s from $%.2f to $%.2f",
                        rs.getString("name"), rs.getDouble("old_price"), rs.getDouble("new_price"));
                notifications.add(new Notification(
                        rs.getInt("price_id"),
                        "price_change",
                        message,
                        rs.getBoolean("is_read")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Load low stock notifications
        String stockQuery = "SELECT lsa.alert_id, p.name, lsa.stock_level, lsa.alert_date, lsa.is_read " +
                "FROM low_stock_alerts lsa " +
                "JOIN products p ON lsa.product_id = p.product_id " +
                "ORDER BY lsa.alert_date DESC LIMIT 5";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(stockQuery);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String message = String.format("📦 Low Stock: %s at %d units",
                        rs.getString("name"), rs.getInt("stock_level"));
                notifications.add(new Notification(
                        rs.getInt("alert_id"),
                        "low_stock",
                        message,
                        rs.getBoolean("is_read")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void markNotificationAsRead(int id, String type) {
        String query;
        if (type.equals("price_change")) {
            query = "UPDATE product_prices SET is_read = TRUE WHERE price_id = ?";
        } else {
            query = "UPDATE low_stock_alerts SET is_read = TRUE WHERE alert_id = ?";
        }
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateNotificationList() {
        notifList.getItems().clear();
        for (Notification notif : notifications) {
            notifList.getItems().add(notif.message);
        }
    }

    private boolean hasUnreadNotifications() {
        return notifications.stream().anyMatch(notif -> !notif.isRead);
    }

    private void animateBell() {
        RotateTransition shake = new RotateTransition(Duration.millis(100), btnNotif);
        shake.setFromAngle(-10);
        shake.setToAngle(10);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setInterpolator(Interpolator.EASE_BOTH);
        shake.play();
    }

    private void addButtonClickListeners(Parent parent) {
        // Find all button controls in the parent node and its children
        parent.lookupAll(".button").forEach(node -> {
            if (node instanceof Button button && button != btnNotif) { // Exclude btnNotif
                addCloseActionToButton(button);
            }
        });

        // Find all MFXButton controls specifically
        parent.lookupAll(".mfx-button").forEach(node -> {
            if (node instanceof MFXButton mfxButton) {
                addCloseActionToButton(mfxButton);
            }
        });
    }

    private void addCloseActionToButton(Button button) {
        // Store original action
        javafx.event.EventHandler<javafx.event.ActionEvent> originalHandler = button.getOnAction();

        // Replace with new action that closes navigation pane after executing original action
        button.setOnAction(event -> {
            // Call original handler if it exists
            if (originalHandler != null) {
                originalHandler.handle(event);
            }

            // Then hide the navigation pane if it's visible
            if (topBarVisible) {
                hideNavigationPane();
            }
        });
    }

    private void hideNavigationPane() {
        // Create slide-up animation
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(200), navigationPane);
        slideUp.setToY(-navigationPane.getHeight());
        slideUp.setInterpolator(Interpolator.EASE_OUT);

        // Create fade-out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), navigationPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Play animations together
        ParallelTransition transition = new ParallelTransition(slideUp, fadeOut);
        transition.setOnFinished(e -> {
            navigationPane.setVisible(false);
            topBarVisible = false;
        });
        transition.play();
    }

    private void toggleNavigation() {
        if (!topBarVisible) {
            // Show navigation bar with slide down animation and fade in
            navigationPane.setVisible(true);
            navigationPane.setOpacity(0); // Start fully transparent

            // Create slide down transition
            TranslateTransition slideDown = new TranslateTransition(Duration.millis(200), navigationPane);
            slideDown.setFromY(-navigationPane.getPrefHeight());
            slideDown.setToY(0);
            slideDown.setInterpolator(Interpolator.EASE_OUT);

            // Create fade in transition
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), navigationPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(Interpolator.EASE_BOTH);

            // Play both animations in parallel
            ParallelTransition parallelTransition = new ParallelTransition(slideDown, fadeIn);

            // Rotate the button counter-clockwise after animations complete
            parallelTransition.setOnFinished(event -> {
                RotateTransition rotateBtn = new RotateTransition(Duration.millis(5), btnNav);
                rotateBtn.setByAngle(-90);
                rotateBtn.setInterpolator(Interpolator.EASE_BOTH);
                rotateBtn.play();
                topBarVisible = true;
            });

            parallelTransition.play();
        } else {
            // Use only hideNavigationPane() for consistent behavior
            hideNavigationPane();

            // Rotate button back
            RotateTransition rotateBtn = new RotateTransition(Duration.millis(5), btnNav);
            rotateBtn.setByAngle(90);
            rotateBtn.setInterpolator(Interpolator.EASE_BOTH);
            rotateBtn.play();
        }
    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) btnExit.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) btnMinimize.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) barPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}