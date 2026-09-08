package com.spectrum.sound;

/**
 * Emulation of the General Instrument AY-3-8912 Programmable Sound Generator (PSG)
 * used in the ZX Spectrum 128K.
 *
 * Clocked at 1.77345 MHz (half the 128K CPU clock).
 * 3 Tone channels (A, B, C), 1 Noise generator, 1 Envelope generator.
 */
public final class Ay38912 {
    // Standard AY-3-8912 logarithmic amplitude table normalized to 0.0 .. 1.0
    private static final float[] VOL_TABLE = new float[] {
        0.0000f, 0.0078f, 0.0110f, 0.0156f,
        0.0221f, 0.0313f, 0.0442f, 0.0625f,
        0.0884f, 0.1250f, 0.1768f, 0.2500f,
        0.3536f, 0.5000f, 0.7071f, 1.0000f
    };

    private final int[] registers = new int[16];
    private int selectedRegister = 0;

    // Tone generators
    private int toneCountA = 0;
    private int toneCountB = 0;
    private int toneCountC = 0;
    private int toneOutA = 1;
    private int toneOutB = 1;
    private int toneOutC = 1;

    // Noise generator
    private int noiseCount = 0;
    private int noiseLfsr = 1;
    private int noiseOut = 1;

    // Envelope generator
    private int envCount = 0;
    private int envStep = 15;
    private boolean envHolding = false;
    private boolean envAttack = false;

    public void reset() {
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0;
        }
        selectedRegister = 0;
        toneCountA = toneCountB = toneCountC = 0;
        toneOutA = toneOutB = toneOutC = 1;
        noiseCount = 0;
        noiseLfsr = 1;
        noiseOut = 1;
        envCount = 0;
        envStep = 15;
        envHolding = false;
        envAttack = false;
    }

    public void selectRegister(int reg) {
        selectedRegister = reg & 0x0F;
    }

    public int readData() {
        if (selectedRegister == 14) {
            return 0xFF; // Port A read
        }
        return registers[selectedRegister];
    }

    public void writeData(int value) {
        value &= 0xFF;
        registers[selectedRegister] = value;

        if (selectedRegister == 13) {
            // Reset envelope cycle
            envCount = 0;
            envHolding = false;
            boolean att = (value & 0x04) != 0;
            envAttack = att;
            envStep = att ? 0 : 15;
        }
    }

    public int[] getRegisters() {
        return registers;
    }

    /**
     * Advances the AY PSG by a number of CPU cycles and generates audio output for channels A, B, C.
     */
    public void step(int cpuCycles) {
        // AY clock is half the CPU clock: 1 AY cycle = 2 CPU cycles
        int ayCycles = cpuCycles >> 1;
        if (ayCycles <= 0) ayCycles = 1;

        // 1. Update Tone Generators
        int periodA = (registers[0] | ((registers[1] & 0x0F) << 8));
        if (periodA == 0) periodA = 1;
        toneCountA += ayCycles;
        while (toneCountA >= (periodA << 3)) {
            toneCountA -= (periodA << 3);
            toneOutA ^= 1;
        }

        int periodB = (registers[2] | ((registers[3] & 0x0F) << 8));
        if (periodB == 0) periodB = 1;
        toneCountB += ayCycles;
        while (toneCountB >= (periodB << 3)) {
            toneCountB -= (periodB << 3);
            toneOutB ^= 1;
        }

        int periodC = (registers[4] | ((registers[5] & 0x0F) << 8));
        if (periodC == 0) periodC = 1;
        toneCountC += ayCycles;
        while (toneCountC >= (periodC << 3)) {
            toneCountC -= (periodC << 3);
            toneOutC ^= 1;
        }

        // 2. Update Noise Generator
        int noisePeriod = (registers[6] & 0x1F);
        if (noisePeriod == 0) noisePeriod = 1;
        noiseCount += ayCycles;
        while (noiseCount >= (noisePeriod << 3)) {
            noiseCount -= (noisePeriod << 3);
            // 17-bit LFSR: feedback = bit 0 ^ bit 3
            int feedback = (noiseLfsr & 1) ^ ((noiseLfsr >> 3) & 1);
            noiseLfsr = (noiseLfsr >> 1) | (feedback << 16);
            noiseOut = noiseLfsr & 1;
        }

        // 3. Update Envelope Generator
        if (!envHolding) {
            int envPeriod = (registers[11] | (registers[12] << 8));
            if (envPeriod == 0) envPeriod = 1;
            envCount += ayCycles;
            while (envCount >= (envPeriod << 3)) {
                envCount -= (envPeriod << 3);
                stepEnvelope();
            }
        }
    }

    private void stepEnvelope() {
        int shape = registers[13] & 0x0F;
        boolean cont = (shape & 0x08) != 0;
        boolean att  = (shape & 0x04) != 0;
        boolean alt  = (shape & 0x02) != 0;
        boolean hold = (shape & 0x01) != 0;

        if (envAttack) {
            if (envStep < 15) {
                envStep++;
            } else {
                handleEnvelopeCycleEnd(cont, att, alt, hold);
            }
        } else {
            if (envStep > 0) {
                envStep--;
            } else {
                handleEnvelopeCycleEnd(cont, att, alt, hold);
            }
        }
    }

    private void handleEnvelopeCycleEnd(boolean cont, boolean att, boolean alt, boolean hold) {
        if (!cont) {
            envStep = 0;
            envHolding = true;
        } else if (hold) {
            if (alt) {
                envStep = envAttack ? 0 : 15;
            }
            envHolding = true;
        } else {
            if (alt) {
                envAttack = !envAttack;
            }
            envStep = envAttack ? 0 : 15;
        }
    }

    /**
     * Computes the current mixed output amplitude for channel A, B, and C.
     */
    public float getChannelA() { return calcChannel(toneOutA, (registers[7] & 0x01) == 0, (registers[7] & 0x08) == 0, registers[8]); }
    public float getChannelB() { return calcChannel(toneOutB, (registers[7] & 0x02) == 0, (registers[7] & 0x10) == 0, registers[9]); }
    public float getChannelC() { return calcChannel(toneOutC, (registers[7] & 0x04) == 0, (registers[7] & 0x20) == 0, registers[10]); }

    private float calcChannel(int toneOut, boolean toneEnabled, boolean noiseEnabled, int ampReg) {
        int bit = 1;
        if (toneEnabled)  bit &= toneOut;
        if (noiseEnabled) bit &= noiseOut;

        if (bit == 0) {
            return 0.0f;
        }

        int vol = (ampReg & 0x10) != 0 ? envStep : (ampReg & 0x0F);
        return VOL_TABLE[vol & 0x0F];
    }
}
