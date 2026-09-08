package com.spectrum.memory;

import java.util.Arrays;

/**
 * Implements the memory architecture of the ZX Spectrum 128K.
 *
 * 128K RAM organized into 8 banks of 16KB (0 - 7).
 * 32K ROM organized into 2 banks of 16KB (0 = 128K Editor, 1 = 48K BASIC).
 *
 * Address Space (64KB):
 * - 0x0000 - 0x3FFF: ROM 0 or ROM 1 (selected by Port 0x7FFD bit 4)
 * - 0x4000 - 0x7FFF: RAM Bank 5 (always, primary screen 0)
 * - 0x8000 - 0xBFFF: RAM Bank 2 (always)
 * - 0xC000 - 0xFFFF: Paged RAM Bank 0 - 7 (selected by Port 0x7FFD bits 0-2)
 */
public final class Spectrum128Memory implements MemoryBus {
    public static final int BANK_SIZE = 16384; // 16KB

    private final byte[][] ram = new byte[8][BANK_SIZE];
    private final byte[][] rom = new byte[2][BANK_SIZE];

    private int activeRamBank = 0;   // Bank paged at 0xC000 (0-7)
    private int activeRomBank = 0;   // ROM paged at 0x0000 (0-1)
    private int screenBank = 5;      // Active display screen: Bank 5 (normal) or Bank 7 (shadow)
    private boolean pagingLocked = false; // Set by Port 0x7FFD bit 5 until reset
    private int lastPort7ffd = 0;

    public Spectrum128Memory() {
        reset();
    }

    public void reset() {
        activeRamBank = 0;
        activeRomBank = 0;
        screenBank = 5;
        pagingLocked = false;
        lastPort7ffd = 0;
    }

    /**
     * Loads 16KB ROM image into ROM bank 0 or 1.
     */
    public void loadRom(int romBankIndex, byte[] data) {
        if (romBankIndex < 0 || romBankIndex > 1) {
            throw new IllegalArgumentException("ROM bank must be 0 or 1");
        }
        int len = Math.min(data.length, BANK_SIZE);
        System.arraycopy(data, 0, rom[romBankIndex], 0, len);
    }

    /**
     * Writes to 128K paging control port 0x7FFD.
     * Decoded when (port & 0x8002) == 0.
     */
    public void writePort7ffd(int value) {
        if (pagingLocked) {
            return; // Further paging disabled until reset
        }

        lastPort7ffd = value & 0xFF;
        activeRamBank = value & 0x07;                // Bits 0-2: RAM bank at 0xC000
        screenBank = ((value & 0x08) != 0) ? 7 : 5;  // Bit 3: Screen 0 (Bank 5) or Screen 1 (Bank 7)
        activeRomBank = ((value & 0x10) != 0) ? 1 : 0; // Bit 4: ROM 0 (128K) or ROM 1 (48K)
        if ((value & 0x20) != 0) {
            pagingLocked = true;                     // Bit 5: Disable paging
        }
    }

    public int getActiveRamBank() { return activeRamBank; }
    public void setActiveRamBank(int bank) { this.activeRamBank = bank & 7; }

    public int getActiveRomBank() { return activeRomBank; }
    public void setActiveRomBank(int bank) { this.activeRomBank = bank & 1; }

    public int getScreenBank() { return screenBank; }
    public void setScreenBank(int bank) { this.screenBank = (bank == 7) ? 7 : 5; }

    public boolean isPagingLocked() { return pagingLocked; }
    public void setPagingLocked(boolean locked) { this.pagingLocked = locked; }

    public int getLastPort7ffd() { return lastPort7ffd; }

    public byte[] getRamBank(int bank) {
        return ram[bank & 7];
    }

    public byte[] getRomBank(int bank) {
        return rom[bank & 1];
    }

    public byte[] getActiveScreenRam() {
        return ram[screenBank];
    }

    @Override
    public int readByte(int address) {
        address &= 0xFFFF;
        int page = address >> 14; // 0..3
        int offset = address & 0x3FFF;

        return switch (page) {
            case 0 -> rom[activeRomBank][offset] & 0xFF;
            case 1 -> ram[5][offset] & 0xFF; // Always Bank 5
            case 2 -> ram[2][offset] & 0xFF; // Always Bank 2
            case 3 -> ram[activeRamBank][offset] & 0xFF;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0xFFFF;
        int page = address >> 14;
        int offset = address & 0x3FFF;
        byte b = (byte) value;

        switch (page) {
            case 0 -> {
                // ROM is read-only, writes are ignored
            }
            case 1 -> ram[5][offset] = b;
            case 2 -> ram[2][offset] = b;
            case 3 -> ram[activeRamBank][offset] = b;
        }
    }

    /**
     * Clears all RAM banks to zero.
     */
    public void clearRam() {
        for (byte[] bank : ram) {
            Arrays.fill(bank, (byte) 0);
        }
    }
}
