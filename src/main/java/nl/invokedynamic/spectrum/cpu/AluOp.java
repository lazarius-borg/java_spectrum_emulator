package nl.invokedynamic.spectrum.cpu;

/**
 * Standard 8-bit Arithmetic Logic Unit (ALU) operations of the Z80 CPU:
 * ADD, ADC (add with carry), SUB, SBC (subtract with carry),
 * AND, XOR, OR, and CP (compare).
 */
public enum AluOp {
    ADD, ADC, SUB, SBC, AND, XOR, OR, CP;

    /**
     * Decodes 3-bit ALU opcode field (bits 3-5 of base ALU instructions).
     */
    public static AluOp fromCode(int code) {
        return switch (code & 7) {
            case 0 -> ADD;
            case 1 -> ADC;
            case 2 -> SUB;
            case 3 -> SBC;
            case 4 -> AND;
            case 5 -> XOR;
            case 6 -> OR;
            case 7 -> CP;
            default -> throw new IllegalStateException();
        };
    }
}
