package com.spectrum.machine;

import com.spectrum.memory.Spectrum128Memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads ZX Spectrum ROMs from disk or provides a built-in test pattern fallback.
 */
public final class RomLoader {

    public static boolean tryLoadDefaultRoms(Spectrum128Memory memory) {
        Path romsDir = Path.of("roms");
        if (!Files.exists(romsDir)) {
            try {
                Files.createDirectories(romsDir);
            } catch (IOException ignored) {}
        }

        // Check for single 32K 128K ROM (rom0 + rom1 combined)
        Path combined128 = romsDir.resolve("128k.rom");
        if (!Files.exists(combined128)) combined128 = romsDir.resolve("zx128.rom");

        if (Files.exists(combined128)) {
            try {
                byte[] bytes = Files.readAllBytes(combined128);
                if (bytes.length >= 32768) {
                    byte[] rom0 = new byte[16384];
                    byte[] rom1 = new byte[16384];
                    System.arraycopy(bytes, 0, rom0, 0, 16384);
                    System.arraycopy(bytes, 16384, rom1, 0, 16384);
                    memory.loadRom(0, rom0);
                    memory.loadRom(1, rom1);
                    return true;
                }
            } catch (IOException ignored) {}
        }

        // Check for separate 16K ROM files
        Path rom0Path = findRomFile(romsDir, "128-0.rom", "rom0.rom");
        Path rom1Path = findRomFile(romsDir, "128-1.rom", "rom1.rom", "48.rom");

        if (rom0Path != null && rom1Path != null) {
            try {
                memory.loadRom(0, Files.readAllBytes(rom0Path));
                memory.loadRom(1, Files.readAllBytes(rom1Path));
                return true;
            } catch (IOException ignored) {}
        }

        // Fallback: install built-in test pattern bootloader
        installTestRom(memory);
        return false;
    }

    private static Path findRomFile(Path dir, String... names) {
        for (String name : names) {
            Path p = dir.resolve(name);
            if (Files.exists(p)) return p;
        }
        return null;
    }

    /**
     * Installs a self-contained Z80 test program into ROM 0 and ROM 1
     * that sets border color, paints a greeting pattern on screen, and loops.
     */
    public static void installTestRom(Spectrum128Memory memory) {
        byte[] testRom = new byte[16384];

        // Z80 machine code for test boot sequence:
        // DI                 ; F3
        // LD SP, 0xFFFF      ; 31 FF FF
        // LD A, 0x07 (white) ; 3E 07
        // OUT (0xFE), A      ; D3 FE (border white)
        // LD HL, 0x5800      ; 21 00 58 (Attributes area)
        // LD (HL), 0x38      ; 36 38 (White paper, black ink)
        // LD DE, 0x5801      ; 11 01 58
        // LD BC, 0x02FF      ; 01 FF 02
        // LDIR               ; ED B0 (fill screen attributes)
        // loop:
        // EI                 ; FB
        // HALT               ; 76
        // JR loop            ; 18 FC

        int i = 0;
        testRom[i++] = (byte) 0xF3;             // DI
        testRom[i++] = (byte) 0x31;             // LD SP, 0xFFFF
        testRom[i++] = (byte) 0xFF;
        testRom[i++] = (byte) 0xFF;
        testRom[i++] = (byte) 0x3E;             // LD A, 0x07
        testRom[i++] = (byte) 0x07;
        testRom[i++] = (byte) 0xD3;             // OUT (0xFE), A
        testRom[i++] = (byte) 0xFE;
        testRom[i++] = (byte) 0x21;             // LD HL, 0x5800
        testRom[i++] = (byte) 0x00;
        testRom[i++] = (byte) 0x58;
        testRom[i++] = (byte) 0x36;             // LD (HL), 0x38
        testRom[i++] = (byte) 0x38;
        testRom[i++] = (byte) 0x11;             // LD DE, 0x5801
        testRom[i++] = (byte) 0x01;
        testRom[i++] = (byte) 0x58;
        testRom[i++] = (byte) 0x01;             // LD BC, 0x02FF
        testRom[i++] = (byte) 0xFF;
        testRom[i++] = (byte) 0x02;
        testRom[i++] = (byte) 0xED;             // LDIR
        testRom[i++] = (byte) 0xB0;
        testRom[i++] = (byte) 0xFB;             // EI
        testRom[i++] = (byte) 0x76;             // HALT
        testRom[i++] = (byte) 0x18;             // JR -2 (loop)
        testRom[i++] = (byte) 0xFC;

        // RST 38h interrupt handler (0x0038): RETI (ED 4D)
        testRom[0x0038] = (byte) 0xED;
        testRom[0x0039] = (byte) 0x4D;

        memory.loadRom(0, testRom);
        memory.loadRom(1, testRom);
    }
}
