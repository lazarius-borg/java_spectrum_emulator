package nl.invokedynamic.spectrum.storage;

import nl.invokedynamic.spectrum.cpu.CpuState;
import nl.invokedynamic.spectrum.cpu.Registers;
import nl.invokedynamic.spectrum.memory.Spectrum128Memory;
import nl.invokedynamic.spectrum.sound.Ay38912;
import nl.invokedynamic.spectrum.ula.UlaDisplay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads .Z80 snapshot files supporting Version 1 (48K), Version 2, and Version 3 (128K).
 */
public final class Z80Snapshot {

    public static void load(Path path, CpuState cpu, Spectrum128Memory memory, UlaDisplay ula, Ay38912 psg) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        loadFromBytes(bytes, cpu, memory, ula, psg);
    }

    public static void loadFromBytes(byte[] bytes, CpuState cpu, Spectrum128Memory memory, UlaDisplay ula, Ay38912 psg) {
        if (bytes.length < 30) {
            throw new IllegalArgumentException("Invalid Z80 file: size too small");
        }

        var r = cpu.getRegisters();

        // 1. Primary Header (30 bytes)
        r.setA(bytes[0] & 0xFF);
        r.setF(bytes[1] & 0xFF);
        r.setBC(readWord(bytes, 2));
        r.setHL(readWord(bytes, 4));
        int pcV1 = readWord(bytes, 6);
        r.setSP(readWord(bytes, 8));
        r.setI(bytes[10] & 0xFF);

        int rLow = bytes[11] & 0x7F;
        int flags12 = bytes[12] & 0xFF;
        int rHigh = (flags12 & 0x01) << 7;
        r.setR(rLow | rHigh);

        ula.setBorderColor((flags12 >> 1) & 0x07);
        boolean compressedV1 = (flags12 & 0x20) != 0;

        r.setDE(readWord(bytes, 13));
        r.setBCPrime(readWord(bytes, 15));
        r.setDEPrime(readWord(bytes, 17));
        r.setHLPrime(readWord(bytes, 19));
        r.setAPrime(bytes[21] & 0xFF);
        r.setFPrime(bytes[22] & 0xFF);
        r.setIY(readWord(bytes, 23));
        r.setIX(readWord(bytes, 25));

        cpu.setIff1(bytes[27] != 0);
        cpu.setIff2(bytes[28] != 0);
        cpu.setIm(bytes[29] & 0x03);

        if (pcV1 != 0) {
            // Version 1 snapshot (48K)
            r.setPC(pcV1);
            memory.setActiveRamBank(0);
            memory.setActiveRomBank(1);

            byte[] ram48k = new byte[49152];
            if (compressedV1) {
                decompressRle(bytes, 30, bytes.length - 30, ram48k, 0, 49152);
            } else {
                System.arraycopy(bytes, 30, ram48k, 0, Math.min(49152, bytes.length - 30));
            }

            // Copy to Bank 5 (0x4000), Bank 2 (0x8000), Bank 0 (0xC000)
            System.arraycopy(ram48k, 0, memory.getRamBank(5), 0, 16384);
            System.arraycopy(ram48k, 16384, memory.getRamBank(2), 0, 16384);
            System.arraycopy(ram48k, 32768, memory.getRamBank(0), 0, 16384);
        } else {
            // Version 2 or 3 snapshot
            int extHeaderLen = readWord(bytes, 30);
            int realPc = readWord(bytes, 32);
            r.setPC(realPc);

            int hwMode = bytes[34] & 0xFF;
            boolean is128k = (hwMode == 3 || hwMode == 4);

            int port7ffd = bytes[35] & 0xFF;
            if (is128k) {
                memory.writePort7ffd(port7ffd);
            }

            // Restore AY registers if present
            if (extHeaderLen >= 23 && psg != null) {
                for (int i = 0; i < 16; i++) {
                    psg.selectRegister(i);
                    psg.writeData(bytes[39 + i] & 0xFF);
                }
            }

            // Parse banked memory blocks
            int offset = 32 + extHeaderLen;
            while (offset + 3 <= bytes.length) {
                int blockLen = readWord(bytes, offset);
                int page = bytes[offset + 2] & 0xFF;
                offset += 3;

                boolean uncompressed = (blockLen == 0xFFFF);
                int actualLen = uncompressed ? 16384 : blockLen;

                byte[] destBank = mapZ80PageToRamBank(page, memory);
                if (destBank != null) {
                    if (uncompressed) {
                        System.arraycopy(bytes, offset, destBank, 0, Math.min(16384, bytes.length - offset));
                    } else {
                        decompressRle(bytes, offset, actualLen, destBank, 0, 16384);
                    }
                }
                offset += actualLen;
            }
        }
    }

    private static byte[] mapZ80PageToRamBank(int page, Spectrum128Memory memory) {
        // Z80 snapshot page mapping for 128K:
        // Page 3..10 map to RAM Bank 0..7
        // Page 4: Bank 5 (in 48K) or Bank 1 (in 128K)
        // Page 5: Bank 2 (in 48K) or Bank 2 (in 128K)
        // Page 8: Bank 5
        return switch (page) {
            case 3 -> memory.getRamBank(0);
            case 4 -> memory.getRamBank(1);
            case 5 -> memory.getRamBank(2);
            case 6 -> memory.getRamBank(3);
            case 7 -> memory.getRamBank(4);
            case 8 -> memory.getRamBank(5);
            case 9 -> memory.getRamBank(6);
            case 10 -> memory.getRamBank(7);
            default -> null;
        };
    }

    private static void decompressRle(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset, int maxDstLen) {
        int srcEnd = srcOffset + srcLen;
        int dstIdx = dstOffset;
        int dstEnd = dstOffset + maxDstLen;

        while (srcOffset < srcEnd && dstIdx < dstEnd) {
            if (srcOffset + 3 < srcEnd
                    && (src[srcOffset] & 0xFF) == 0xED
                    && (src[srcOffset + 1] & 0xFF) == 0xED) {
                int count = src[srcOffset + 2] & 0xFF;
                byte val = src[srcOffset + 3];
                srcOffset += 4;
                for (int i = 0; i < count && dstIdx < dstEnd; i++) {
                    dst[dstIdx++] = val;
                }
            } else {
                dst[dstIdx++] = src[srcOffset++];
            }
        }
    }

    private static int readWord(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
}
