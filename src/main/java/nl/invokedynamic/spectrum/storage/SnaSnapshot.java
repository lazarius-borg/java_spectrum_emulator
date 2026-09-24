package nl.invokedynamic.spectrum.storage;

import nl.invokedynamic.spectrum.cpu.CpuState;
import nl.invokedynamic.spectrum.cpu.Registers;
import nl.invokedynamic.spectrum.memory.Spectrum128Memory;
import nl.invokedynamic.spectrum.ula.UlaDisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves ZX Spectrum .SNA snapshots (both 48K and 128K models).
 */
public final class SnaSnapshot {

    public static void load(Path path, CpuState cpu, Spectrum128Memory memory, UlaDisplay ula) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        loadFromBytes(bytes, cpu, memory, ula);
    }

    public static void loadFromBytes(byte[] bytes, CpuState cpu, Spectrum128Memory memory, UlaDisplay ula) {
        if (bytes.length < 49179) {
            throw new IllegalArgumentException("Invalid SNA file: size too small (" + bytes.length + " bytes)");
        }

        var r = cpu.getRegisters();

        // Header (27 bytes)
        r.setI(bytes[0] & 0xFF);
        r.setHLPrime(readWord(bytes, 1));
        r.setDEPrime(readWord(bytes, 3));
        r.setBCPrime(readWord(bytes, 5));
        r.setAFPrime(readWord(bytes, 7));

        r.setHL(readWord(bytes, 9));
        r.setDE(readWord(bytes, 11));
        r.setBC(readWord(bytes, 13));
        r.setIY(readWord(bytes, 15));
        r.setIX(readWord(bytes, 17));

        int iff2 = bytes[19] & 0xFF;
        cpu.setIff1((iff2 & 0x04) != 0);
        cpu.setIff2((iff2 & 0x04) != 0);

        r.setR(bytes[20] & 0xFF);
        r.setAF(readWord(bytes, 21));
        r.setSP(readWord(bytes, 23));

        cpu.setIm(bytes[25] & 0x03);
        ula.setBorderColor(bytes[26] & 0x07);

        boolean is128k = bytes.length > 49179;

        if (!is128k) {
            // 48K Snapshot:
            // Bank 5 at 0x4000, Bank 2 at 0x8000, Bank 0 at 0xC000
            System.arraycopy(bytes, 27, memory.getRamBank(5), 0, 16384);
            System.arraycopy(bytes, 27 + 16384, memory.getRamBank(2), 0, 16384);
            System.arraycopy(bytes, 27 + 32768, memory.getRamBank(0), 0, 16384);

            memory.setActiveRamBank(0);
            memory.setActiveRomBank(1); // 48K BASIC ROM

            // For 48K SNA, PC was pushed onto the stack
            int sp = r.getSP();
            int pc = memory.readWord(sp);
            r.setSP((sp + 2) & 0xFFFF);
            r.setPC(pc);
        } else {
            // 128K Snapshot:
            int pagedBank = bytes[49181] & 0x07;
            System.arraycopy(bytes, 27, memory.getRamBank(5), 0, 16384);
            System.arraycopy(bytes, 27 + 16384, memory.getRamBank(2), 0, 16384);
            System.arraycopy(bytes, 27 + 32768, memory.getRamBank(pagedBank), 0, 16384);

            int pc = readWord(bytes, 49179);
            r.setPC(pc);

            int port7ffd = bytes[49181] & 0xFF;
            memory.writePort7ffd(port7ffd);

            // Remaining 5 banks in ascending order
            int offset = 49183;
            for (int b = 0; b < 8; b++) {
                if (b != 5 && b != 2 && b != pagedBank) {
                    if (offset + 16384 <= bytes.length) {
                        System.arraycopy(bytes, offset, memory.getRamBank(b), 0, 16384);
                        offset += 16384;
                    }
                }
            }
        }
    }

    public static void save(Path path, CpuState cpu, Spectrum128Memory memory, UlaDisplay ula, boolean is128k) throws IOException {
        var r = cpu.getRegisters();

        if (!is128k) {
            byte[] bytes = new byte[49179];

            // Push PC to stack for 48K SNA
            int sp = (r.getSP() - 2) & 0xFFFF;
            r.setSP(sp);
            memory.writeWord(sp, r.getPC());

            fillHeader(bytes, cpu, r, ula);

            System.arraycopy(memory.getRamBank(5), 0, bytes, 27, 16384);
            System.arraycopy(memory.getRamBank(2), 0, bytes, 27 + 16384, 16384);
            System.arraycopy(memory.getRamBank(0), 0, bytes, 27 + 32768, 16384);

            Files.write(path, bytes);
        } else {
            byte[] bytes = new byte[131103]; // 27 + 49152 + 4 + (5 * 16384)
            fillHeader(bytes, cpu, r, ula);

            int pagedBank = memory.getActiveRamBank();
            System.arraycopy(memory.getRamBank(5), 0, bytes, 27, 16384);
            System.arraycopy(memory.getRamBank(2), 0, bytes, 27 + 16384, 16384);
            System.arraycopy(memory.getRamBank(pagedBank), 0, bytes, 27 + 32768, 16384);

            writeWord(bytes, 49179, r.getPC());
            bytes[49181] = (byte) memory.getLastPort7ffd();
            bytes[49182] = 0; // TR-DOS

            int offset = 49183;
            for (int b = 0; b < 8; b++) {
                if (b != 5 && b != 2 && b != pagedBank) {
                    System.arraycopy(memory.getRamBank(b), 0, bytes, offset, 16384);
                    offset += 16384;
                }
            }
            Files.write(path, bytes);
        }
    }

    private static void fillHeader(byte[] bytes, CpuState cpu, Registers r, UlaDisplay ula) {
        bytes[0] = (byte) r.getI();
        writeWord(bytes, 1, r.getHLPrime());
        writeWord(bytes, 3, r.getDEPrime());
        writeWord(bytes, 5, r.getBCPrime());
        writeWord(bytes, 7, r.getAFPrime());

        writeWord(bytes, 9, r.getHL());
        writeWord(bytes, 11, r.getDE());
        writeWord(bytes, 13, r.getBC());
        writeWord(bytes, 15, r.getIY());
        writeWord(bytes, 17, r.getIX());

        bytes[19] = (byte) (cpu.isIff2() ? 0x04 : 0x00);
        bytes[20] = (byte) r.getR();
        writeWord(bytes, 21, r.getAF());
        writeWord(bytes, 23, r.getSP());

        bytes[25] = (byte) cpu.getIm();
        bytes[26] = (byte) ula.getBorderColor();
    }

    private static int readWord(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static void writeWord(byte[] data, int offset, int val) {
        data[offset] = (byte) (val & 0xFF);
        data[offset + 1] = (byte) ((val >> 8) & 0xFF);
    }
}
