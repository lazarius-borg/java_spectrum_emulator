package nl.invokedynamic.spectrum.cpu;

/**
 * Encapsulates the entire CPU operational state including registers,
 * interrupt flip-flops, interrupt modes, and control signals.
 */
public final class CpuState {
    private final Registers registers = new Registers();
    private int im = 0;               // Interrupt mode: 0, 1, or 2
    private boolean iff1 = false;     // Interrupt flip-flop 1 (enables INT)
    private boolean iff2 = false;     // Interrupt flip-flop 2 (preserves IFF1 during NMI)
    private boolean halted = false;   // CPU is in HALT state
    private boolean eiDelay = false;  // EI enables interrupts after the subsequent instruction
    private int q = 0;                // Internal Q register tracking flag changes for SCF/CCF F3/F5
    private boolean nmiPending = false;
    private boolean intPending = false;
    private long totalCycles = 0;

    public CpuState() {
        reset();
    }

    /**
     * Resets all CPU state variables and registers to their power-on defaults.
     */
    public void reset() {
        registers.reset();
        im = 0;
        iff1 = false;
        iff2 = false;
        halted = false;
        eiDelay = false;
        q = 0;
        nmiPending = false;
        intPending = false;
        totalCycles = 0;
    }

    /** Returns the register file. */
    public Registers getRegisters() { return registers; }

    /** Returns the active interrupt mode (0, 1, or 2). */
    public int getIm() { return im; }
    /** Sets the active interrupt mode. */
    public void setIm(int im) { this.im = im; }

    /** Returns true if maskable interrupts are enabled (IFF1). */
    public boolean isIff1() { return iff1; }
    /** Sets the primary interrupt flip-flop state. */
    public void setIff1(boolean iff1) { this.iff1 = iff1; }

    /** Returns true if maskable interrupts were enabled before NMI (IFF2). */
    public boolean isIff2() { return iff2; }
    /** Sets the secondary interrupt flip-flop state. */
    public void setIff2(boolean iff2) { this.iff2 = iff2; }

    /** Returns true if the CPU is in HALT state. */
    public boolean isHalted() { return halted; }
    /** Sets the CPU HALT state. */
    public void setHalted(boolean halted) { this.halted = halted; }

    /** Returns true if EI was just executed, delaying interrupt processing by 1 instruction. */
    public boolean isEiDelay() { return eiDelay; }
    /** Sets the EI delay flag. */
    public void setEiDelay(boolean eiDelay) { this.eiDelay = eiDelay; }

    /** Returns internal Q register tracking flag changes for SCF/CCF F3 and F5 bits. */
    public int getQ() { return q; }
    /** Sets internal Q register value. */
    public void setQ(int q) { this.q = q & 0xFF; }

    /** Returns true if a non-maskable interrupt (NMI) is pending. */
    public boolean isNmiPending() { return nmiPending; }
    /** Sets NMI pending flag. */
    public void setNmiPending(boolean nmiPending) { this.nmiPending = nmiPending; }

    /** Returns true if a maskable interrupt (INT) is pending. */
    public boolean isIntPending() { return intPending; }
    /** Sets INT pending flag. */
    public void setIntPending(boolean intPending) { this.intPending = intPending; }

    /** Returns cumulative execution T-states since reset. */
    public long getTotalCycles() { return totalCycles; }
    /** Adds executed instruction T-states to the total cycle counter. */
    public void addCycles(int cycles) { this.totalCycles += cycles; }
}
