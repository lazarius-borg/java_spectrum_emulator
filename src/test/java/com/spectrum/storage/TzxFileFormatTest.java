package com.spectrum.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TzxFileFormatTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoadTzxRoundTrip() throws IOException {
        Path tzxPath = tempDir.resolve("test_roundtrip.tzx");

        // Create sample header and data blocks
        byte[] payload1 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04};
        TapFileFormat.TapBlock block1 = TapFileFormat.createBlock(0x00, payload1);

        byte[] payload2 = new byte[] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
        TapFileFormat.TapBlock block2 = TapFileFormat.createBlock(0xFF, payload2);

        List<TapFileFormat.TapBlock> originalBlocks = List.of(block1, block2);

        // Save as TZX
        TzxFileFormat.save(tzxPath, originalBlocks);

        // Verify file is marked as TZX
        byte[] fileBytes = Files.readAllBytes(tzxPath);
        assertThat(TzxFileFormat.isTzx(fileBytes)).isTrue();

        // Load back via TzxFileFormat
        List<TapFileFormat.TapBlock> loadedBlocks = TzxFileFormat.load(tzxPath);
        assertThat(loadedBlocks).hasSize(2);

        assertThat(loadedBlocks.get(0).flag()).isEqualTo(0x00);
        assertThat(loadedBlocks.get(0).getPayload()).isEqualTo(payload1);
        assertThat(loadedBlocks.get(0).checksum()).isEqualTo(block1.checksum());

        assertThat(loadedBlocks.get(1).flag()).isEqualTo(0xFF);
        assertThat(loadedBlocks.get(1).getPayload()).isEqualTo(payload2);
        assertThat(loadedBlocks.get(1).checksum()).isEqualTo(block2.checksum());

        // Load via unified TapeFormat
        List<TapFileFormat.TapBlock> unifiedLoaded = TapeFormat.load(tzxPath);
        assertThat(unifiedLoaded).hasSize(2);
        assertThat(unifiedLoaded.get(0).getPayload()).isEqualTo(payload1);
    }

    @Test
    void testParseTzxWithMetadataBlocks() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1. TZX Header: "ZXTape!\x1A\x01\x14"
        out.write(new byte[] {'Z', 'X', 'T', 'a', 'p', 'e', '!', 0x1A, 1, 20});

        // 2. Block 0x30: Text Description ("Sample Game")
        out.write(0x30);
        byte[] text = "Sample Game".getBytes();
        out.write(text.length);
        out.write(text);

        // 3. Block 0x20: Pause 1000ms
        out.write(0x20);
        out.write(0xE8);
        out.write(0x03);

        // 4. Block 0x10: Standard Speed Data (1-byte flag 0x00, 2 bytes payload, 1 byte checksum)
        out.write(0x10);
        out.write(0xE8); out.write(0x03); // pause 1000ms
        out.write(4); out.write(0);       // data length 4
        out.write(new byte[] {0x00, 0x12, 0x34, 0x26}); // flag 00, data 12 34, chk 26

        // 5. Block 0x32: Archive Info
        out.write(0x32);
        out.write(4); out.write(0); // block length 4
        out.write(new byte[] {0x00, 0x01, 0x02, 0x03});

        // 6. Block 0x11: Turbo Speed Data Block
        out.write(0x11);
        // 15 header bytes:
        out.write(new byte[15]);
        // 3 bytes data length: 3 bytes (0x03 0x00 0x00)
        out.write(3); out.write(0); out.write(0);
        out.write(new byte[] {(byte) 0xFF, 0x42, (byte) 0xBD}); // flag FF, data 42, chk BD

        byte[] tzxData = out.toByteArray();
        List<TapFileFormat.TapBlock> blocks = TzxFileFormat.parse(tzxData);

        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).flag()).isEqualTo(0x00);
        assertThat(blocks.get(0).checksum()).isEqualTo(0x26);

        assertThat(blocks.get(1).flag()).isEqualTo(0xFF);
        assertThat(blocks.get(1).checksum()).isEqualTo(0xBD);
    }

    @Test
    void testInvalidTzxHeaderThrows() {
        byte[] invalidData = new byte[] {'B', 'A', 'D', 'T', 'A', 'P', 'E', '!'};
        assertThatThrownBy(() -> TzxFileFormat.parse(invalidData))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Invalid TZX header");
    }
}
