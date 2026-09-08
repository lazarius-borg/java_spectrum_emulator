package com.spectrum.cpu;

/**
 * Z80 Flag register bit definitions and lookup tables.
 *
 * Bit 7: S  (Sign)
 * Bit 6: Z  (Zero)
 * Bit 5: F5 (Undocumented, bit 5 of result / MEMPTR)
 * Bit 4: H  (Half Carry)
 * Bit 3: F3 (Undocumented, bit 3 of result / MEMPTR)
 * Bit 2: P  (Parity / Overflow: P/V)
 * Bit 1: N  (Subtract)
 * Bit 0: C  (Carry)
 */
public final class Flags {
    public static final int C_MASK  = 0x01;
    public static final int N_MASK  = 0x02;
    public static final int PV_MASK = 0x04;
    public static final int F3_MASK = 0x08;
    public static final int H_MASK  = 0x10;
    public static final int F5_MASK = 0x20;
    public static final int Z_MASK  = 0x40;
    public static final int S_MASK  = 0x80;

    /**
     * Precomputed parity table for all 256 8-bit values.
     * True if parity is even (even number of 1 bits, PV_MASK set).
     */
    public static final int[] PARITY_TABLE = new int[256];
    
    /**
     * Precomputed SZ53 table for 8-bit values (sets S, Z, F5, F3).
     */
    public static final int[] SZ53_TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int bits = Integer.bitCount(i);
            PARITY_TABLE[i] = (bits % 2 == 0) ? PV_MASK : 0;

            int flags = i & (S_MASK | F5_MASK | F3_MASK);
            if (i == 0) {
                flags |= Z_MASK;
            }
            SZ53_TABLE[i] = flags;
        }
    }

    private Flags() {}
}
