package com.spectrum.cpu;

public enum AluOp {
    ADD, ADC, SUB, SBC, AND, XOR, OR, CP;

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
