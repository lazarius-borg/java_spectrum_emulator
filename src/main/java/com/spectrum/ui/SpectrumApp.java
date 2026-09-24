package com.spectrum.ui;

import com.spectrum.machine.SpectrumMachine;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application entry point for the ZX Spectrum 128K emulator.
 * Loads the FXML-based retro TUI interface.
 */
public final class SpectrumApp extends Application {

    private SpectrumController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/spectrum/ui/main_view.fxml"));
        Parent root = loader.load();
        this.controller = loader.getController();
        controller.setStage(stage);

        Scene scene = new Scene(root, 1040, 820);

        // Forward host keyboard events to the Spectrum keyboard and joystick mapper
        scene.setOnKeyPressed(controller.getKeyboardMapper()::handleKeyPressed);
        scene.setOnKeyReleased(controller.getKeyboardMapper()::handleKeyReleased);

        stage.setTitle("ZX Spectrum 128K Emulator");
        stage.setScene(scene);
        stage.setMinWidth(780);
        stage.setMinHeight(620);
        stage.setOnCloseRequest(e -> {
            controller.shutdown();
            Platform.exit();
        });

        stage.show();
        controller.start();
    }

    public SpectrumController getController() {
        return controller;
    }

    public SpectrumMachine getMachine() {
        return controller != null ? controller.getMachine() : null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
