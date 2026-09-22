package com.spectrum.ui;

import com.spectrum.io.Joystick;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Interactive configuration and testing dialog for ZX Spectrum Joysticks.
 */
public final class JoystickSettingsDialog extends Dialog<Void> {

    public JoystickSettingsDialog(Window owner, Joystick joystick, KeyboardMapper keyboardMapper, OnScreenJoystickView mainJoyView) {
        initOwner(owner);
        setTitle("ZX Spectrum Joystick Settings & Tester");
        setHeaderText("Configure Joystick Interface & Host Keyboard Profile");

        DialogPane pane = getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setStyle("-fx-background-color: #24242A;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(12));
        content.setPrefWidth(540);

        // 1. Spectrum Interface Selection
        VBox interfaceBox = new VBox(6);
        Label ifaceHeader = new Label("Spectrum Joystick Interface:");
        ifaceHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #00E5FF; -fx-font-size: 12px;");

        ToggleGroup ifaceGroup = new ToggleGroup();
        RadioButton rbKempston = new RadioButton("Kempston Joystick (Port 0x1F) - Recommended");
        RadioButton rbSinclair1 = new RadioButton("Sinclair 1 (Keys 6, 7, 8, 9, 0)");
        RadioButton rbSinclair2 = new RadioButton("Sinclair 2 (Keys 1, 2, 3, 4, 5)");
        RadioButton rbCursor = new RadioButton("Cursor / Protek (Keys 5, 8, 6, 7, 0)");

        rbKempston.setToggleGroup(ifaceGroup);
        rbSinclair1.setToggleGroup(ifaceGroup);
        rbSinclair2.setToggleGroup(ifaceGroup);
        rbCursor.setToggleGroup(ifaceGroup);

        String rbStyle = "-fx-text-fill: #E0E0E0;";
        rbKempston.setStyle(rbStyle);
        rbSinclair1.setStyle(rbStyle);
        rbSinclair2.setStyle(rbStyle);
        rbCursor.setStyle(rbStyle);

        switch (joystick.getType()) {
            case KEMPSTON -> rbKempston.setSelected(true);
            case SINCLAIR_1 -> rbSinclair1.setSelected(true);
            case SINCLAIR_2 -> rbSinclair2.setSelected(true);
            case CURSOR -> rbCursor.setSelected(true);
        }

        rbKempston.setOnAction(e -> {
            joystick.setType(Joystick.JoystickType.KEMPSTON);
            if (mainJoyView != null) mainJoyView.updateInfo("Mode: KEMPSTON");
        });
        rbSinclair1.setOnAction(e -> {
            joystick.setType(Joystick.JoystickType.SINCLAIR_1);
            if (mainJoyView != null) mainJoyView.updateInfo("Mode: SINCLAIR 1");
        });
        rbSinclair2.setOnAction(e -> {
            joystick.setType(Joystick.JoystickType.SINCLAIR_2);
            if (mainJoyView != null) mainJoyView.updateInfo("Mode: SINCLAIR 2");
        });
        rbCursor.setOnAction(e -> {
            joystick.setType(Joystick.JoystickType.CURSOR);
            if (mainJoyView != null) mainJoyView.updateInfo("Mode: CURSOR");
        });

        Label ifaceDesc = new Label("Kempston is a dedicated digital I/O port. Sinclair and Cursor share keyboard matrix lines.");
        ifaceDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
        ifaceDesc.setWrapText(true);

        interfaceBox.getChildren().addAll(ifaceHeader, rbKempston, rbSinclair1, rbSinclair2, rbCursor, ifaceDesc);

        // 2. Keyboard Mapping Profile Selection
        VBox profileBox = new VBox(6);
        Label profHeader = new Label("Host Keyboard Profile:");
        profHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #00E5FF; -fx-font-size: 12px;");

        ComboBox<KeyboardMapper.HostJoystickProfile> profileCombo = new ComboBox<>();
        profileCombo.getItems().addAll(KeyboardMapper.HostJoystickProfile.values());
        profileCombo.setValue(keyboardMapper.getProfile());
        profileCombo.setMaxWidth(Double.MAX_VALUE);
        profileCombo.setOnAction(e -> keyboardMapper.setProfile(profileCombo.getValue()));

        CheckBox chkArrowCursor = new CheckBox("Map Arrow Keys to Sinclair Cursor (Caps Shift + 5, 6, 7, 8)");
        chkArrowCursor.setSelected(keyboardMapper.isMapArrowsToCursorKeys());
        chkArrowCursor.setStyle(rbStyle);
        chkArrowCursor.setOnAction(e -> keyboardMapper.setMapArrowsToCursorKeys(chkArrowCursor.isSelected()));

        profileBox.getChildren().addAll(profHeader, profileCombo, chkArrowCursor);

        // 3. Live Interactive Input Tester
        VBox testerBox = new VBox(6);
        Label testerHeader = new Label("Live Joystick Input Tester:");
        testerHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFD700; -fx-font-size: 12px;");
        Label testerHint = new Label("Press your configured movement/fire keys or drag the stick below to test:");
        testerHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #AAAAAA;");

        OnScreenJoystickView testerView = new OnScreenJoystickView(joystick);
        testerView.setStyle("-fx-background-color: #16161A; -fx-border-color: #444450; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Label stateStatus = new Label("State: Centered | Fire: Released");
        stateStatus.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #A0FFA0;");

        testerBox.getChildren().addAll(testerHeader, testerHint, testerView, stateStatus);

        content.getChildren().addAll(interfaceBox, new Separator(), profileBox, new Separator(), testerBox);
        pane.setContent(content);

        // Animation timer to update the live tester within the dialog
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                testerView.updateState();
                StringBuilder sb = new StringBuilder("Dir: ");
                boolean anyDir = false;
                if (joystick.isUp())    { sb.append("UP "); anyDir = true; }
                if (joystick.isDown())  { sb.append("DOWN "); anyDir = true; }
                if (joystick.isLeft())  { sb.append("LEFT "); anyDir = true; }
                if (joystick.isRight()) { sb.append("RIGHT "); anyDir = true; }
                if (!anyDir) sb.append("CENTER ");
                sb.append("| Fire: ").append(joystick.isFire() ? "PRESSED (0x10)" : "Released");
                if (joystick.getType() == Joystick.JoystickType.KEMPSTON) {
                    sb.append(String.format(" | Port 0x1F: 0x%02X", joystick.readKempston()));
                }
                stateStatus.setText(sb.toString());
            }
        };
        timer.start();

        setOnHidden(e -> timer.stop());
    }
}
