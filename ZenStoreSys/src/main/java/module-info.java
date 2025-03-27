module com.company.zenstoresys {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.company.zenstoresys to javafx.fxml;
    exports com.company.zenstoresys;
}