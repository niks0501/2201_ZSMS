module com.company.zenstoresys {


    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires MaterialFX;
    requires java.sql;
    requires com.google.zxing.javase;
    requires com.google.zxing;
    requires webcam.capture;
    requires org.slf4j;
    requires org.slf4j.nop;
    requires org.apache.pdfbox;
    requires org.apache.pdfbox.io;
    requires java.mail;


    opens controllers to javafx.fxml;
    opens table_models to javafx.base, javafx.fxml;
    exports com.company.zenstoresys;
}