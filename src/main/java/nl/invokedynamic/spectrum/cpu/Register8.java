package nl.invokedynamic.spectrum.cpu;

/**
 * 8-bit Z80 registers, including index half-registers (IXH, IXL, IYH, IYL).
 */
public enum Register8 {
    B, C, D, E, H, L, A,
    // Undocumented 8-bit halves of IX and IY
    IXH, IXL, IYH, IYL;

    public static Register8 fromCode(int code) {
        return switch (code & 7) {
            case 0 -> B;
            case 1 -> C;
            case 2 -> D;
            case 3 -> E;
            case 4 -> H;
            case 5 -> L;
            case 7 -> A;
            default -> throw new IllegalArgumentException("Invalid 8-bit register code: " + code);
        };
    }
}
