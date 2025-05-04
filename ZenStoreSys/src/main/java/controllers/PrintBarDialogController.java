package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import other_classes.ProductDAO;
import table_models.Product;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PrintBarDialogController {

    @FXML
    private AnchorPane mainPane;

    @FXML
    private MFXButton btnExit;

    @FXML
    private MFXButton btnMinimize;

    @FXML
    private Pane barPane;

    @FXML
    private MFXButton btnPrint;

    @FXML
    private ListView<Product> productsListView;

    @FXML
    private MFXToggleButton toggleMSelection;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar printProgress;

    private final BooleanProperty multipleSelection = new SimpleBooleanProperty(false);

    private double xOffset = 0;
    private double yOffset = 0;


    @FXML
    public void initialize() {
        // Initial setup - don't load products yet
        productsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        toggleMSelection.selectedProperty().bindBidirectional(multipleSelection);
        toggleMSelection.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectionMode(newVal));
        setupCellFactory();
        btnPrint.setOnAction(e -> printBarcodes());
        btnExit.setOnAction(e -> ((Stage) mainPane.getScene().getWindow()).close());
        btnMinimize.setOnAction(e -> {
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setIconified(true);
        });

        // Make barPane draggable
        barPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        barPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        printProgress.setVisible(true);
        printProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setVisible(true);
        statusLabel.setText("Loading products...");

        // Load products asynchronously
        loadProductsAsync();
    }


    private void loadProductsAsync() {
        new Thread(() -> {
            try {
                // Do database work in background thread
                ObservableList<Product> products = ProductDAO.getAllProducts();

                // Update UI on JavaFX thread after loading completes
                javafx.application.Platform.runLater(() -> {
                    productsListView.setItems(products);
                    printProgress.setVisible(false);
                    statusLabel.setText("Ready to print barcodes");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    printProgress.setVisible(true);
                    statusLabel.setText("Error loading products");
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load products: " + e.getMessage());
                });
            }
        }).start();
    }

    private void updateSelectionMode(boolean isMultipleSelection) {
        if (isMultipleSelection) {
            productsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        } else {
            productsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            productsListView.getSelectionModel().clearSelection();
        }
        productsListView.refresh();
    }

    private void setupCellFactory() {
        productsListView.setCellFactory(listView -> new ListCell<Product>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (multipleSelection.get()) {
                        checkBox.setText(product.getName());
                        checkBox.setSelected(getListView().getSelectionModel().getSelectedItems().contains(product));
                        checkBox.setOnAction(event -> {
                            if (checkBox.isSelected()) {
                                getListView().getSelectionModel().select(getIndex());
                            } else {
                                getListView().getSelectionModel().clearSelection(getIndex());
                            }
                            event.consume();
                        });
                        setGraphic(checkBox);
                        setText(null);
                    } else {
                        setText(product.getName());
                        setGraphic(null);
                    }
                }
            }
        });
        productsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> productsListView.refresh());
    }

    private void printBarcodes() {
        List<Product> selectedProducts = new ArrayList<>();
        if (multipleSelection.get()) {
            selectedProducts.addAll(productsListView.getSelectionModel().getSelectedItems());
        } else if (productsListView.getSelectionModel().getSelectedItem() != null) {
            selectedProducts.add(productsListView.getSelectionModel().getSelectedItem());
        }
        if (selectedProducts.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No products selected", "Please select at least one product to print barcodes.");
            return;
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Save Barcode PDF");
        File directory = directoryChooser.showDialog(mainPane.getScene().getWindow());
        if (directory == null) return;

        // Initialize progress
        printProgress.setVisible(true);
        printProgress.setProgress(0);
        statusLabel.setVisible(true);
        statusLabel.setText("Generating barcodes...");

        new Thread(() -> {
            try {
                String fileName = generateBarcodesPDF(selectedProducts, directory);
                javafx.application.Platform.runLater(() -> {
                    printProgress.setVisible(false);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Barcodes have been generated successfully.\nSaved to: " + fileName);
                    statusLabel.setText("Completed successfully!");
                });
            } catch (IOException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    printProgress.setVisible(false);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate barcodes: " + e.getMessage());
                    statusLabel.setText("Failed to generate PDF");
                });
            }
        }).start();
    }

    private String generateBarcodesPDF(List<Product> products, File directory) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = directory.getAbsolutePath() + File.separator + "barcodes_" + timestamp + ".pdf";

        // Calculate total number of barcodes for progress tracking
        int totalBarcodesToGenerate = products.stream()
                .filter(product -> product.getBarcodePath() != null && !product.getBarcodePath().isEmpty())
                .mapToInt(product -> Math.max(1, product.getStock()))
                .sum();

        // For progress updates
        final int[] barcodesGenerated = {0};

        try (PDDocument document = new PDDocument()) {
            int barcodesPerRow = 4; // Changed from 3 to 4 columns

            // Use landscape orientation for better fit
            PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float marginX = 40;
            float marginY = 40;

            // Keep barcode dimensions exactly as set
            float barcodeWidth = 150 * (72f/96f);
            float barcodeHeight = 35 * (72f/96f);
            float labelSpace = 25; // Space for label below barcode

            // Calculate available space
            float usableWidth = pageWidth - 2 * marginX;
            float usableHeight = pageHeight - 2 * marginY;

            // Calculate optimal spacing to fill the page width
            float horizontalSpacing = (usableWidth - (barcodesPerRow * barcodeWidth)) / (barcodesPerRow - 1);

            // Calculate how many rows can fit on a page
            float itemHeight = barcodeHeight + labelSpace;
            int barcodesPerCol = (int) Math.floor(usableHeight / itemHeight);

            // Calculate optimal vertical spacing to fill the page height
            float verticalSpacing = (usableHeight - (barcodesPerCol * itemHeight)) / (barcodesPerCol - 1);

            int barcodesPerPage = barcodesPerRow * barcodesPerCol;
            int totalBarcodesGenerated = 0;

            for (Product product : products) {
                if (product.getBarcodePath() == null || product.getBarcodePath().isEmpty()) {
                    continue;
                }

                PDImageXObject barcodeImage = PDImageXObject.createFromFile(product.getBarcodePath(), document);
                int numberOfBarcodes = product.getStock();
                if (numberOfBarcodes <= 0) numberOfBarcodes = 1;

                for (int i = 0; i < numberOfBarcodes; i++) {
                    int position = totalBarcodesGenerated % barcodesPerPage;
                    int row = position / barcodesPerRow;
                    int col = position % barcodesPerRow;

                    if (position == 0) {
                        // Add a landscape page
                        PDPage page = new PDPage(pageSize);
                        document.addPage(page);
                    }

                    PDPage page = document.getPage(document.getNumberOfPages() - 1);

                    // Calculate position to maximize space usage
                    float x = marginX + (col * (barcodeWidth + horizontalSpacing));
                    float y = pageHeight - marginY - (row * (itemHeight + verticalSpacing)) - barcodeHeight;

                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                        // Draw barcode with fixed dimensions
                        contentStream.drawImage(barcodeImage, x, y, barcodeWidth, barcodeHeight);

                        // Add product name centered under the barcode
                        String productName = product.getName();
                        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                        float fontSize = 8;
                        float textWidth = font.getStringWidth(productName) / 1000 * fontSize;
                        float textX = x + (barcodeWidth - textWidth) / 2;
                        float textY = y - 12; // Position text below barcode

                        contentStream.beginText();
                        contentStream.setFont(font, fontSize);
                        contentStream.newLineAtOffset(textX, textY);
                        contentStream.showText(productName);
                        contentStream.endText();

                        // Add counter if multiple barcodes
                        if (numberOfBarcodes > 1) {
                            String counter = (i + 1) + "/" + numberOfBarcodes;
                            font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                            textWidth = font.getStringWidth(counter) / 1000 * fontSize;
                            textX = x + barcodeWidth - textWidth - 10;

                            contentStream.beginText();
                            contentStream.setFont(font, fontSize);
                            contentStream.newLineAtOffset(textX, textY - 12);
                            contentStream.showText(counter);
                            contentStream.endText();
                        }
                    }

                    totalBarcodesGenerated++;
                    barcodesGenerated[0]++;

                    // Update progress bar
                    double progress = (double) barcodesGenerated[0] / totalBarcodesToGenerate;
                    javafx.application.Platform.runLater(() -> {
                        printProgress.setProgress(progress);
                        statusLabel.setText("Generated " + barcodesGenerated[0] + " of " + totalBarcodesToGenerate);
                    });
                }
            }
            document.save(fileName);
        }
        return fileName;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}