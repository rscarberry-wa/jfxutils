module org.gillius.jfxutils.examples {

        requires java.desktop;
        requires javafx.controls;
        requires javafx.fxml;

        requires org.gillius.jfxutils;

        opens org.gillius.jfxutils.examples to javafx.fxml;

        exports org.gillius.jfxutils.examples;
}