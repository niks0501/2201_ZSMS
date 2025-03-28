module com.company.zenstoresys {


    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires MaterialFX;


    opens controllers to javafx.fxml;
    exports com.company.zenstoresys;
}