module com.company.zenstoresys {


    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires MaterialFX;
    requires java.sql;
    requires com.google.zxing.javase;
    requires com.google.zxing;


    opens controllers to javafx.fxml;
    exports com.company.zenstoresys;
}