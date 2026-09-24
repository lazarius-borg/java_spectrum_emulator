package nl.invokedynamic.spectrum.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles reading and writing ZX Spectrum .TZX tape container files.
 *
 * Supports TZX v1.20 specification including:
 * - Standard Speed Data Blocks (ID 0x10)
 * - Turbo Speed Data Blocks (ID 0x11)
 * - Pure Data Blocks (ID 0x14)
 * - Flow control, pause, group, text description, archive info, and custom info blocks.
 */
public final class TzxFileFormat {

    private static final byte[] TZX_SIGNATURE = new byte[] {
        'Z', 'X', 'T', 'a', 'p', 'e', '!', 0x1A
    };

    private TzxFileFormat() {}

    /**
     * Checks if the given byte array begins with the TZX file header ("ZXTape!\x1A").
     */
    public static boolean isTzx(byte[] bytes) {
        if (bytes == null || bytes.length < 10) return false;
        for (int i = 0; i < TZX_SIGNATURE.length; i++) {
            if (bytes[i] != TZX_SIGNATURE[i]) return false;
        }
        return true;
    }

    public static List<TapFileFormat.TapBlock> load(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return parse(bytes);
    }

    public static List<TapFileFormat.TapBlock> load(InputStream is) throws IOException {
        byte[] bytes = is.readAllBytes();
        return parse(bytes);
    }

    /**
     * Parses TZX data into standard TapBlocks compatible with instant loading and audio playback.
     */
    public static List<TapFileFormat.TapBlock> parse(byte[] bytes) throws IOException {
        if (!isTzx(bytes)) {
            throw new IOException("Invalid TZX header: missing 'ZXTape!\\x1A' signature");
        }

        List<TapFileFormat.TapBlock> blocks = new ArrayList<>();
        int offset = 10; // 8 bytes signature + 1 byte major ver + 1 byte minor ver

        while (offset < bytes.length) {
            int blockId = bytes[offset++] & 0xFF;

            switch (blockId) {
                // 0x10: Standard Speed Data Block
                case 0x10 -> {
                    if (offset + 4 > bytes.length) break;
                    int pauseMs = readWordLE(bytes, offset);
                    int dataLen = readWordLE(bytes, offset + 2);
                    offset += 4;

                    if (offset + dataLen > bytes.length) break;
                    byte[] data = Arrays.copyOfRange(bytes, offset, offset + dataLen);
                    offset += dataLen;

                    if (data.length >= 2) {
                        int flag = data[0] & 0xFF;
                        int checksum = data[data.length - 1] & 0xFF;
                        blocks.add(new TapFileFormat.TapBlock(flag, data, checksum));
                    }
                }

                // 0x11: Turbo Speed Data Block
                case 0x11 -> {
                    if (offset + 18 > bytes.length) break;
                    // 2 bytes pilot, 2 sync1, 2 sync2, 2 zero, 2 one, 2 pilot tone, 1 last bits, 2 pause
                    int dataLen = readTripleLE(bytes, offset + 15);
                    offset += 18;

                    if (offset + dataLen > bytes.length) break;
                    byte[] data = Arrays.copyOfRange(bytes, offset, offset + dataLen);
                    offset += dataLen;

                    if (data.length >= 2) {
                        int flag = data[0] & 0xFF;
                        int checksum = data[data.length - 1] & 0xFF;
                        blocks.add(new TapFileFormat.TapBlock(flag, data, checksum));
                    }
                }

                // 0x12: Pure Tone
                case 0x12 -> offset += 4;

                // 0x13: Pulse Sequence
                case 0x13 -> {
                    if (offset >= bytes.length) break;
                    int numPulses = bytes[offset++] & 0xFF;
                    offset += numPulses * 2;
                }

                // 0x14: Pure Data Block
                case 0x14 -> {
                    if (offset + 10 > bytes.length) break;
                    int dataLen = readTripleLE(bytes, offset + 7);
                    offset += 10;

                    if (offset + dataLen > bytes.length) break;
                    byte[] data = Arrays.copyOfRange(bytes, offset, offset + dataLen);
                    offset += dataLen;

                    if (data.length >= 2) {
                        int flag = data[0] & 0xFF;
                        int checksum = data[data.length - 1] & 0xFF;
                        blocks.add(new TapFileFormat.TapBlock(flag, data, checksum));
                    }
                }

                // 0x15: Direct Recording
                case 0x15 -> {
                    if (offset + 8 > bytes.length) break;
                    int dataLen = readTripleLE(bytes, offset + 5);
                    offset += 8 + dataLen;
                }

                // 0x18: CSW Recording
                case 0x18 -> {
                    if (offset + 4 > bytes.length) break;
                    int blockLen = readDwordLE(bytes, offset);
                    offset += 4 + blockLen;
                }

                // 0x19: Generalized Data Block
                case 0x19 -> {
                    if (offset + 4 > bytes.length) break;
                    int blockLen = readDwordLE(bytes, offset);
                    offset += 4 + blockLen;
                }

                // 0x20: Pause or Stop the Tape
                case 0x20 -> offset += 2;

                // 0x21: Group Start
                case 0x21 -> {
                    if (offset >= bytes.length) break;
                    int len = bytes[offset++] & 0xFF;
                    offset += len;
                }

                // 0x22: Group End
                case 0x22 -> {}

                // 0x23: Jump To Block
                case 0x23 -> offset += 2;

                // 0x24: Loop Start
                case 0x24 -> offset += 2;

                // 0x25: Loop End
                case 0x25 -> {}

                // 0x26: Call Sequence
                case 0x26 -> {
                    if (offset + 2 > bytes.length) break;
                    int numCalls = readWordLE(bytes, offset);
                    offset += 2 + numCalls * 2;
                }

                // 0x27: Return From Sequence
                case 0x27 -> {}

                // 0x28: Select Block
                case 0x28 -> {
                    if (offset + 2 > bytes.length) break;
                    int blockLen = readWordLE(bytes, offset);
                    offset += 2 + blockLen;
                }

                // 0x2A: Stop the tape if in 48K mode
                case 0x2A -> offset += 4;

                // 0x2B: Set signal level
                case 0x2B -> offset += 5;

                // 0x30: Text Description
                case 0x30 -> {
                    if (offset >= bytes.length) break;
                    int len = bytes[offset++] & 0xFF;
                    offset += len;
                }

                // 0x31: Message Block
                case 0x31 -> {
                    if (offset + 2 > bytes.length) break;
                    offset++; // time in seconds
                    int len = bytes[offset++] & 0xFF;
                    offset += len;
                }

                // 0x32: Archive Info
                case 0x32 -> {
                    if (offset + 2 > bytes.length) break;
                    int blockLen = readWordLE(bytes, offset);
                    offset += 2 + blockLen;
                }

                // 0x33: Hardware Type
                case 0x33 -> {
                    if (offset >= bytes.length) break;
                    int numMachines = bytes[offset++] & 0xFF;
                    offset += numMachines * 3;
                }

                // 0x35: Custom Info Block
                case 0x35 -> {
                    if (offset + 20 > bytes.length) break;
                    offset += 16; // Identification string
                    int infoLen = readDwordLE(bytes, offset);
                    offset += 4 + infoLen;
                }

                // 0x5A: Glue Block
                case 0x5A -> offset += 9;

                default -> {
                    // Unknown block: cannot determine length, terminate parsing safely
                    return blocks;
                }
            }
        }

        return blocks;
    }

    /**
     * Saves standard TapBlocks as a valid TZX v1.20 file.
     */
    public static void save(Path path, List<TapFileFormat.TapBlock> blocks) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1. Write Header: "ZXTape!\x1A\x01\x14"
        out.write(TZX_SIGNATURE);
        out.write(1);  // Major version 1
        out.write(20); // Minor version 20 (v1.20)

        // 2. Write each block as a Standard Speed Data Block (0x10)
        for (TapFileFormat.TapBlock block : blocks) {
            byte[] data = block.data();
            out.write(0x10); // Block ID

            // 1000 ms pause after block
            out.write(0xE8);
            out.write(0x03);

            // Data length
            int len = data.length;
            out.write(len & 0xFF);
            out.write((len >> 8) & 0xFF);

            // Data bytes
            out.write(data);
        }

        Files.write(path, out.toByteArray());
    }

    private static int readWordLE(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8);
    }

    private static int readTripleLE(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8) | ((b[offset + 2] & 0xFF) << 16);
    }

    private static int readDwordLE(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8)
             | ((b[offset + 2] & 0xFF) << 16) | ((b[offset + 3] & 0xFF) << 24);
    }
}
