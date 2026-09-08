package com.spectrum.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading and writing ZX Spectrum .TAP tape container files.
 *
 * Each block in a .TAP file consists of:
 * - 2-byte little-endian length N
 * - N bytes of data (including 1-byte flag and 1-byte XOR checksum).
 */
public final class TapFileFormat {

    public record TapBlock(int flag, byte[] data, int checksum) {
        public byte[] getPayload() {
            if (data.length <= 2) return new byte[0];
            byte[] payload = new byte[data.length - 2];
            System.arraycopy(data, 1, payload, 0, payload.length);
            return payload;
        }

        public boolean isHeader() {
            return flag == 0x00;
        }

        public boolean isData() {
            return flag == 0xFF;
        }
    }

    public static List<TapBlock> load(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return parse(bytes);
    }

    public static List<TapBlock> parse(byte[] bytes) {
        List<TapBlock> blocks = new ArrayList<>();
        int offset = 0;

        while (offset + 2 <= bytes.length) {
            int len = (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
            offset += 2;

            if (offset + len > bytes.length) {
                break; // Corrupted or truncated block
            }

            byte[] blockData = new byte[len];
            System.arraycopy(bytes, offset, blockData, 0, len);
            offset += len;

            if (len >= 2) {
                int flag = blockData[0] & 0xFF;
                int checksum = blockData[len - 1] & 0xFF;
                blocks.add(new TapBlock(flag, blockData, checksum));
            }
        }
        return blocks;
    }

    public static void save(Path path, List<TapBlock> blocks) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (TapBlock block : blocks) {
            byte[] data = block.data();
            int len = data.length;
            out.write(len & 0xFF);
            out.write((len >> 8) & 0xFF);
            out.write(data);
        }
        Files.write(path, out.toByteArray());
    }

    public static TapBlock createBlock(int flag, byte[] payload) {
        byte[] data = new byte[payload.length + 2];
        data[0] = (byte) flag;
        System.arraycopy(payload, 0, data, 1, payload.length);

        int checksum = flag;
        for (byte b : payload) {
            checksum ^= (b & 0xFF);
        }
        data[data.length - 1] = (byte) checksum;

        return new TapBlock(flag, data, checksum);
    }
}
