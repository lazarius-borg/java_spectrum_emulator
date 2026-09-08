package com.spectrum.cpu;

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

    public Registers getRegisters() { return registers; }

    public int getIm() { return im; }
    public void setIm(int im) { this.im = im; }

    public boolean isIff1() { return iff1; }
    public void setIff1(boolean iff1) { this.iff1 = iff1; }

    public boolean isIff2() { return iff2; }
    public void setIff2(boolean iff2) { this.iff2 = iff2; }

    public boolean isHalted() { return halted; }
    public void setHalted(boolean halted) { this.halted = halted; }

    public boolean isEiDelay() { return eiDelay; }
    public void setEiDelay(boolean eiDelay) { this.eiDelay = eiDelay; }

    public int getQ() { return q; }
    public void setQ(int q) { this.q = q & 0xFF; }

    public boolean isNmiPending() { return nmiPending; }
    public void setNmiPending(boolean nmiPending) { this.nmiPending = nmiPending; }

    public boolean isIntPending() { return intPending; }
    public void setIntPending(boolean intPending) { this.intPending = intPending; }

    public long getTotalCycles() { return totalCycles; }
    public void addCycles(int cycles) { this.totalCycles += cycles; }
}
