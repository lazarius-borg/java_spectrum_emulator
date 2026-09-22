package com.spectrum.ui;

import com.spectrum.io.Joystick;
import com.spectrum.machine.MachineModel;
import com.spectrum.machine.SpectrumMachine;
import com.spectrum.storage.SnaSnapshot;
import com.spectrum.storage.TapFileFormat;
import com.spectrum.storage.Z80Snapshot;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * Main JavaFX application for the ZX Spectrum 128K emulator.
 */
public final class SpectrumApp extends Application {
    private SpectrumMachine machine;
    private KeyboardMapper keyboardMapper;
    private ScreenView screenView;
    private KeyboardView keyboardView;
    private OnScreenJoystickView onScreenJoystickView;
    private AnimationTimer loop;

    private Label statusLabel;
    private Label fpsLabel;
    private Label tapeLabel;
    private CheckMenuItem showKeyboardMenu;
    private CheckMenuItem showJoystickMenu;
    private ToggleButton btnToggleKeyboard;
    private ToggleButton btnToggleJoystick;
    private String currentTapeFileName = null;

    private long lastTime = 0;
    private int frames = 0;
    private double currentFps = 50.0;

    @Override
    public void start(Stage stage) {
        this.machine = new SpectrumMachine();
        this.keyboardMapper = new KeyboardMapper(machine.getKeyboard(), machine.getJoystick());
        this.screenView = new ScreenView();
        this.keyboardView = new KeyboardView(machine.getKeyboard());
        this.onScreenJoystickView = new OnScreenJoystickView(machine.getJoystick());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #222222;");

        // 1. Menu Bar
        MenuBar menuBar = createMenuBar(stage);
        root.setTop(menuBar);

        // 2. Center: Screen View + On-Screen Joystick + Keyboard
        VBox centerBox = new VBox(0);
        centerBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(screenView, Priority.ALWAYS);
        centerBox.getChildren().addAll(screenView, onScreenJoystickView, keyboardView);
        root.setCenter(centerBox);

        // 3. Bottom: Tape Deck Toolbar & Status Bar
        HBox bottomPanel = createBottomPanel(stage);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 760, 780);

        // Forward keyboard events to the Spectrum
        scene.setOnKeyPressed(keyboardMapper::handleKeyPressed);
        scene.setOnKeyReleased(keyboardMapper::handleKeyReleased);

        stage.setTitle("ZX Spectrum 128K Emulator");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (loop != null) loop.stop();
            machine.getAudioMixer().close();
            Platform.exit();
        });

        stage.show();

        startLoop();
    }

    private RadioMenuItem kempstonItem;
    private RadioMenuItem sinclair1Item;
    private RadioMenuItem sinclair2Item;
    private RadioMenuItem cursorItem;
    private final java.util.Map<KeyboardMapper.HostJoystickProfile, RadioMenuItem> profileMenuItems = new java.util.EnumMap<>(KeyboardMapper.HostJoystickProfile.class);

    public void syncInputMenuState() {
        if (kempstonItem == null) return;
        switch (machine.getJoystick().getType()) {
            case KEMPSTON -> kempstonItem.setSelected(true);
            case SINCLAIR_1 -> sinclair1Item.setSelected(true);
            case SINCLAIR_2 -> sinclair2Item.setSelected(true);
            case CURSOR -> cursorItem.setSelected(true);
        }
        RadioMenuItem profItem = profileMenuItems.get(keyboardMapper.getProfile());
        if (profItem != null) profItem.setSelected(true);
        if (onScreenJoystickView != null) {
            onScreenJoystickView.updateInfo("Mode: " + machine.getJoystick().getType().name());
        }
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar bar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem openSnapshot = new MenuItem("Open Snapshot (.SNA, .Z80)...");
        openSnapshot.setOnAction(e -> handleOpenSnapshot(stage));

        MenuItem saveSnapshot = new MenuItem("Save Snapshot (.SNA)...");
        saveSnapshot.setOnAction(e -> handleSaveSnapshot(stage));

        MenuItem openTape = new MenuItem("Insert Tape (.TAP)...");
        openTape.setOnAction(e -> handleOpenTape(stage));

        MenuItem loadRom = new MenuItem("Load ROM File...");
        loadRom.setOnAction(e -> handleLoadRom(stage));

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            if (loop != null) loop.stop();
            machine.getAudioMixer().close();
            stage.close();
            Platform.exit();
        });

        fileMenu.getItems().addAll(openSnapshot, saveSnapshot, new SeparatorMenuItem(), openTape, loadRom, new SeparatorMenuItem(), exitItem);

        // Machine Menu
        Menu machineMenu = new Menu("Machine");
        ToggleGroup modelGroup = new ToggleGroup();
        RadioMenuItem m128k = new RadioMenuItem("ZX Spectrum 128K");
        RadioMenuItem m48k = new RadioMenuItem("ZX Spectrum 48K");
        m128k.setToggleGroup(modelGroup);
        m48k.setToggleGroup(modelGroup);
        m128k.setSelected(true);

        m128k.setOnAction(e -> machine.setModel(MachineModel.SPECTRUM_128K));
        m48k.setOnAction(e -> machine.setModel(MachineModel.SPECTRUM_48K));

        MenuItem resetItem = new MenuItem("Reset");
        resetItem.setOnAction(e -> machine.reset());

        CheckMenuItem pauseItem = new CheckMenuItem("Pause");
        pauseItem.setOnAction(e -> machine.setPaused(pauseItem.isSelected()));

        CheckMenuItem fastForwardItem = new CheckMenuItem("Fast Forward (Turbo)");
        fastForwardItem.setOnAction(e -> machine.setFastForward(fastForwardItem.isSelected()));

        machineMenu.getItems().addAll(m128k, m48k, new SeparatorMenuItem(), resetItem, pauseItem, fastForwardItem);

        // Input Menu
        Menu inputMenu = new Menu("Input");
        ToggleGroup joyGroup = new ToggleGroup();
        kempstonItem = new RadioMenuItem("Kempston Joystick (Port 0x1F)");
        sinclair1Item = new RadioMenuItem("Sinclair 1 (6, 7, 8, 9, 0)");
        sinclair2Item = new RadioMenuItem("Sinclair 2 (1, 2, 3, 4, 5)");
        cursorItem = new RadioMenuItem("Cursor / Protek (5, 6, 7, 8, 0)");
        kempstonItem.setToggleGroup(joyGroup);
        sinclair1Item.setToggleGroup(joyGroup);
        sinclair2Item.setToggleGroup(joyGroup);
        cursorItem.setToggleGroup(joyGroup);
        kempstonItem.setSelected(true);

        kempstonItem.setOnAction(e -> {
            machine.getJoystick().setType(Joystick.JoystickType.KEMPSTON);
            syncInputMenuState();
        });
        sinclair1Item.setOnAction(e -> {
            machine.getJoystick().setType(Joystick.JoystickType.SINCLAIR_1);
            syncInputMenuState();
        });
        sinclair2Item.setOnAction(e -> {
            machine.getJoystick().setType(Joystick.JoystickType.SINCLAIR_2);
            syncInputMenuState();
        });
        cursorItem.setOnAction(e -> {
            machine.getJoystick().setType(Joystick.JoystickType.CURSOR);
            syncInputMenuState();
        });

        // Host Keyboard Profile Submenu
        Menu profileMenu = new Menu("Host Keyboard Profile");
        ToggleGroup profGroup = new ToggleGroup();
        for (KeyboardMapper.HostJoystickProfile prof : KeyboardMapper.HostJoystickProfile.values()) {
            RadioMenuItem item = new RadioMenuItem(prof.getDisplayName());
            item.setToggleGroup(profGroup);
            if (prof == keyboardMapper.getProfile()) item.setSelected(true);
            item.setOnAction(ev -> {
                keyboardMapper.setProfile(prof);
                syncInputMenuState();
            });
            profileMenuItems.put(prof, item);
            profileMenu.getItems().add(item);
        }

        MenuItem joySettings = new MenuItem("Joystick Settings & Tester...");
        joySettings.setOnAction(e -> {
            new JoystickSettingsDialog(stage, machine.getJoystick(), machine.getKeyboard(), keyboardMapper, onScreenJoystickView, this::syncInputMenuState).showAndWait();
            syncInputMenuState();
        });

        inputMenu.getItems().addAll(kempstonItem, sinclair1Item, sinclair2Item, cursorItem, new SeparatorMenuItem(), profileMenu, new SeparatorMenuItem(), joySettings);

        // Audio Menu
        Menu audioMenu = new Menu("Audio");
        CheckMenuItem audioEnable = new CheckMenuItem("Enable Audio");
        audioEnable.setSelected(true);
        audioEnable.setOnAction(e -> machine.getAudioMixer().setAudioEnabled(audioEnable.isSelected()));

        MenuItem vol100 = new MenuItem("Volume: 100%");
        vol100.setOnAction(e -> machine.getAudioMixer().setMasterVolume(1.0f));
        MenuItem vol75 = new MenuItem("Volume: 75%");
        vol75.setOnAction(e -> machine.getAudioMixer().setMasterVolume(0.75f));
        MenuItem vol50 = new MenuItem("Volume: 50%");
        vol50.setOnAction(e -> machine.getAudioMixer().setMasterVolume(0.50f));

        audioMenu.getItems().addAll(audioEnable, new SeparatorMenuItem(), vol100, vol75, vol50);

        // View Menu
        Menu viewMenu = new Menu("View");
        showKeyboardMenu = new CheckMenuItem("Show On-Screen Keyboard");
        showKeyboardMenu.setSelected(true);
        showKeyboardMenu.setOnAction(e -> {
            boolean show = showKeyboardMenu.isSelected();
            keyboardView.setVisible(show);
            keyboardView.setManaged(show);
            if (btnToggleKeyboard != null) btnToggleKeyboard.setSelected(show);
        });

        showJoystickMenu = new CheckMenuItem("Show On-Screen Joystick");
        showJoystickMenu.setSelected(true);
        showJoystickMenu.setOnAction(e -> {
            boolean show = showJoystickMenu.isSelected();
            onScreenJoystickView.setVisible(show);
            onScreenJoystickView.setManaged(show);
            if (btnToggleJoystick != null) btnToggleJoystick.setSelected(show);
        });

        viewMenu.getItems().addAll(showKeyboardMenu, showJoystickMenu);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem keyboardHelp = new MenuItem("Keyboard & Joystick Guide...");
        keyboardHelp.setOnAction(e -> showKeyboardHelp());
        MenuItem about = new MenuItem("About ZX Spectrum Emulator");
        about.setOnAction(e -> showAbout());
        helpMenu.getItems().addAll(keyboardHelp, new SeparatorMenuItem(), about);

        bar.getMenus().addAll(fileMenu, machineMenu, inputMenu, audioMenu, viewMenu, helpMenu);
        return bar;
    }

    private HBox createBottomPanel(Stage stage) {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(6, 12, 6, 12));
        panel.setStyle("-fx-background-color: #333333; -fx-text-fill: white;");

        // Tape buttons
        Button btnPlay = new Button("▶ Play");
        Button btnPause = new Button("⏸ Pause");
        Button btnRewind = new Button("⏮ Rewind");
        Button btnStop = new Button("⏹ Stop");

        btnPlay.setOnAction(e -> machine.getTapePlayer().play());
        btnPause.setOnAction(e -> machine.getTapePlayer().pause());
        btnRewind.setOnAction(e -> machine.getTapePlayer().rewind());
        btnStop.setOnAction(e -> machine.getTapePlayer().stop());

        CheckBox chkInstant = new CheckBox("Instant Load");
        chkInstant.setSelected(true);
        chkInstant.setStyle("-fx-text-fill: white;");
        chkInstant.setOnAction(e -> machine.getTapePlayer().setInstantLoadingEnabled(chkInstant.isSelected()));

        btnToggleKeyboard = new ToggleButton("⌨ Keyboard");
        btnToggleKeyboard.setSelected(true);
        btnToggleKeyboard.setOnAction(e -> {
            boolean show = btnToggleKeyboard.isSelected();
            keyboardView.setVisible(show);
            keyboardView.setManaged(show);
            if (showKeyboardMenu != null) showKeyboardMenu.setSelected(show);
        });

        btnToggleJoystick = new ToggleButton("🕹 Joystick");
        btnToggleJoystick.setSelected(true);
        btnToggleJoystick.setOnAction(e -> {
            boolean show = btnToggleJoystick.isSelected();
            onScreenJoystickView.setVisible(show);
            onScreenJoystickView.setManaged(show);
            if (showJoystickMenu != null) showJoystickMenu.setSelected(show);
        });

        tapeLabel = new Label("Tape: None");
        tapeLabel.setStyle("-fx-text-fill: #A0FFA0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("Bank 0 | ROM 0 | Screen 5");
        statusLabel.setStyle("-fx-text-fill: #CCCCCC;");

        fpsLabel = new Label("50.0 FPS");
        fpsLabel.setStyle("-fx-text-fill: #FFD700;");

        panel.getChildren().addAll(
            btnPlay, btnPause, btnStop, btnRewind, chkInstant,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            btnToggleKeyboard, btnToggleJoystick,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            tapeLabel, spacer, statusLabel, fpsLabel
        );
        return panel;
    }

    private void handleOpenSnapshot(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Spectrum Snapshot");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Snapshots (*.sna, *.z80)", "*.sna", "*.z80"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".sna")) {
                    SnaSnapshot.load(file.toPath(), machine.getCpu().getState(), machine.getMemory(), machine.getUla());
                } else if (name.endsWith(".z80")) {
                    Z80Snapshot.load(file.toPath(), machine.getCpu().getState(), machine.getMemory(), machine.getUla(), machine.getPsg());
                }
            } catch (Exception ex) {
                showError("Failed to load snapshot", ex.getMessage());
            }
        }
    }

    private void handleSaveSnapshot(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Spectrum Snapshot");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SNA Snapshot (*.sna)", "*.sna"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                boolean is128k = machine.getModel() == MachineModel.SPECTRUM_128K;
                SnaSnapshot.save(file.toPath(), machine.getCpu().getState(), machine.getMemory(), machine.getUla(), is128k);
            } catch (Exception ex) {
                showError("Failed to save snapshot", ex.getMessage());
            }
        }
    }

    private void handleOpenTape(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Insert Tape (.TAP)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TAP Files (*.tap)", "*.tap"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                List<TapFileFormat.TapBlock> blocks = TapFileFormat.load(file.toPath());
                machine.getTapePlayer().loadTape(blocks);
                currentTapeFileName = file.getName();
                tapeLabel.setText(String.format("Tape: %s (%d blks)", currentTapeFileName, blocks.size()));
            } catch (Exception ex) {
                showError("Failed to load tape", ex.getMessage());
            }
        }
    }

    private void handleLoadRom(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load 16KB/32KB ROM File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ROM Files (*.rom, *.bin)", "*.rom", "*.bin"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                if (data.length >= 32768) {
                    byte[] r0 = new byte[16384];
                    byte[] r1 = new byte[16384];
                    System.arraycopy(data, 0, r0, 0, 16384);
                    System.arraycopy(data, 16384, r1, 0, 16384);
                    machine.getMemory().loadRom(0, r0);
                    machine.getMemory().loadRom(1, r1);
                } else if (data.length >= 16384) {
                    machine.getMemory().loadRom(0, data);
                    machine.getMemory().loadRom(1, data);
                }
                machine.reset();
            } catch (Exception ex) {
                showError("Failed to load ROM", ex.getMessage());
            }
        }
    }

    private void showKeyboardHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ZX Spectrum Keyboard Mapping Reference");
        alert.setHeaderText("Host Keyboard to ZX Spectrum Mapping");

        String helpText = """
            ZX SPECTRUM KEYBOARD MAPPING REFERENCE
            --------------------------------------
            • Arrow Keys (↑, ↓, ←, →):
                Mapped to Sinclair Cursor keys (Caps Shift + 7, 6, 5, 8)
                and Kempston / Cursor Joystick directions.
            
            • Enter / Return:
                Mapped to Spectrum ENTER.
            
            • Backspace / Delete:
                Mapped to Spectrum DELETE (Caps Shift + 0).
            
            • Shift (Left / Right):
                Mapped to CAPS SHIFT.
            
            • Control / Alt / ` (Backtick):
                Mapped to SYMBOL SHIFT.
            
            • Escape:
                Mapped to BREAK (Caps Shift + Space).
            
            • Caps Lock:
                Mapped to CAPS LOCK (Caps Shift + 2).
            
            • Direct Punctuation / Math:
                " (Quote)        -> Symbol Shift + P
                ; (Semicolon)    -> Symbol Shift + O
                , (Comma)        -> Symbol Shift + N
                . (Period)       -> Symbol Shift + M
                / (Slash)        -> Symbol Shift + V
                - (Minus)        -> Symbol Shift + J
                = (Equal)        -> Symbol Shift + L
                + (Plus)         -> Symbol Shift + K
                * (Asterisk)     -> Symbol Shift + B
            
            • Interactive On-Screen Keyboard:
                Click any key below the screen with your mouse!
                Click CAPS SHIFT or SYM SHIFT to toggle/latch shift mode.
                Active keys highlight in bright cyan when pressed on either
                your host keyboard or via mouse.

            JOYSTICK SIMULATION REFERENCE
            -----------------------------
            • How Joystick Simulation Works:
              There are two separate concepts working in harmony:
              1. Spectrum Joystick Interface (What the emulated game reads):
                 - Kempston (Port 0x1F): Dedicated port; zero typing conflicts;
                   supported by 95%+ of games (RECOMMENDED).
                 - Sinclair 1: Simulates Interface 2 Port 1 (keys 6, 7, 8, 9, 0).
                 - Sinclair 2: Simulates Interface 2 Port 2 (keys 1, 2, 3, 4, 5).
                 - Cursor / Protek: Simulates Cursor joystick (keys 5, 8, 6, 7, 0).

              2. Host Controller Input (How YOU steer on your PC):
                 - Host Keyboard Profile (Arrows, WASD, Numpad, or Native Keys).
                 - Virtual Arcade Stick (mouse/trackpad drag & Fire button).
                 - In Sinclair 1/2 or Cursor mode, the native number keys (1-5 or
                   6-0) ALSO steer the joystick directly on your PC!

            • Playing Games:
              1. In the emulator's 'Input' menu, select the interface matching what
                 you choose in the game's menu (e.g. Kempston or Sinclair 2).
              2. Steer using your host Arrow keys, WASD, or the on-screen stick!

            • Live Joystick Settings & Tester:
              Open 'Input -> Joystick Settings & Tester...' to configure profiles
              and verify directional and button inputs in real time.
            """;

        TextArea area = new TextArea(helpText);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(540, 440);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About ZX Spectrum 128K");
        alert.setHeaderText("ZX Spectrum 128K Emulator");
        alert.setContentText("""
            Cycle-accurate ZX Spectrum 128K / 48K Emulator.
            Built with Java 26, JavaFX, and Sealed Record CPU Architecture.
            
            Features:
            - Full Z80 instruction set with 100% undocumented opcodes
            - 128K Memory Paging (Port 0x7FFD)
            - AY-3-8912 Sound Generator & 1-bit Beeper
            - Cycle-exact EAR/MIC tape audio and Instant ROM trap loader
            - Interactive On-Screen Keyboard with live matrix feedback
            - Multi-mode Joystick Simulation (Kempston, Sinclair 1/2, Cursor)
            - On-Screen Virtual Arcade Stick and configurable Host Keyboard Profiles
            """);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    private void startLoop() {
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Step one frame (70908 T-states on 128K)
                machine.stepFrame();

                // Render frame
                screenView.render(machine.getUla().getFrameBuffer());

                // Update on-screen keyboard live highlights
                if (keyboardView.isVisible()) {
                    keyboardView.updateState();
                }

                // Update on-screen joystick live state
                if (onScreenJoystickView != null && onScreenJoystickView.isVisible()) {
                    onScreenJoystickView.updateState();
                }

                // FPS & Status metrics
                frames++;
                if (now - lastTime >= 1_000_000_000L) {
                    currentFps = frames * 1_000_000_000.0 / (now - lastTime);
                    fpsLabel.setText(String.format("%.1f FPS", currentFps));
                    frames = 0;
                    lastTime = now;

                    // Update status bar
                    var mem = machine.getMemory();
                    statusLabel.setText(String.format("RAM Bank: %d | ROM: %d | Screen: %d",
                        mem.getActiveRamBank(), mem.getActiveRomBank(), mem.getScreenBank()));

                    var tape = machine.getTapePlayer();
                    if (tape.getBlocks().isEmpty()) {
                        tapeLabel.setText("Tape: Empty");
                    } else {
                        String name = currentTapeFileName != null ? currentTapeFileName : "Loaded";
                        tapeLabel.setText(String.format("Tape: %s [%s %d/%d]",
                            name, tape.getState(), tape.getCurrentBlockIndex() + 1, tape.getBlocks().size()));
                    }
                }
            }
        };
        loop.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
