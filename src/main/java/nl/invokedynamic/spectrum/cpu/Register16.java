package nl.invokedynamic.spectrum.cpu;

/**
 * 16-bit register pairs and special pointers.
 */
public enum Register16 {
    BC, DE, HL, SP, AF, IX, IY, PC;

    public static Register16 fromRp(int code) {
        return switch (code & 3) {
            case 0 -> BC;
            case 1 -> DE;
            case 2 -> HL;
            case 3 -> SP;
            default -> throw new IllegalStateException();
        };
    }

    public static Register16 fromRp2(int code) {
        return switch (code & 3) {
            case 0 -> BC;
            case 1 -> DE;
            case 2 -> HL;
            case 3 -> AF;
            default -> throw new IllegalStateException();
        };
    }
}
