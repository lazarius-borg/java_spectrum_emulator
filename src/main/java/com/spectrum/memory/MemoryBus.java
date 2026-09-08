package com.spectrum.memory;

/**
 * Interface representing the 64K memory bus of the ZX Spectrum.
 */
public interface MemoryBus {
    /**
     * Reads a byte from the specified 16-bit address (0x0000 - 0xFFFF).
     */
    int readByte(int address);

    /**
     * Writes a byte to the specified 16-bit address (0x0000 - 0xFFFF).
     */
    void writeByte(int address, int value);

    /**
     * Reads a 16-bit word (little-endian) from the specified address.
     */
    default int readWord(int address) {
        int low = readByte(address & 0xFFFF);
        int high = readByte((address + 1) & 0xFFFF);
        return (high << 8) | low;
    }

    /**
     * Writes a 16-bit word (little-endian) to the specified address.
     */
    default void writeWord(int address, int value) {
        writeByte(address & 0xFFFF, value & 0xFF);
        writeByte((address + 1) & 0xFFFF, (value >> 8) & 0xFF);
    }

    /**
     * Optional contention cycle notification when memory is accessed.
     */
    default void contend(int address, int tStates) {}
}
