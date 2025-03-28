package controllers;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


public class LoadingController {

    @FXML
    private Label loadingLbl;

    @FXML
    private MFXProgressBar progressBar;

    @FXML
    private MFXButton btnExit;

    @FXML
    private void onExit() {
        javafx.stage.Stage stage = (javafx.stage.Stage) btnExit.getScene().getWindow();
        stage.close();
    }

}
