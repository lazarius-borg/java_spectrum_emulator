module nl.invokedynamic.spectrum.emulator {
    // Platform modules
    requires java.desktop;

    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Exported domain packages
    exports nl.invokedynamic.spectrum.cpu;
    exports nl.invokedynamic.spectrum.io;
    exports nl.invokedynamic.spectrum.machine;
    exports nl.invokedynamic.spectrum.memory;
    exports nl.invokedynamic.spectrum.sound;
    exports nl.invokedynamic.spectrum.storage;
    exports nl.invokedynamic.spectrum.ui;
    exports nl.invokedynamic.spectrum.ula;

    // Open packages for reflection & dependency injection
    opens nl.invokedynamic.spectrum.ui to javafx.fxml, javafx.graphics;
    opens nl.invokedynamic.spectrum.storage to javafx.base;
}
