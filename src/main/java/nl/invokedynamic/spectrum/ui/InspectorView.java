package nl.invokedynamic.spectrum.ui;

import nl.invokedynamic.spectrum.cpu.Disassembler;
import nl.invokedynamic.spectrum.cpu.Flags;
import nl.invokedynamic.spectrum.cpu.Registers;
import nl.invokedynamic.spectrum.machine.SpectrumMachine;
import nl.invokedynamic.spectrum.sound.Ay38912;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Live retro TUI inspector panel for ZX Spectrum internals (Z80 CPU, 128K Memory Paging, AY Sound).
 * Inspired by Lazygit sub-tabs (CPU, Memory, Sound, All).
 */
public final class InspectorView extends VBox {

    /**
     * Diagnostic inspection categories available in the inspector panel.
     */
    public enum InspectorTab {
        CPU("CPU"),
        MEMORY("Memory"),
        SOUND("Sound"),
        ALL("All");

        private final String label;
        InspectorTab(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final SpectrumMachine machine;
    private InspectorTab activeTab = InspectorTab.CPU;

    // Header & Tabs
    private final HBox tabHeaderBox;
    private final Label[] tabLabels;

    // View Containers
    private final StackPane contentStack;
    private final VBox cpuView;
    private final VBox memoryView;
    private final VBox soundView;
    private final ScrollPane allScrollPane;
    private final VBox allView;

    // --- CPU UI Controls ---
    private final Label cpuStatusLabel = new Label("RUNNING");
    private final Button btnStep = new Button("⏭ Step");
    private final Button btnStepFrame = new Button("⏩ Frame");
    private final Label lblAf = new Label();
    private final Label lblBc = new Label();
    private final Label lblDe = new Label();
    private final Label lblHl = new Label();
    private final Label lblAfAlt = new Label();
    private final Label lblBcAlt = new Label();
    private final Label lblDeAlt = new Label();
    private final Label lblHlAlt = new Label();
    private final Label lblIxIy = new Label();
    private final Label lblPcSp = new Label();
    private final Label lblIR = new Label();
    private final Label[] flagBadges = new Label[8];
    private static final String[] FLAG_NAMES = {"S", "Z", "5", "H", "3", "P", "N", "C"};
    private final VBox disasmBox = new VBox(2);

    // --- Memory UI Controls ---
    private final Label lblSlot0 = new Label();
    private final Label lblSlot1 = new Label();
    private final Label lblSlot2 = new Label();
    private final Label lblSlot3 = new Label();
    private final Label lblPagingLock = new Label();
    private final Label lblScreenBank = new Label();
    private final Label lblVideoAddress = new Label();

    // --- Sound UI Controls ---
    private final Label lblChA = new Label();
    private final Label lblChB = new Label();
    private final Label lblChC = new Label();
    private final Label lblNoiseEnv = new Label();
    private final Label lblBeeper = new Label();

    /**
     * Constructs a new InspectorView tied to the provided machine instance.
     *
     * @param machine the Spectrum machine coordinator
     */
    public InspectorView(SpectrumMachine machine) {
        this.machine = machine;
        getStyleClass().add("retro-panel");
        setSpacing(6);
        setPadding(new Insets(4));
        setPrefWidth(320);
        setMinWidth(280);
        setMaxWidth(420);

        // 1. Panel Header Row (Title + Model Badge)
        tabHeaderBox = new HBox(6);
        tabHeaderBox.setAlignment(Pos.CENTER_LEFT);
        tabHeaderBox.getStyleClass().add("retro-panel-header");

        Label titleLabel = new Label("-[ ⚙ Inspector ]-");
        titleLabel.setStyle("-fx-text-fill: #bd93f9; -fx-font-weight: bold; -fx-font-family: monospace;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badgeLabel = new Label("[ 128K ]");
        badgeLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-text-fill: #bd93f9;");
        tabHeaderBox.getChildren().addAll(titleLabel, spacer, badgeLabel);

        // 2. Segmented Full-Width Subtab Bar (Lazygit-style)
        HBox tabBarBox = new HBox(4);
        tabBarBox.setAlignment(Pos.CENTER);
        tabBarBox.getStyleClass().add("inspector-tab-bar");

        InspectorTab[] tabs = InspectorTab.values();
        tabLabels = new Label[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            final InspectorTab tab = tabs[i];
            Label tabLbl = new Label(tab.getLabel());
            tabLbl.getStyleClass().add("inspector-subtab");
            tabLbl.setMaxWidth(Double.MAX_VALUE);
            tabLbl.setAlignment(Pos.CENTER);
            HBox.setHgrow(tabLbl, Priority.ALWAYS);
            tabLbl.setOnMouseClicked(e -> selectTab(tab));
            tabLabels[i] = tabLbl;
            tabBarBox.getChildren().add(tabLbl);
        }

        // 3. Build Subviews
        cpuView = buildCpuView();
        memoryView = buildMemoryView();
        soundView = buildSoundView();
        allView = new VBox(8);
        allView.setPadding(new Insets(2));
        allScrollPane = new ScrollPane(allView);
        allScrollPane.setFitToWidth(true);
        allScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        contentStack = new StackPane(cpuView, memoryView, soundView, allScrollPane);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        getChildren().addAll(tabHeaderBox, tabBarBox, contentStack);

        selectTab(InspectorTab.CPU);
        updateState();
    }

    /**
     * Switches the active inspector subtab.
     *
     * @param tab the tab category to display
     */
    public void selectTab(InspectorTab tab) {
        this.activeTab = tab;
        InspectorTab[] tabs = InspectorTab.values();
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (tabs[i] == tab);
            tabLabels[i].getStyleClass().remove("inspector-subtab-active");
            if (active) {
                tabLabels[i].getStyleClass().add("inspector-subtab-active");
            }
        }

        cpuView.setVisible(tab == InspectorTab.CPU);
        memoryView.setVisible(tab == InspectorTab.MEMORY);
        soundView.setVisible(tab == InspectorTab.SOUND);
        allScrollPane.setVisible(tab == InspectorTab.ALL);

        if (tab == InspectorTab.ALL) {
            allView.getChildren().clear();
            allView.getChildren().addAll(
                createSectionLabel("── Z80 CPU ──"),
                buildCpuContentBox(),
                createSectionLabel("── 128K MEMORY ──"),
                buildMemoryContentBox(),
                createSectionLabel("── AY-3-8912 PSG ──"),
                buildSoundContentBox()
            );
        }
        updateState();
    }

    private Label createSectionLabel(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #ffb86c; -fx-font-weight: bold; -fx-font-family: monospace; -fx-font-size: 10px;");
        return lbl;
    }

    // =========================================================================
    // CPU View
    // =========================================================================

    private VBox buildCpuView() {
        VBox box = new VBox(6);
        VBox.setVgrow(box, Priority.ALWAYS);

        // Control bar (Step instruction / Step frame when paused)
        HBox ctrlBar = new HBox(6);
        ctrlBar.setAlignment(Pos.CENTER_LEFT);
        btnStep.getStyleClass().add("retro-btn");
        btnStepFrame.getStyleClass().add("retro-btn");
        btnStep.setOnAction(e -> {
            machine.stepSingleInstruction();
            updateState();
        });
        btnStepFrame.setOnAction(e -> {
            machine.stepSingleFrame();
            updateState();
        });

        cpuStatusLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #50fa7b;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        ctrlBar.getChildren().addAll(btnStep, btnStepFrame, sp, cpuStatusLabel);

        box.getChildren().addAll(ctrlBar, buildCpuContentBox());
        return box;
    }

    private VBox buildCpuContentBox() {
        VBox box = new VBox(6);

        // Registers Grid
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(2);

        applyMonoStyle(lblAf, lblBc, lblDe, lblHl, lblAfAlt, lblBcAlt, lblDeAlt, lblHlAlt, lblIxIy, lblPcSp, lblIR);

        grid.add(lblAf, 0, 0);
        grid.add(lblAfAlt, 1, 0);
        grid.add(lblBc, 0, 1);
        grid.add(lblBcAlt, 1, 1);
        grid.add(lblDe, 0, 2);
        grid.add(lblDeAlt, 1, 2);
        grid.add(lblHl, 0, 3);
        grid.add(lblHlAlt, 1, 3);

        VBox regBox = new VBox(2);
        regBox.getChildren().addAll(
            grid,
            lblIxIy,
            lblPcSp,
            lblIR
        );

        // Flags row
        HBox flagsRow = new HBox(4);
        flagsRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 8; i++) {
            Label fLbl = new Label(FLAG_NAMES[i]);
            fLbl.setPadding(new Insets(1, 4, 1, 4));
            fLbl.setStyle("-fx-font-family: monospace; -fx-font-size: 9px; -fx-background-color: #24242c; -fx-text-fill: #666677; -fx-border-color: #383844; -fx-border-width: 1; -fx-border-radius: 2;");
            flagBadges[i] = fLbl;
            flagsRow.getChildren().add(fLbl);
        }

        // Disassembly preview
        Label disasmTitle = new Label("DISASSEMBLY @ PC:");
        disasmTitle.setStyle("-fx-font-family: monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #00e5ff;");
        disasmBox.setStyle("-fx-background-color: #121216; -fx-padding: 4; -fx-border-color: #282834; -fx-border-width: 1; -fx-border-radius: 3;");

        box.getChildren().addAll(regBox, flagsRow, disasmTitle, disasmBox);
        return box;
    }

    // =========================================================================
    // Memory View
    // =========================================================================

    private VBox buildMemoryView() {
        VBox box = new VBox(6);
        box.getChildren().add(buildMemoryContentBox());
        return box;
    }

    private VBox buildMemoryContentBox() {
        VBox box = new VBox(6);
        applyMonoStyle(lblSlot0, lblSlot1, lblSlot2, lblSlot3, lblPagingLock, lblScreenBank, lblVideoAddress);

        VBox mapBox = new VBox(4);
        mapBox.setStyle("-fx-background-color: #141418; -fx-padding: 6; -fx-border-color: #282834; -fx-border-width: 1; -fx-border-radius: 3;");
        Label mapTitle = new Label("128K MEMORY MAP (0x0000 - 0xFFFF):");
        mapTitle.setStyle("-fx-font-family: monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");
        mapBox.getChildren().addAll(mapTitle, lblSlot0, lblSlot1, lblSlot2, lblSlot3);

        VBox statusBox = new VBox(3);
        statusBox.setStyle("-fx-background-color: #141418; -fx-padding: 6; -fx-border-color: #282834; -fx-border-width: 1; -fx-border-radius: 3;");
        Label statusTitle = new Label("PORT 0x7FFD PAGING STATE:");
        statusTitle.setStyle("-fx-font-family: monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");
        statusBox.getChildren().addAll(statusTitle, lblPagingLock, lblScreenBank, lblVideoAddress);

        box.getChildren().addAll(mapBox, statusBox);
        return box;
    }

    // =========================================================================
    // Sound View
    // =========================================================================

    private VBox buildSoundView() {
        VBox box = new VBox(6);
        box.getChildren().add(buildSoundContentBox());
        return box;
    }

    private VBox buildSoundContentBox() {
        VBox box = new VBox(6);
        applyMonoStyle(lblChA, lblChB, lblChC, lblNoiseEnv, lblBeeper);

        VBox ayBox = new VBox(4);
        ayBox.setStyle("-fx-background-color: #141418; -fx-padding: 6; -fx-border-color: #282834; -fx-border-width: 1; -fx-border-radius: 3;");
        Label ayTitle = new Label("AY-3-8912 PSG (3 CHANNELS):");
        ayTitle.setStyle("-fx-font-family: monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #ff79c6;");
        ayBox.getChildren().addAll(ayTitle, lblChA, lblChB, lblChC, lblNoiseEnv);

        VBox beepBox = new VBox(3);
        beepBox.setStyle("-fx-background-color: #141418; -fx-padding: 6; -fx-border-color: #282834; -fx-border-width: 1; -fx-border-radius: 3;");
        Label beepTitle = new Label("1-BIT BEEPER & TAPE EAR:");
        beepTitle.setStyle("-fx-font-family: monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #ff79c6;");
        beepBox.getChildren().addAll(beepTitle, lblBeeper);

        box.getChildren().addAll(ayBox, beepBox);
        return box;
    }

    private void applyMonoStyle(Label... labels) {
        for (Label l : labels) {
            l.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-text-fill: #dcdce2;");
        }
    }

    // =========================================================================
    // State Update (Called every frame or on step)
    // =========================================================================

    public void updateState() {
        if (!isVisible()) return;

        Registers r = machine.getCpu().getState().getRegisters();
        int f = r.getF();

        // Pause / Step state
        boolean isPaused = machine.isPaused();
        btnStep.setDisable(!isPaused);
        btnStepFrame.setDisable(!isPaused);
        cpuStatusLabel.setText(isPaused ? "PAUSED" : "RUNNING");
        cpuStatusLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: " +
            (isPaused ? "#ffd700;" : "#50fa7b;"));

        // Update CPU registers
        lblAf.setText(String.format("AF: %04X", r.getAF()));
        lblBc.setText(String.format("BC: %04X", r.getBC()));
        lblDe.setText(String.format("DE: %04X", r.getDE()));
        lblHl.setText(String.format("HL: %04X", r.getHL()));

        lblAfAlt.setText(String.format("AF': %04X", r.getAFPrime()));
        lblBcAlt.setText(String.format("BC': %04X", r.getBCPrime()));
        lblDeAlt.setText(String.format("DE': %04X", r.getDEPrime()));
        lblHlAlt.setText(String.format("HL': %04X", r.getHLPrime()));

        lblIxIy.setText(String.format("IX: %04X   IY: %04X", r.getIX(), r.getIY()));
        lblPcSp.setText(String.format("PC: %04X   SP: %04X", r.getPC(), r.getSP()));
        lblIR.setText(String.format("I:  %02X     R:  %02X", r.getI(), r.getR()));

        // Update Flags: S Z 5 H 3 P N C
        boolean[] flagStates = {
            (f & Flags.S_MASK) != 0,
            (f & Flags.Z_MASK) != 0,
            (f & Flags.F5_MASK) != 0,
            (f & Flags.H_MASK) != 0,
            (f & Flags.F3_MASK) != 0,
            (f & Flags.PV_MASK) != 0,
            (f & Flags.N_MASK) != 0,
            (f & Flags.C_MASK) != 0
        };

        for (int i = 0; i < 8; i++) {
            boolean active = flagStates[i];
            flagBadges[i].setStyle(active
                ? "-fx-font-family: monospace; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: #1d333e; -fx-text-fill: #50fa7b; -fx-border-color: #50fa7b; -fx-border-width: 1; -fx-border-radius: 2;"
                : "-fx-font-family: monospace; -fx-font-size: 9px; -fx-background-color: #24242c; -fx-text-fill: #555566; -fx-border-color: #383844; -fx-border-width: 1; -fx-border-radius: 2;");
        }

        // Update Disassembly preview (4 instructions starting at PC)
        disasmBox.getChildren().clear();
        List<Disassembler.DisassembledInstruction> disasm = Disassembler.disassemble(machine.getMemory(), r.getPC(), 4);
        for (int i = 0; i < disasm.size(); i++) {
            var item = disasm.get(i);
            Label row = new Label((i == 0 ? "► " : "  ") + item.toString());
            row.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-text-fill: " +
                (i == 0 ? "#00e5ff; -fx-font-weight: bold;" : "#aaaaaf;"));
            disasmBox.getChildren().add(row);
        }

        // Update Memory state
        var mem = machine.getMemory();
        int rom = mem.getActiveRomBank();
        int pagedRam = mem.getActiveRamBank();
        int screen = mem.getScreenBank();
        boolean locked = mem.isPagingLocked();

        lblSlot0.setText(String.format("Slot 0 [0000-3FFF]: ROM %d (%s)", rom, rom == 0 ? "128K Editor" : "48K BASIC"));
        lblSlot1.setText("Slot 1 [4000-7FFF]: RAM 5 (Screen 5)");
        lblSlot2.setText("Slot 2 [8000-BFFF]: RAM 2");
        lblSlot3.setText(String.format("Slot 3 [C000-FFFF]: RAM %d (Paged)", pagedRam));

        lblPagingLock.setText("Paging Lock:  " + (locked ? "LOCKED (Bit 5)" : "UNLOCKED"));
        lblPagingLock.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-text-fill: " +
            (locked ? "#ffd700;" : "#50fa7b;"));

        lblScreenBank.setText(String.format("Active Screen: Bank %d (%s)", screen, screen == 7 ? "Shadow Screen" : "Normal Screen"));
        lblVideoAddress.setText(String.format("Screen Base:   0x%04X", screen == 7 ? 0xC000 : 0x4000));

        // Update AY-3-8912 PSG state
        Ay38912 psg = machine.getPsg();
        lblChA.setText(formatChannel("A", psg.getChannelVolume(0), psg.getChannelPeriod(0), psg.isToneEnabled(0), psg.isNoiseEnabled(0)));
        lblChB.setText(formatChannel("B", psg.getChannelVolume(1), psg.getChannelPeriod(1), psg.isToneEnabled(1), psg.isNoiseEnabled(1)));
        lblChC.setText(formatChannel("C", psg.getChannelVolume(2), psg.getChannelPeriod(2), psg.isToneEnabled(2), psg.isNoiseEnabled(2)));

        lblNoiseEnv.setText(String.format("Noise: 0x%02X | Env: Per=0x%04X Shape=%X",
            psg.getNoisePeriod(), psg.getEnvelopePeriod(), psg.getEnvelopeShape()));

        lblBeeper.setText(String.format("Beeper Out: %s | Tape EAR: %s",
            machine.getBeeper().getSpeakerState() != 0 ? "HIGH" : "LOW",
            machine.getTapePlayer().getEarSignal() ? "PULSING" : "IDLE"));
    }

    private static String formatChannel(String ch, int vol, int period, boolean tone, boolean noise) {
        int bars = Math.min(10, (vol * 10) / 15);
        StringBuilder sb = new StringBuilder();
        sb.append(ch).append(": ");
        for (int i = 0; i < 10; i++) {
            sb.append(i < bars ? "■" : "□");
        }
        sb.append(String.format(" V:%02d P:%04X %s%s",
            vol, period, tone ? "T" : "-", noise ? "N" : "-"));
        return sb.toString();
    }
}
