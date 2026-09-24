package nl.invokedynamic.spectrum.storage;

import nl.invokedynamic.spectrum.cpu.CpuState;
import nl.invokedynamic.spectrum.cpu.Flags;
import nl.invokedynamic.spectrum.memory.MemoryBus;

import java.util.ArrayList;
import java.util.List;

/**
 * ZX Spectrum Tape deck supporting both cycle-exact pulse emulation
 * (audio and border stripes) and instantaneous ROM trap loading.
 */
public final class TapePlayer {
    public enum State {
        STOPPED,
        PLAYING,
        RECORDING,
        PAUSED
    }

    private State state = State.STOPPED;
    private final List<TapFileFormat.TapBlock> blocks = new ArrayList<>();
    private int currentBlockIndex = 0;

    // Pulse generator timing parameters (in T-states)
    private static final int PILOT_PULSE_LEN = 2168;
    private static final int PILOT_PULSES_HEADER = 8063;
    private static final int PILOT_PULSES_DATA = 3223;
    private static final int SYNC_1_LEN = 667;
    private static final int SYNC_2_LEN = 735;
    private static final int BIT_0_PULSE_LEN = 855;
    private static final int BIT_1_PULSE_LEN = 1710;
    private static final int PAUSE_LEN = 3500000; // 1 second @ 3.5 MHz

    private enum Phase {
        PILOT, SYNC1, SYNC2, DATA_PULSE_1, DATA_PULSE_2, PAUSE
    }

    private Phase currentPhase = Phase.PILOT;
    private int phasePulsesRemaining = 0;
    private int currentByteIndex = 0;
    private int currentBitIndex = 7;
    private int tstatesUntilNextEdge = 0;
    private boolean earSignal = false;

    // Instant load option (ROM LD-BYTES trap at 0x0556)
    private boolean instantLoadingEnabled = true;

    // Recording buffer
    private final List<TapFileFormat.TapBlock> recordedBlocks = new ArrayList<>();
    private boolean lastMicState = false;

    public void loadTape(List<TapFileFormat.TapBlock> newBlocks) {
        stop();
        blocks.clear();
        blocks.addAll(newBlocks);
        currentBlockIndex = 0;
    }

    public List<TapFileFormat.TapBlock> getBlocks() {
        return blocks;
    }

    public int getCurrentBlockIndex() {
        return currentBlockIndex;
    }

    public void play() {
        if (blocks.isEmpty() || currentBlockIndex >= blocks.size()) {
            state = State.STOPPED;
            return;
        }
        state = State.PLAYING;
        startBlock(currentBlockIndex);
    }

    public void pause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
        } else if (state == State.PAUSED) {
            state = State.PLAYING;
        }
    }

    public void stop() {
        state = State.STOPPED;
        earSignal = false;
        tstatesUntilNextEdge = 0;
    }

    public void rewind() {
        stop();
        currentBlockIndex = 0;
    }

    public State getState() {
        return state;
    }

    public boolean getEarSignal() {
        return earSignal;
    }

    public boolean isInstantLoadingEnabled() {
        return instantLoadingEnabled;
    }

    public void setInstantLoadingEnabled(boolean enabled) {
        this.instantLoadingEnabled = enabled;
    }

    private void startBlock(int index) {
        if (index >= blocks.size()) {
            stop();
            return;
        }
        var block = blocks.get(index);
        currentPhase = Phase.PILOT;
        phasePulsesRemaining = block.isHeader() ? PILOT_PULSES_HEADER : PILOT_PULSES_DATA;
        tstatesUntilNextEdge = PILOT_PULSE_LEN;
        currentByteIndex = 0;
        currentBitIndex = 7;
        earSignal = false;
    }

    /**
     * Advances tape pulse generator by given CPU T-states during emulation.
     */
    public void step(int tStates) {
        if (state != State.PLAYING || blocks.isEmpty() || currentBlockIndex >= blocks.size()) {
            return;
        }

        tstatesUntilNextEdge -= tStates;
        while (tstatesUntilNextEdge <= 0) {
            advancePulse();
        }
    }

    private void advancePulse() {
        earSignal = !earSignal;
        var block = blocks.get(currentBlockIndex);
        byte[] data = block.data();

        switch (currentPhase) {
            case PILOT -> {
                phasePulsesRemaining--;
                if (phasePulsesRemaining > 0) {
                    tstatesUntilNextEdge += PILOT_PULSE_LEN;
                } else {
                    currentPhase = Phase.SYNC1;
                    tstatesUntilNextEdge += SYNC_1_LEN;
                }
            }
            case SYNC1 -> {
                currentPhase = Phase.SYNC2;
                tstatesUntilNextEdge += SYNC_2_LEN;
            }
            case SYNC2 -> {
                currentPhase = Phase.DATA_PULSE_1;
                tstatesUntilNextEdge += getBitPulseLen(data[currentByteIndex], currentBitIndex);
            }
            case DATA_PULSE_1 -> {
                currentPhase = Phase.DATA_PULSE_2;
                tstatesUntilNextEdge += getBitPulseLen(data[currentByteIndex], currentBitIndex);
            }
            case DATA_PULSE_2 -> {
                currentBitIndex--;
                if (currentBitIndex < 0) {
                    currentBitIndex = 7;
                    currentByteIndex++;
                    if (currentByteIndex >= data.length) {
                        currentPhase = Phase.PAUSE;
                        tstatesUntilNextEdge += PAUSE_LEN;
                        earSignal = false;
                        return;
                    }
                }
                currentPhase = Phase.DATA_PULSE_1;
                tstatesUntilNextEdge += getBitPulseLen(data[currentByteIndex], currentBitIndex);
            }
            case PAUSE -> {
                currentBlockIndex++;
                if (currentBlockIndex < blocks.size()) {
                    startBlock(currentBlockIndex);
                } else {
                    stop();
                }
            }
        }
    }

    private int getBitPulseLen(byte b, int bit) {
        return ((b >> bit) & 1) != 0 ? BIT_1_PULSE_LEN : BIT_0_PULSE_LEN;
    }

    /**
     * Intercepts the standard Sinclair Spectrum ROM LD-BYTES routine at 0x0556.
     * When active, copies the requested block directly into memory and returns success!
     */
    public boolean checkRomTrap(CpuState cpu, MemoryBus memory) {
        if (!instantLoadingEnabled || blocks.isEmpty() || currentBlockIndex >= blocks.size()) {
            return false;
        }

        // Only active in ROM 1 (48K BASIC / Sinclair ROM LD-BYTES)
        if (memory instanceof nl.invokedynamic.spectrum.memory.Spectrum128Memory mem128) {
            if (mem128.getActiveRomBank() != 1) {
                return false;
            }
        }

        var r = cpu.getRegisters();
        int pc = r.getPC();

        // 0x0556 is the entry point of LD-BYTES in standard Spectrum ROM (ROM 1 or 48K)
        if (pc == 0x0556) {
            int requestedFlag = r.getA();
            int startAddress = r.getIX();
            int length = r.getDE();

            // Find next matching block with the requested flag
            int searchIdx = currentBlockIndex;
            while (searchIdx < blocks.size() && blocks.get(searchIdx).flag() != requestedFlag) {
                searchIdx++;
            }

            if (searchIdx >= blocks.size()) {
                return false;
            }

            currentBlockIndex = searchIdx;
            TapFileFormat.TapBlock block = blocks.get(currentBlockIndex);
            byte[] data = block.data();

            int availableBytes = Math.max(0, data.length - 2);
            int bytesToLoad = Math.min(length, availableBytes);

            // Copy block payload to memory
            for (int i = 0; i < bytesToLoad; i++) {
                memory.writeByte(startAddress + i, data[1 + i] & 0xFF);
            }

            currentBlockIndex++;
            if (currentBlockIndex >= blocks.size()) {
                stop();
            }

            // Emulate successful return from LD-BYTES:
            // 1. IX advanced by length
            r.setIX((startAddress + length) & 0xFFFF);
            // 2. DE decremented to 0
            r.setDE(0);
            // 3. A set to 0 (checksum match)
            r.setA(0);
            // 4. Carry flag set = success
            r.setF((r.getF() | Flags.C_MASK) & ~Flags.Z_MASK);

            // RET to caller: pop PC from SP
            int sp = r.getSP();
            int retAddr = memory.readWord(sp);
            r.setSP((sp + 2) & 0xFFFF);
            r.setPC(retAddr);

            return true;
        }
        return false;
    }

    public void recordMic(boolean mic) {
        if (state != State.RECORDING) return;
        // Simple edge recorder
        lastMicState = mic;
    }
}
