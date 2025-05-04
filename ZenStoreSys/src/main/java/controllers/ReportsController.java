package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ReportsController {

    @FXML
    private Button btnPullReport;

    @FXML
    private VBox fileContainer;

    @FXML
    private Pane reportsContentPane;

    @FXML
    private StackPane reportsMainFrame;

    @FXML
    private ScrollPane vboxContainer;

    @FXML
    private MFXComboBox<String> sortFile;

    @FXML
    private MFXButton refreshReloadFile;

    @FXML
    private StackPane genReportContainer;

    private String currentSortCriterion = "Date"; // Default sort criterion

    // This Node will hold the content loaded from report-generate.fxml
    private Node reportGenerateNode;
    private boolean isReportGenerateVisible = false;

    // Width of the genReportContainer (should match report-generate.fxml prefWidth)
    private final double reportGeneratePaneWidth = 500.0;
    // Duration of the slide animation in milliseconds
    private final double slideDurationMillis = 350;
    // Target X position for the genReportContainer when it becomes visible
    private final double targetXGenReportVisible = -500;
    // Target X position for the btnPullReport when the genReportContainer is visible
    private final double targetXBtnPullReportVisible = reportGeneratePaneWidth;
    // Target X position for the reportsContentPane when the genReportContainer is visible
    private final double targetXReportsContentVisible = reportGeneratePaneWidth;
    // Initial X position for the genReportContainer (off-screen left)
    private final double initialXGenReportHidden = -reportGeneratePaneWidth;
    // Initial X position for the reportsContentPane (usually 0)
    private final double initialXReportsContent = 0.0;
    // Initial X position for the btnPullReport (usually 0)
    private final double initialXBtnPullReport = 0.0;

    // Base directory for reports
    private final String reportsBaseDir = "C:\\Users\\Nikko\\Documents\\IntelliJ IDEA Projects\\ZenStore\\ZenStoreSys\\src\\main\\resources\\reports";

    @FXML
    public void initialize() {
        // Ensure genReportContainer is present before loading into it
        if (genReportContainer == null) {
            System.err.println("fx:id=\"genReportContainer\" is missing in reports.fxml or not injected.");
            btnPullReport.setDisable(true);
            return;
        }
        loadReportGeneratePane();
        setupButtonAction();
        setupSortOptions();
        loadReportFiles();
    }

    private void setupSortOptions() {
        // Initialize sort options
        sortFile.getItems().addAll("Name", "Date", "Size");
        sortFile.selectItem("Date"); // Default selection

        // Add listener to sort files when selection changes
        sortFile.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSortCriterion = newVal;
                loadReportFiles(); // Reload files with new sorting
            }
        });
    }

    /**
     * Loads the report-generate.fxml file into genReportContainer and prepares it.
     */
    private void loadReportGeneratePane() {
        try {
            URL fxmlUrl = getClass().getResource("/modals/report-generate.fxml");
            // Load as Parent to access stylesheets
            Parent loadedRoot = FXMLLoader.load(Objects.requireNonNull(fxmlUrl, "Cannot find FXML file: /modals/report-generate.fxml"));
            reportGenerateNode = loadedRoot; // Assign to the Node variable

            // Apply the specific CSS to the loaded node
            URL cssUrl = getClass().getResource("/css/report-generate.css");
            if (cssUrl != null) {
                loadedRoot.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warning: Could not find /css/report-generate.css");
            }

            // Add the loaded node to the designated container
            genReportContainer.getChildren().add(reportGenerateNode);

            // Set initial position of the CONTAINER off-screen to the left and hide it
            genReportContainer.setTranslateX(initialXGenReportHidden);
            genReportContainer.setVisible(false);

            // Ensure the container is managed (participates in layout) but invisible
            genReportContainer.setManaged(true);
        } catch (IOException e) {
            System.err.println("Failed to load report-generate.fxml: " + e.getMessage());
            e.printStackTrace();
            btnPullReport.setDisable(true);
        } catch (NullPointerException e) {
            System.err.println("FXML file or CSS file not found. Check paths: /modals/report-generate.fxml and /css/report-generate.css");
            e.printStackTrace();
            btnPullReport.setDisable(true);
        }
    }

    /**
     * Sets up the action handler for the btnPullReport button.
     */
    private void setupButtonAction() {
        btnPullReport.setOnAction(event -> toggleReportGeneratePane());
    }

    /**
     * Loads all generated report PDF files into fileContainer.
     */
    private void loadReportFiles() {
        fileContainer.getChildren().clear(); // Clear existing content
        File baseDir = new File(reportsBaseDir);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            Label noFilesLabel = new Label("No reports found in the specified directory.");
            noFilesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
            fileContainer.getChildren().add(noFilesLabel);
            return;
        }

        // Get PDF files directly from reports directory
        File[] pdfFiles = baseDir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            Label noFilesLabel = new Label("No PDF reports found in the reports directory.");
            noFilesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
            fileContainer.getChildren().add(noFilesLabel);
            return;
        }

        // Sort files based on the selected criterion
        sortFiles(pdfFiles);

        for (File pdf : pdfFiles) {
            MFXButton fileButton = createFileButton(pdf);
            fileContainer.getChildren().add(fileButton);
        }

        // Apply CSS to fileContainer
        URL cssUrl = getClass().getResource("/css/reports.css");
        if (cssUrl != null) {
            fileContainer.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: Could not find /css/reports.css");
        }
    }

    private void sortFiles(File[] files) {
        switch (currentSortCriterion) {
            case "Name":
                // Sort alphabetically by name
                java.util.Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                break;
            case "Date":
                // Sort by creation date (newest first)
                java.util.Arrays.sort(files, (f1, f2) -> {
                    try {
                        BasicFileAttributes attr1 = Files.readAttributes(f1.toPath(), BasicFileAttributes.class);
                        BasicFileAttributes attr2 = Files.readAttributes(f2.toPath(), BasicFileAttributes.class);
                        // Reverse order for newest first
                        return attr2.creationTime().compareTo(attr1.creationTime());
                    } catch (IOException e) {
                        return 0; // If error, don't change order
                    }
                });
                break;
            case "Size":
                // Sort by file size (largest first)
                java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.length(), f1.length()));
                break;
        }
    }

    /**
     * Creates a styled MFXButton for a PDF file with metadata.
     */
    private MFXButton createFileButton(File pdf) {
        // Get metadata
        String fileName = pdf.getName();
        String creationDate = getFileCreationDate(pdf);
        String fileSize = getFileSize(pdf);

        // Extract report type from filename (e.g., "daily_sales_..." -> "Daily")
        String reportType = "Unknown";
        if (fileName.startsWith("daily_sales_")) {
            reportType = "Daily";
        } else if (fileName.startsWith("weekly_sales_")) {
            reportType = "Weekly";
        } else if (fileName.startsWith("monthly_sales_")) {
            reportType = "Monthly";
        } else if (fileName.startsWith("custom_sales_")) {
            reportType = "Custom";
        }

        // Create button text
        String buttonText = String.format("%s\nType: %s | Created: %s | Size: %s",
                fileName, reportType, creationDate, fileSize);

        MFXButton button = new MFXButton(buttonText);
        button.setPrefWidth(780); // Fit within 812 px with padding
        button.setPrefHeight(75);
        button.setWrapText(true);
        button.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        button.getStyleClass().add("file-button");

        // Add tooltip with full path
        Tooltip tooltip = new Tooltip(pdf.getAbsolutePath());
        button.setTooltip(tooltip);

        // Set action to open file
        button.setOnAction(e -> openFile(pdf));

        // Add context menu for file operations
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            // Show confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Delete");
            confirmDialog.setHeaderText("Delete Report");
            confirmDialog.setContentText("Are you sure you want to delete this report?");

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (pdf.delete()) {
                        loadReportFiles(); // Refresh the file list after deletion
                    } else {
                        showAlert("Error", "Could not delete the file. It may be in use by another application.");
                    }
                }
            });
        });

        contextMenu.getItems().add(deleteItem);

        // Apply styling to context menu
        contextMenu.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #81B29A; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 8, 0, 0, 3); " +
                        "-fx-min-width: 120px;"
        );

        // Style the menu item
        for (MenuItem item : contextMenu.getItems()) {
            item.setStyle(
                    "-fx-background-radius: 3px; " +
                            "-fx-padding: 8px 12px;"
            );
        }

        // Apply styling to context menu
        contextMenu.getStyleClass().add("file-context-menu");

        // Attach context menu to button
        button.setContextMenu(contextMenu);

        return button;
    }

    /**
     * Gets the creation date of a file.
     */
    private String getFileCreationDate(File file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(new Date(attrs.creationTime().toMillis()));
        } catch (IOException e) {
            return "Unknown";
        }
    }

    /**
     * Gets the file size in KB or MB.
     */
    private String getFileSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Opens the specified PDF file using the system's default PDF viewer.
     */
    private void openFile(File pdf) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdf);
            } else {
                showAlert("Error", "Cannot open file: Platform does not support file opening.");
            }
        } catch (IOException e) {
            System.err.println("Failed to open file: " + pdf.getName());
            e.printStackTrace();
            showAlert("Error", "Failed to open file: " + e.getMessage());
        }
    }

    /**
     * Shows an error alert.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Toggles the visibility of the genReportContainer with a slide animation.
     * It also slides the reportsContentPane to make space and refreshes the file list.
     */
    private void toggleReportGeneratePane() {
        if (genReportContainer == null || reportGenerateNode == null || btnPullReport == null) {
            System.err.println("genReportContainer, its content, or btnPullReport is not loaded/injected. Cannot perform animation.");
            return;
        }

        Duration duration = Duration.millis(slideDurationMillis);

        // Create transitions for the CONTAINER, the content pane, and the button
        TranslateTransition ttGenReportContainer = new TranslateTransition(duration, genReportContainer);
        TranslateTransition ttContent = new TranslateTransition(duration, reportsContentPane);
        TranslateTransition ttButton = new TranslateTransition(duration, btnPullReport);

        if (!isReportGenerateVisible) {
            // --- Slide In ---
            genReportContainer.setVisible(true);
            ttGenReportContainer.setToX(targetXGenReportVisible);
            ttContent.setToX(targetXReportsContentVisible);
            ttButton.setToX(targetXBtnPullReportVisible);

            // Play animations
            ttGenReportContainer.play();
            ttContent.play();
            ttButton.play();
            isReportGenerateVisible = true;
        } else {
            // --- Slide Out ---
            ttGenReportContainer.setToX(initialXGenReportHidden);
            ttContent.setToX(initialXReportsContent);
            ttButton.setToX(initialXBtnPullReport);

            // Refresh file list and hide the sliding CONTAINER after animation
            ttGenReportContainer.setOnFinished(e -> {
                genReportContainer.setVisible(false);
                loadReportFiles(); // Refresh the file list
                ttGenReportContainer.setOnFinished(null);
            });

            // Play animations
            ttGenReportContainer.play();
            ttContent.play();
            ttButton.play();
            isReportGenerateVisible = false;
        }
    }
}