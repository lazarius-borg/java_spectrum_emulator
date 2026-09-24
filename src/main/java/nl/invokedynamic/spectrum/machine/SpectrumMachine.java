package nl.invokedynamic.spectrum.machine;

import nl.invokedynamic.spectrum.cpu.Z80Cpu;
import nl.invokedynamic.spectrum.io.Joystick;
import nl.invokedynamic.spectrum.io.Keyboard;
import nl.invokedynamic.spectrum.io.SpectrumIoBus;
import nl.invokedynamic.spectrum.memory.Spectrum128Memory;
import nl.invokedynamic.spectrum.sound.AudioMixer;
import nl.invokedynamic.spectrum.sound.Ay38912;
import nl.invokedynamic.spectrum.sound.Beeper;
import nl.invokedynamic.spectrum.storage.TapePlayer;
import nl.invokedynamic.spectrum.ula.UlaDisplay;

/**
 * Main machine coordinator representing the ZX Spectrum 128K / 48K.
 * Coordinates the CPU, memory, video, sound, I/O, and tape subsystems.
 */
public final class SpectrumMachine {
    private final Z80Cpu cpu;
    private final Spectrum128Memory memory;
    private final UlaDisplay ula;
    private final Beeper beeper;
    private final Ay38912 psg;
    private final AudioMixer audioMixer;
    private final Keyboard keyboard;
    private final Joystick joystick;
    private final TapePlayer tapePlayer;
    private final SpectrumIoBus ioBus;

    private MachineModel model = MachineModel.SPECTRUM_128K;
    private boolean paused = false;
    private boolean fastForward = false;

    // Cycle tracking for audio sampling: 1 sample every ~80.42 cycles (3546900 / 44100)
    private double audioCycleAccumulator = 0;
    private double cyclesPerAudioSample;

    public SpectrumMachine() {
        this.cpu = new Z80Cpu();
        this.memory = new Spectrum128Memory();
        this.ula = new UlaDisplay();
        this.beeper = new Beeper();
        this.psg = new Ay38912();
        this.audioMixer = new AudioMixer(beeper, psg);
        this.keyboard = new Keyboard();
        this.joystick = new Joystick(keyboard);
        this.tapePlayer = new TapePlayer();

        this.ioBus = new SpectrumIoBus(keyboard, joystick, ula, beeper, psg, memory);
        this.ioBus.setTapePlayer(tapePlayer);
        this.ioBus.setCpu(cpu);

        updateModelTiming();
        RomLoader.tryLoadDefaultRoms(memory);
        reset();
    }

    public void setModel(MachineModel model) {
        this.model = model;
        updateModelTiming();
        reset();
    }

    private void updateModelTiming() {
        this.cyclesPerAudioSample = (double) model.getClockFrequencyHz() / AudioMixer.SAMPLE_RATE;
    }

    public void reset() {
        cpu.reset();
        memory.reset();
        if (model == MachineModel.SPECTRUM_48K) {
            memory.setActiveRomBank(1);
            memory.setPagingLocked(true);
        }
        ula.clear(7);
        beeper.reset();
        psg.reset();
        keyboard.reset();
        joystick.reset();
        ioBus.resetPortDetection();
        audioCycleAccumulator = 0;
    }

    /**
     * Executes one video frame (~20ms, 70908 T-states on 128K).
     */
    public void stepFrame() {
        if (paused) return;

        int frameCycles = 0;
        int targetCycles = model.getTstatesPerFrame();

        while (frameCycles < targetCycles) {
            // Check tape ROM trap before executing instruction
            if (tapePlayer.checkRomTrap(cpu.getState(), memory)) {
                // Trap loaded block and performed RET
                continue;
            }

            int stepCycles = cpu.step(memory, ioBus);
            frameCycles += stepCycles;

            // Step peripherals
            psg.step(stepCycles);
            tapePlayer.step(stepCycles);
            beeper.setEarState(tapePlayer.getState() == TapePlayer.State.PLAYING && tapePlayer.getEarSignal());

            // Audio sampling
            if (!fastForward) {
                audioCycleAccumulator += stepCycles;
                while (audioCycleAccumulator >= cyclesPerAudioSample) {
                    audioCycleAccumulator -= cyclesPerAudioSample;
                    audioMixer.generateSample();
                }
            }
        }

        // Generate 50Hz frame interrupt for the next frame
        cpu.requestInt();

        // Render video frame buffer
        ula.renderFrame(memory.getActiveScreenRam());

        // Flush remaining audio samples
        if (!fastForward) {
            audioMixer.flushBuffer();
        }
    }

    /**
     * Executes a single CPU instruction when paused, updating video and peripherals.
     */
    public void stepSingleInstruction() {
        if (tapePlayer.checkRomTrap(cpu.getState(), memory)) {
            return;
        }
        int stepCycles = cpu.step(memory, ioBus);
        psg.step(stepCycles);
        tapePlayer.step(stepCycles);
        beeper.setEarState(tapePlayer.getState() == TapePlayer.State.PLAYING && tapePlayer.getEarSignal());
        ula.renderFrame(memory.getActiveScreenRam());
    }

    /**
     * Advances by exactly one full frame when paused.
     */
    public void stepSingleFrame() {
        boolean wasPaused = paused;
        paused = false;
        stepFrame();
        paused = wasPaused;
    }

    // --- Subsystem Accessors ---
    public Z80Cpu getCpu() { return cpu; }
    public Spectrum128Memory getMemory() { return memory; }
    public UlaDisplay getUla() { return ula; }
    public Beeper getBeeper() { return beeper; }
    public Ay38912 getPsg() { return psg; }
    public AudioMixer getAudioMixer() { return audioMixer; }
    public Keyboard getKeyboard() { return keyboard; }
    public Joystick getJoystick() { return joystick; }
    public SpectrumIoBus getIoBus() { return ioBus; }
    public TapePlayer getTapePlayer() { return tapePlayer; }
    public MachineModel getModel() { return model; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public boolean isFastForward() { return fastForward; }
    public void setFastForward(boolean fastForward) { this.fastForward = fastForward; }
}
