package com.spectrum.cpu;

/**
 * Z80 Rotate and shift operations, including undocumented SLL.
 */
public enum RotateOp {
    RLC, // Rotate left circular
    RRC, // Rotate right circular
    RL,  // Rotate left through carry
    RR,  // Rotate right through carry
    SLA, // Shift left arithmetic (sets bit 0 to 0)
    SRA, // Shift right arithmetic (preserves bit 7)
    SLL, // Undocumented: Shift left logical (sets bit 0 to 1)
    SRL; // Shift right logical (sets bit 7 to 0)

    public static RotateOp fromCode(int code) {
        return switch (code & 7) {
            case 0 -> RLC;
            case 1 -> RRC;
            case 2 -> RL;
            case 3 -> RR;
            case 4 -> SLA;
            case 5 -> SRA;
            case 6 -> SLL; // Undocumented opcode: shift left and set bit 0 to 1
            case 7 -> SRL;
            default -> throw new IllegalStateException();
        };
    }
}
