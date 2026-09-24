package com.spectrum.ui;

import com.spectrum.io.Joystick;
import com.spectrum.machine.MachineModel;
import com.spectrum.machine.SpectrumMachine;
import com.spectrum.storage.SnaSnapshot;
import com.spectrum.storage.TapFileFormat;
import com.spectrum.storage.TapeLibrary;
import com.spectrum.storage.TapeLibraryEntry;
import com.spectrum.storage.Z80Snapshot;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the FXML-based ZX Spectrum UI.
 */
public final class SpectrumController {

    @FXML private BorderPane rootPane;
    @FXML private MenuBar menuBar;

    @FXML private RadioMenuItem m128k;
    @FXML private RadioMenuItem m48k;
    @FXML private CheckMenuItem pauseItem;
    @FXML private CheckMenuItem fastForwardItem;

    @FXML private RadioMenuItem kempstonItem;
    @FXML private RadioMenuItem sinclair1Item;
    @FXML private RadioMenuItem sinclair2Item;
    @FXML private RadioMenuItem cursorItem;
    @FXML private Menu profileMenu;

    @FXML private CheckMenuItem audioEnableItem;

    @FXML private CheckMenuItem showLibraryMenu;
    @FXML private CheckMenuItem showKeyboardMenu;
    @FXML private CheckMenuItem showJoystickMenu;

    @FXML private VBox libraryPanel;
    @FXML private VBox screenPanel;
    @FXML private HBox inputDeckPanel;
    @FXML private VBox keyboardPanel;
    @FXML private VBox joystickPanel;

    @FXML private StackPane screenContainer;
    @FXML private StackPane keyboardContainer;
    @FXML private StackPane joystickContainer;

    @FXML private ListView<TapeLibraryEntry> tapeListView;

    @FXML private Button btnPlay;
    @FXML private Button btnPause;
    @FXML private Button btnStop;
    @FXML private Button btnRewind;
    @FXML private CheckBox chkInstant;

    @FXML private ToggleButton btnToggleLibrary;
    @FXML private ToggleButton btnToggleKeyboard;
    @FXML private ToggleButton btnToggleJoystick;

    @FXML private Label tapeLabel;
    @FXML private Label statusLabel;
    @FXML private Label fpsLabel;

    private SpectrumMachine machine;
    private KeyboardMapper keyboardMapper;
    private ScreenView screenView;
    private KeyboardView keyboardView;
    private OnScreenJoystickView onScreenJoystickView;
    private TapeLibrary tapeLibrary;
    private Stage stage;
    private AnimationTimer loop;

    private final Map<KeyboardMapper.HostJoystickProfile, RadioMenuItem> profileMenuItems = new EnumMap<>(KeyboardMapper.HostJoystickProfile.class);

    private String currentTapeFileName = null;
    private long lastTime = 0;
    private int frames = 0;
    private double currentFps = 50.0;

    @FXML
    public void initialize() {
        this.machine = new SpectrumMachine();
        this.keyboardMapper = new KeyboardMapper(machine.getKeyboard(), machine.getJoystick());
        this.screenView = new ScreenView();
        this.keyboardView = new KeyboardView(machine.getKeyboard());
        this.onScreenJoystickView = new OnScreenJoystickView(machine.getJoystick());
        this.tapeLibrary = new TapeLibrary();

        // Attach custom views to containers
        screenContainer.getChildren().add(screenView);
        keyboardContainer.getChildren().add(keyboardView);
        joystickContainer.getChildren().add(onScreenJoystickView);

        // Host profiles menu setup
        setupProfileMenu();

        // Tape Library ListView setup
        setupTapeListView();

        // Initial synchronization
        syncInputMenuState();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public SpectrumMachine getMachine() {
        return machine;
    }

    public KeyboardMapper getKeyboardMapper() {
        return keyboardMapper;
    }

    public void start() {
        startLoop();
    }

    public void shutdown() {
        if (loop != null) loop.stop();
        if (machine != null) {
            machine.getAudioMixer().close();
        }
    }

    private void setupProfileMenu() {
        ToggleGroup profGroup = new ToggleGroup();
        for (KeyboardMapper.HostJoystickProfile prof : KeyboardMapper.HostJoystickProfile.values()) {
            RadioMenuItem item = new RadioMenuItem(prof.getDisplayName());
            item.setToggleGroup(profGroup);
            if (prof == keyboardMapper.getProfile()) {
                item.setSelected(true);
            }
            item.setOnAction(e -> {
                keyboardMapper.setProfile(prof);
                syncInputMenuState();
            });
            profileMenuItems.put(prof, item);
            profileMenu.getItems().add(item);
        }
    }

    private void setupTapeListView() {
        tapeListView.setItems(tapeLibrary.getEntries());

        tapeListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TapeLibraryEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setAlignment(Pos.CENTER_LEFT);

                    HBox topRow = new HBox(6);
                    Label nameLbl = new Label("💾 " + item.name());
                    nameLbl.getStyleClass().add("tape-entry-name");
                    Label metaLbl = new Label(String.format("[%d blks, %s]", item.blockCount(), item.formattedSize()));
                    metaLbl.getStyleClass().add("tape-entry-meta");
                    topRow.getChildren().addAll(nameLbl, metaLbl);

                    Label pathLbl = new Label(item.filePath());
                    pathLbl.getStyleClass().add("tape-entry-path");
                    if (!item.exists()) {
                        pathLbl.setText(item.filePath() + " (File not found)");
                        pathLbl.setStyle("-fx-text-fill: #ff5555;");
                    }

                    box.getChildren().addAll(topRow, pathLbl);
                    setGraphic(box);

                    setTooltip(new Tooltip("Location: " + item.filePath() + "\nBlocks: " + item.blockCount() + "\nSize: " + item.formattedSize()));
                }
            }
        });

        // Double-click to load tape
        tapeListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TapeLibraryEntry selected = tapeListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadTapeFromEntry(selected);
                }
            }
        });

        // Context menu
        ContextMenu cm = new ContextMenu();
        MenuItem insertItem = new MenuItem("Insert Tape into Spectrum");
        insertItem.setOnAction(e -> {
            TapeLibraryEntry selected = tapeListView.getSelectionModel().getSelectedItem();
            if (selected != null) loadTapeFromEntry(selected);
        });
        MenuItem removeItem = new MenuItem("Remove from Library");
        removeItem.setOnAction(e -> handleRemoveTapeFromLibrary());
        cm.getItems().addAll(insertItem, removeItem);
        tapeListView.setContextMenu(cm);
    }

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

    // =========================================================================
    // Tape Library Actions
    // =========================================================================

    @FXML
    public void handleAddTapeToLibrary() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add Tape to Library (.TAP)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TAP Files (*.tap)", "*.tap"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                List<TapFileFormat.TapBlock> blocks = TapFileFormat.load(file.toPath());
                TapeLibraryEntry entry = tapeLibrary.addOrUpdate(file.toPath(), blocks.size());
                tapeListView.getSelectionModel().select(entry);
            } catch (Exception ex) {
                showError("Failed to add tape to library", ex.getMessage());
            }
        }
    }

    @FXML
    public void handleInsertSelectedTape() {
        TapeLibraryEntry selected = tapeListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            loadTapeFromEntry(selected);
        } else {
            showInfo("No Tape Selected", "Please select a tape from the library first.");
        }
    }

    @FXML
    public void handleRemoveTapeFromLibrary() {
        TapeLibraryEntry selected = tapeListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tapeLibrary.remove(selected);
        }
    }

    public void loadTapeFromEntry(TapeLibraryEntry entry) {
        if (entry == null) return;
        Path path = entry.toPath();
        if (!Files.exists(path)) {
            showError("File Not Found", "The tape file could not be found at:\n" + entry.filePath());
            return;
        }
        try {
            List<TapFileFormat.TapBlock> blocks = TapFileFormat.load(path);
            machine.getTapePlayer().loadTape(blocks);
            currentTapeFileName = entry.name();
            tapeLibrary.addOrUpdate(path, blocks.size());
            tapeLabel.setText(String.format("Tape: %s (%d blks)", currentTapeFileName, blocks.size()));
        } catch (Exception ex) {
            showError("Failed to load tape", ex.getMessage());
        }
    }

    // =========================================================================
    // Menu Actions: File
    // =========================================================================

    @FXML
    public void handleOpenSnapshot() {
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

    @FXML
    public void handleSaveSnapshot() {
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

    @FXML
    public void handleOpenTape() {
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
                tapeLibrary.addOrUpdate(file.toPath(), blocks.size());
            } catch (Exception ex) {
                showError("Failed to load tape", ex.getMessage());
            }
        }
    }

    @FXML
    public void handleLoadRom() {
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

    @FXML
    public void handleExit() {
        shutdown();
        if (stage != null) stage.close();
        Platform.exit();
    }

    // =========================================================================
    // Menu Actions: Machine
    // =========================================================================

    @FXML
    public void handleModel128K() {
        machine.setModel(MachineModel.SPECTRUM_128K);
    }

    @FXML
    public void handleModel48K() {
        machine.setModel(MachineModel.SPECTRUM_48K);
    }

    @FXML
    public void handleReset() {
        machine.reset();
    }

    @FXML
    public void handlePause() {
        machine.setPaused(pauseItem.isSelected());
    }

    @FXML
    public void handleFastForward() {
        machine.setFastForward(fastForwardItem.isSelected());
    }

    // =========================================================================
    // Menu Actions: Input
    // =========================================================================

    @FXML
    public void handleKempstonSelected() {
        machine.getJoystick().setType(Joystick.JoystickType.KEMPSTON);
        syncInputMenuState();
    }

    @FXML
    public void handleSinclair1Selected() {
        machine.getJoystick().setType(Joystick.JoystickType.SINCLAIR_1);
        syncInputMenuState();
    }

    @FXML
    public void handleSinclair2Selected() {
        machine.getJoystick().setType(Joystick.JoystickType.SINCLAIR_2);
        syncInputMenuState();
    }

    @FXML
    public void handleCursorSelected() {
        machine.getJoystick().setType(Joystick.JoystickType.CURSOR);
        syncInputMenuState();
    }

    @FXML
    public void handleJoystickSettings() {
        new JoystickSettingsDialog(stage, machine.getJoystick(), machine.getKeyboard(),
            keyboardMapper, onScreenJoystickView, this::syncInputMenuState).showAndWait();
        syncInputMenuState();
    }

    // =========================================================================
    // Menu Actions: Audio
    // =========================================================================

    @FXML
    public void handleAudioToggle() {
        machine.getAudioMixer().setAudioEnabled(audioEnableItem.isSelected());
    }

    @FXML
    public void handleVolume100() {
        machine.getAudioMixer().setMasterVolume(1.0f);
    }

    @FXML
    public void handleVolume75() {
        machine.getAudioMixer().setMasterVolume(0.75f);
    }

    @FXML
    public void handleVolume50() {
        machine.getAudioMixer().setMasterVolume(0.50f);
    }

    // =========================================================================
    // View Toggles
    // =========================================================================

    @FXML
    public void toggleLibraryFromMenu() {
        boolean show = showLibraryMenu.isSelected();
        setLibraryVisible(show);
    }

    @FXML
    public void toggleLibraryFromButton() {
        boolean show = btnToggleLibrary.isSelected();
        setLibraryVisible(show);
    }

    private void setLibraryVisible(boolean show) {
        libraryPanel.setVisible(show);
        libraryPanel.setManaged(show);
        showLibraryMenu.setSelected(show);
        btnToggleLibrary.setSelected(show);
    }

    @FXML
    public void toggleKeyboardFromMenu() {
        boolean show = showKeyboardMenu.isSelected();
        setKeyboardVisible(show);
    }

    @FXML
    public void toggleKeyboardFromButton() {
        boolean show = btnToggleKeyboard.isSelected();
        setKeyboardVisible(show);
    }

    private void setKeyboardVisible(boolean show) {
        keyboardPanel.setVisible(show);
        keyboardPanel.setManaged(show);
        keyboardView.setVisible(show);
        keyboardView.setManaged(show);
        showKeyboardMenu.setSelected(show);
        btnToggleKeyboard.setSelected(show);
        updateInputDeckVisibility();
    }

    @FXML
    public void toggleJoystickFromMenu() {
        boolean show = showJoystickMenu.isSelected();
        setJoystickVisible(show);
    }

    @FXML
    public void toggleJoystickFromButton() {
        boolean show = btnToggleJoystick.isSelected();
        setJoystickVisible(show);
    }

    private void setJoystickVisible(boolean show) {
        joystickPanel.setVisible(show);
        joystickPanel.setManaged(show);
        onScreenJoystickView.setVisible(show);
        onScreenJoystickView.setManaged(show);
        showJoystickMenu.setSelected(show);
        btnToggleJoystick.setSelected(show);
        updateInputDeckVisibility();
    }

    private void updateInputDeckVisibility() {
        boolean anyVisible = keyboardPanel.isVisible() || joystickPanel.isVisible();
        inputDeckPanel.setVisible(anyVisible);
        inputDeckPanel.setManaged(anyVisible);
    }

    // =========================================================================
    // Tape Transport Controls
    // =========================================================================

    @FXML
    public void handlePlay() {
        machine.getTapePlayer().play();
    }

    @FXML
    public void handleTapePause() {
        machine.getTapePlayer().pause();
    }

    @FXML
    public void handleTapeStop() {
        machine.getTapePlayer().stop();
    }

    @FXML
    public void handleTapeRewind() {
        machine.getTapePlayer().rewind();
    }

    @FXML
    public void handleInstantLoadToggle() {
        machine.getTapePlayer().setInstantLoadingEnabled(chkInstant.isSelected());
    }

    // =========================================================================
    // Help & Information
    // =========================================================================

    @FXML
    public void handleKeyboardHelp() {
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

    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About ZX Spectrum 128K");
        alert.setHeaderText("ZX Spectrum 128K Emulator");
        alert.setContentText("""
            Cycle-accurate ZX Spectrum 128K / 48K Emulator.
            Built with Java 26, JavaFX FXML, and Sealed Record CPU Architecture.
            
            Features:
            - Full Z80 instruction set with 100% undocumented opcodes
            - 128K Memory Paging (Port 0x7FFD)
            - AY-3-8912 Sound Generator & 1-bit Beeper
            - Cycle-exact EAR/MIC tape audio and Instant ROM trap loader
            - Tape Library catalog with automatic persistence
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    // =========================================================================
    // Main Emulation Loop
    // =========================================================================

    private void startLoop() {
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Step one frame (70908 T-states on 128K)
                machine.stepFrame();

                // Render frame buffer
                screenView.render(machine.getUla().getFrameBuffer());

                // Update on-screen keyboard live highlights
                if (keyboardView.isVisible()) {
                    keyboardView.updateState();
                }

                // Update on-screen joystick deflection
                if (onScreenJoystickView != null && onScreenJoystickView.isVisible()) {
                    onScreenJoystickView.updateState();
                }

                // FPS & Status metrics
                frames++;
                if (now - lastTime >= 1_000_000_000L) {
                    currentFps = frames * 1_000_000_000.0 / (now - lastTime);
                    fpsLabel.setText(String.format(java.util.Locale.ROOT, "%.1f FPS", currentFps));
                    frames = 0;
                    lastTime = now;

                    // Update status bar
                    var mem = machine.getMemory();
                    statusLabel.setText(String.format("RAM: %d | ROM: %d | Screen: %d",
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
}
