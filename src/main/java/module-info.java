module com.spectrum.emulator {
    // Platform modules
    requires java.desktop;

    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Exported domain packages
    exports com.spectrum.cpu;
    exports com.spectrum.io;
    exports com.spectrum.machine;
    exports com.spectrum.memory;
    exports com.spectrum.sound;
    exports com.spectrum.storage;
    exports com.spectrum.ui;
    exports com.spectrum.ula;

    // Open packages for reflection & dependency injection
    opens com.spectrum.ui to javafx.fxml, javafx.graphics;
    opens com.spectrum.storage to javafx.base;
}
