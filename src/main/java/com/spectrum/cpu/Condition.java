package com.spectrum.cpu;

/**
 * Conditional jump/call/return condition codes.
 */
public enum Condition {
    NZ, // Not Zero (Z = 0)
    Z,  // Zero (Z = 1)
    NC, // Not Carry (C = 0)
    C,  // Carry (C = 1)
    PO, // Parity Odd / No Overflow (P/V = 0)
    PE, // Parity Even / Overflow (P/V = 1)
    P,  // Positive (S = 0)
    M;  // Minus / Negative (S = 1)

    public static Condition fromCode(int code) {
        return switch (code & 7) {
            case 0 -> NZ;
            case 1 -> Z;
            case 2 -> NC;
            case 3 -> C;
            case 4 -> PO;
            case 5 -> PE;
            case 6 -> P;
            case 7 -> M;
            default -> throw new IllegalStateException();
        };
    }

    public boolean test(int flags) {
        return switch (this) {
            case NZ -> (flags & Flags.Z_MASK) == 0;
            case Z  -> (flags & Flags.Z_MASK) != 0;
            case NC -> (flags & Flags.C_MASK) == 0;
            case C  -> (flags & Flags.C_MASK) != 0;
            case PO -> (flags & Flags.PV_MASK) == 0;
            case PE -> (flags & Flags.PV_MASK) != 0;
            case P  -> (flags & Flags.S_MASK) == 0;
            case M  -> (flags & Flags.S_MASK) != 0;
        };
    }
}
