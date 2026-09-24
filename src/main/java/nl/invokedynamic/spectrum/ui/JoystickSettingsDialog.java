package nl.invokedynamic.spectrum.ui;

import nl.invokedynamic.spectrum.io.Joystick;
import nl.invokedynamic.spectrum.io.Keyboard;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Interactive configuration and testing dialog for ZX Spectrum Joysticks.
 * Synchronizes real-time interface and keyboard profile settings with the main emulator.
 */
public final class JoystickSettingsDialog extends Dialog<Void> {

    public JoystickSettingsDialog(Window owner, Joystick joystick, Keyboard keyboard,
                                  KeyboardMapper keyboardMapper, OnScreenJoystickView mainJoyView,
                                  Runnable onSettingsChanged) {
        initOwner(owner);
        setTitle("ZX Spectrum Joystick Settings & Tester");
        setHeaderText("Configure Joystick Interface & Host Keyboard Profile");

        DialogPane pane = getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setStyle("-fx-background-color: #24242A;");

        VBox content = new VBox(14);
        content.setPadding(new Insets(12));
        content.setPrefWidth(580);

        // Architecture Guide Banner
        VBox guideBox = new VBox(4);
        guideBox.setPadding(new Insets(8, 12, 8, 12));
        guideBox.setStyle("-fx-background-color: #1A1A22; -fx-border-color: #383848; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Label guideTitle = new Label("💡 How Joystick Simulation Works:");
        guideTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFD700; -fx-font-size: 11px;");

        Label guideText = new Label("""
            • Spectrum Interface: Sets what hardware the game reads (Kempston Port 0x1F, Sinclair 1/2, or Cursor).
            • Host Keyboard Profile: Sets which PC keys steer the joystick (Arrow keys, WASD, Numpad, or Native Keys).
            • In Sinclair 1/2 or Cursor mode, the native number keys (1-5 or 6-0) ALSO drive the joystick directly!
            • You can steer using your host keyboard AND drag the virtual joystick with your mouse at the same time.
            """);
        guideText.setStyle("-fx-font-size: 10px; -fx-text-fill: #C0C0C8;");
        guideText.setWrapText(true);
        guideBox.getChildren().addAll(guideTitle, guideText);

        // 1. Spectrum Interface Selection
        VBox interfaceBox = new VBox(6);
        Label ifaceHeader = new Label("1. Emulated Spectrum Joystick Interface:");
        ifaceHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #00E5FF; -fx-font-size: 12px;");

        ToggleGroup ifaceGroup = new ToggleGroup();
        RadioButton rbKempston = new RadioButton("Kempston Joystick (Port 0x1F) — Recommended (95%+ of games)");
        RadioButton rbSinclair1 = new RadioButton("Sinclair 1 (Keys 6, 7, 8, 9, 0) — Interface II Port 1");
        RadioButton rbSinclair2 = new RadioButton("Sinclair 2 (Keys 1, 2, 3, 4, 5) — Interface II Port 2");
        RadioButton rbCursor = new RadioButton("Cursor / Protek (Keys 5, 8, 6, 7, 0) — Cursor Keys");

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
            if (onSettingsChanged != null) onSettingsChanged.run();
        });
        rbSinclair1.setOnAction(e -> {
            joystick.setType(Joystick.JoystickType.SINCLAIR_1);
            if (mainJoyView != null) mainJoyView.updateInfo("Mode: SINCLAIR 1");
            if (onSettingsChanged != null) onSettingsChanged.run();
        });
        rbSinclair2.setOnAction(e -> {
            joystick.setType(Joystick.JoystickType.SINCLAIR_2);
            if (mainJoyView != null) mainJoyView.updateInfo("Mode: SINCLAIR 2");
            if (onSettingsChanged != null) onSettingsChanged.run();
        });
        rbCursor.setOnAction(e -> {
            joystick.setType(Joystick.JoystickType.CURSOR);
            if (mainJoyView != null) mainJoyView.updateInfo("Mode: CURSOR");
            if (onSettingsChanged != null) onSettingsChanged.run();
        });

        interfaceBox.getChildren().addAll(ifaceHeader, rbKempston, rbSinclair1, rbSinclair2, rbCursor);

        // 2. Keyboard Mapping Profile Selection
        VBox profileBox = new VBox(6);
        Label profHeader = new Label("2. Host Keyboard Profile (Controls on your PC):");
        profHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #00E5FF; -fx-font-size: 12px;");

        ComboBox<KeyboardMapper.HostJoystickProfile> profileCombo = new ComboBox<>();
        profileCombo.getItems().addAll(KeyboardMapper.HostJoystickProfile.values());
        profileCombo.setValue(keyboardMapper.getProfile());
        profileCombo.setMaxWidth(Double.MAX_VALUE);
        profileCombo.setOnAction(e -> {
            keyboardMapper.setProfile(profileCombo.getValue());
            if (onSettingsChanged != null) onSettingsChanged.run();
        });

        CheckBox chkArrowCursor = new CheckBox("Map Arrow Keys to Sinclair Cursor (Caps Shift + 5, 6, 7, 8)");
        chkArrowCursor.setSelected(keyboardMapper.isMapArrowsToCursorKeys());
        chkArrowCursor.setStyle(rbStyle);
        chkArrowCursor.setOnAction(e -> keyboardMapper.setMapArrowsToCursorKeys(chkArrowCursor.isSelected()));

        profileBox.getChildren().addAll(profHeader, profileCombo, chkArrowCursor);

        // 3. Live Interactive Input Tester
        VBox testerBox = new VBox(8);
        Label testerHeader = new Label("3. Live Joystick & Hardware Tester:");
        testerHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFD700; -fx-font-size: 12px;");

        Label testerHint = new Label("Press configured host keys (Arrows/WASD/Digits) or drag the arcade stick below:");
        testerHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #AAAAAA;");

        OnScreenJoystickView testerView = new OnScreenJoystickView(joystick);
        testerView.setStyle("-fx-background-color: #16161A; -fx-border-color: #444450; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        // Live Joystick State Label
        Label joyStateLabel = new Label("Joystick: CENTER | Fire: Released");
        joyStateLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #A0FFA0;");

        // Live Hardware Output Section
        VBox hwBox = new VBox(4);
        hwBox.setPadding(new Insets(6, 10, 6, 10));
        hwBox.setStyle("-fx-background-color: #1A1A22; -fx-border-color: #383848; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        Label hwHeader = new Label("Active Spectrum Hardware Output:");
        hwHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #E0E0E0;");

        HBox badgesBox = new HBox(8);
        badgesBox.setAlignment(Pos.CENTER_LEFT);

        Label[] badges = new Label[5];
        for (int i = 0; i < 5; i++) {
            badges[i] = new Label();
            badges[i].setPadding(new Insets(3, 8, 3, 8));
            badges[i].setStyle("-fx-background-color: #2A2A35; -fx-text-fill: #777788; -fx-font-family: monospace; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-border-color: #444455; -fx-border-radius: 4px;");
        }
        badgesBox.getChildren().addAll(badges);
        hwBox.getChildren().addAll(hwHeader, badgesBox);

        testerBox.getChildren().addAll(testerHeader, testerHint, testerView, joyStateLabel, hwBox);

        content.getChildren().addAll(guideBox, interfaceBox, new Separator(), profileBox, new Separator(), testerBox);
        pane.setContent(content);

        // Key event filtering: capture host keys in dialog so tester responds immediately
        pane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            keyboardMapper.handleKeyPressed(event);
            if (isTesterKey(keyboardMapper, event.getCode())) {
                event.consume();
            }
        });

        pane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            keyboardMapper.handleKeyReleased(event);
            if (isTesterKey(keyboardMapper, event.getCode())) {
                event.consume();
            }
        });

        // Animation timer to update live feedback
        String activeBadgeStyle = "-fx-background-color: #00E5FF; -fx-text-fill: #000000; -fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 4px;";
        String inactiveBadgeStyle = "-fx-background-color: #2A2A35; -fx-text-fill: #777788; -fx-font-family: monospace; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-border-color: #444455; -fx-border-radius: 4px;";

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                testerView.updateState();

                // 1. Virtual Joystick State
                StringBuilder sb = new StringBuilder("Joystick: ");
                boolean anyDir = false;
                if (joystick.isUp())    { sb.append("UP "); anyDir = true; }
                if (joystick.isDown())  { sb.append("DOWN "); anyDir = true; }
                if (joystick.isLeft())  { sb.append("LEFT "); anyDir = true; }
                if (joystick.isRight()) { sb.append("RIGHT "); anyDir = true; }
                if (!anyDir) sb.append("CENTER ");
                sb.append("| Fire: ").append(joystick.isFire() ? "PRESSED" : "Released");
                joyStateLabel.setText(sb.toString());

                // 2. Hardware Output Badges
                switch (joystick.getType()) {
                    case KEMPSTON -> {
                        int portVal = joystick.readKempston();
                        hwHeader.setText(String.format("Port 0x1F Output: 0x%02X (Binary: %s)",
                                portVal, String.format("%5s", Integer.toBinaryString(portVal)).replace(' ', '0')));
                        updateBadge(badges[0], "RIGHT", (portVal & 0x01) != 0, activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[1], "LEFT",  (portVal & 0x02) != 0, activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[2], "DOWN",  (portVal & 0x04) != 0, activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[3], "UP",    (portVal & 0x08) != 0, activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[4], "FIRE",  (portVal & 0x10) != 0, activeBadgeStyle, inactiveBadgeStyle);
                    }
                    case SINCLAIR_1 -> {
                        hwHeader.setText("Spectrum Keyboard Matrix: Half-Row 4 (0xEF00)");
                        updateBadge(badges[0], "Key 6: LEFT",  keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 4), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[1], "Key 7: RIGHT", keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 3), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[2], "Key 8: DOWN",  keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 2), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[3], "Key 9: UP",    keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 1), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[4], "Key 0: FIRE",  keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0), activeBadgeStyle, inactiveBadgeStyle);
                    }
                    case SINCLAIR_2 -> {
                        hwHeader.setText("Spectrum Keyboard Matrix: Half-Row 3 (0xF700)");
                        updateBadge(badges[0], "Key 1: LEFT",  keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 0), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[1], "Key 2: RIGHT", keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 1), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[2], "Key 3: DOWN",  keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 2), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[3], "Key 4: UP",    keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 3), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[4], "Key 5: FIRE",  keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 4), activeBadgeStyle, inactiveBadgeStyle);
                    }
                    case CURSOR -> {
                        hwHeader.setText("Spectrum Keyboard Matrix: Cursor Keys");
                        updateBadge(badges[0], "Key 5: LEFT",  keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 4), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[1], "Key 8: RIGHT", keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 2), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[2], "Key 6: DOWN",  keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 4), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[3], "Key 7: UP",    keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 3), activeBadgeStyle, inactiveBadgeStyle);
                        updateBadge(badges[4], "Key 0: FIRE",  keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0), activeBadgeStyle, inactiveBadgeStyle);
                    }
                }
            }
        };
        timer.start();

        setOnHidden(e -> {
            timer.stop();
            joystick.reset();
            keyboard.reset();
            if (onSettingsChanged != null) onSettingsChanged.run();
        });
    }

    private static void updateBadge(Label badge, String text, boolean active, String activeStyle, String inactiveStyle) {
        badge.setText(text);
        badge.setStyle(active ? activeStyle : inactiveStyle);
    }

    private static boolean isTesterKey(KeyboardMapper mapper, KeyCode code) {
        if (mapper.isJoystickKey(code)) return true;
        return switch (code) {
            case UP, DOWN, LEFT, RIGHT, SPACE, CONTROL,
                 W, A, S, D, J, K,
                 NUMPAD8, NUMPAD2, NUMPAD4, NUMPAD6, NUMPAD0,
                 DIGIT0, DIGIT1, DIGIT2, DIGIT3, DIGIT4, DIGIT5, DIGIT6, DIGIT7, DIGIT8, DIGIT9 -> true;
            default -> false;
        };
    }
}
