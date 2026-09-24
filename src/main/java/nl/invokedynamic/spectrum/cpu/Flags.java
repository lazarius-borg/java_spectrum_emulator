package nl.invokedynamic.spectrum.cpu;

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
    /** Carry flag mask (bit 0). */
    public static final int C_MASK  = 0x01;
    /** Add/Subtract flag mask (bit 1, 1 for subtract). */
    public static final int N_MASK  = 0x02;
    /** Parity / Overflow flag mask (bit 2). */
    public static final int PV_MASK = 0x04;
    /** Undocumented copy of result bit 3 (bit 3). */
    public static final int F3_MASK = 0x08;
    /** Half Carry flag mask (bit 4). */
    public static final int H_MASK  = 0x10;
    /** Undocumented copy of result bit 5 (bit 5). */
    public static final int F5_MASK = 0x20;
    /** Zero flag mask (bit 6). */
    public static final int Z_MASK  = 0x40;
    /** Sign flag mask (bit 7, 1 for negative). */
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
