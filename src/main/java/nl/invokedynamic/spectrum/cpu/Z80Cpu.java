package nl.invokedynamic.spectrum.cpu;

import nl.invokedynamic.spectrum.io.IoBus;
import nl.invokedynamic.spectrum.memory.MemoryBus;

/**
 * Z80 CPU implementation coordinating the decoder, executor,
 * state, and interrupt handling.
 */
public final class Z80Cpu {
    private final CpuState state;
    private final InstructionDecoder decoder;
    private final InstructionExecutor executor;

    public Z80Cpu() {
        this.state = new CpuState();
        this.decoder = new InstructionDecoder();
        this.executor = new InstructionExecutor();
    }

    public CpuState getState() {
        return state;
    }

    public void reset() {
        state.reset();
    }

    /**
     * Executes one instruction or handles a pending interrupt.
     * Returns the number of T-states consumed.
     */
    public int step(MemoryBus memory, IoBus io) {
        // 1. Check Non-Maskable Interrupt (NMI)
        if (state.isNmiPending()) {
            state.setNmiPending(false);
            return handleNmi(memory);
        }

        // 2. Check Maskable Interrupt (INT)
        if (state.isIntPending() && state.isIff1() && !state.isEiDelay()) {
            state.setIntPending(false);
            return handleInt(memory);
        }

        // Reset EI delay after checking interrupts
        state.setEiDelay(false);

        // 3. Handle HALT state
        if (state.isHalted()) {
            state.getRegisters().incR();
            state.addCycles(4);
            return 4;
        }

        // 4. Fetch, Decode, and Execute instruction
        Instruction instruction = decoder.decode(state, memory);
        int cycles = executor.execute(instruction, state, memory, io);
        state.addCycles(cycles);
        return cycles;
    }

    /**
     * Triggers a maskable interrupt request line.
     */
    public void requestInt() {
        state.setIntPending(true);
    }

    /**
     * Triggers a non-maskable interrupt (NMI).
     */
    public void requestNmi() {
        state.setNmiPending(true);
    }

    private int handleNmi(MemoryBus memory) {
        state.setHalted(false);
        state.setIff1(false); // Preserves IFF2
        state.getRegisters().incR();

        pushWord(memory, state.getRegisters().getPC());
        state.getRegisters().setPC(0x0066);
        state.getRegisters().setMemptr(0x0066);

        state.addCycles(11);
        return 11;
    }

    private int handleInt(MemoryBus memory) {
        state.setHalted(false);
        state.setIff1(false);
        state.setIff2(false);
        state.getRegisters().incR();

        int cycles;
        int im = state.getIm();

        switch (im) {
            case 0, 1 -> {
                // In ZX Spectrum, IM 0 and IM 1 both jump to 0x0038
                pushWord(memory, state.getRegisters().getPC());
                state.getRegisters().setPC(0x0038);
                state.getRegisters().setMemptr(0x0038);
                cycles = 13;
            }
            case 2 -> {
                pushWord(memory, state.getRegisters().getPC());
                // Data bus on Spectrum floats to 0xFF when idle
                int vectorAddress = ((state.getRegisters().getI() & 0xFF) << 8) | 0xFF;
                int targetPc = memory.readWord(vectorAddress);
                state.getRegisters().setPC(targetPc);
                state.getRegisters().setMemptr(targetPc);
                cycles = 19;
            }
            default -> throw new IllegalStateException("Unexpected IM: " + im);
        }

        state.addCycles(cycles);
        return cycles;
    }

    private void pushWord(MemoryBus memory, int val) {
        var r = state.getRegisters();
        int sp = (r.getSP() - 2) & 0xFFFF;
        r.setSP(sp);
        memory.writeWord(sp, val);
    }
}
