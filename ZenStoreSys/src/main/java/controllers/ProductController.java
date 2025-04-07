package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXPagination;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class ProductController {

    @FXML
    private TableColumn<?, ?> barcodeColumn;

    @FXML
    private MFXButton btnAddProduct;

    @FXML
    private Button btnPopup;

    @FXML
    private TableColumn<?, ?> categoryColumn;

    @FXML
    private TableColumn<?, ?> costPriceColumn;

    @FXML
    private TableColumn<?, ?> markupColumn;

    @FXML
    private Pane prodContentPane;

    @FXML
    private StackPane prodMainFrame;

    @FXML
    private TableColumn<?, ?> productImgColumn;

    @FXML
    private TableColumn<?, ?> productNameColumn;

    @FXML
    private TableView<?> productTbl;

    @FXML
    private MFXPagination productTblPage;

    @FXML
    private MFXTextField searchFld;

    @FXML
    private TableColumn<?, ?> sellingPriceColumn;

    @FXML
    private MFXComboBox<?> sortTbl;

    @FXML
    private TableColumn<?, ?> stocksColumn;

}
