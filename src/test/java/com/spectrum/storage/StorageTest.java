package com.spectrum.storage;

import com.spectrum.cpu.CpuState;
import com.spectrum.memory.Spectrum128Memory;
import com.spectrum.ula.UlaDisplay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StorageTest {

    @TempDir
    Path tempDir;

    private CpuState cpu;
    private Spectrum128Memory memory;
    private UlaDisplay ula;

    @BeforeEach
    void setUp() {
        cpu = new CpuState();
        memory = new Spectrum128Memory();
        ula = new UlaDisplay();
    }

    @Test
    void testTapBlockCreationAndParsing() throws IOException {
        byte[] payload = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        TapFileFormat.TapBlock block = TapFileFormat.createBlock(0xFF, payload);

        assertThat(block.isData()).isTrue();
        assertThat(block.getPayload()).containsExactly(payload);

        Path tapPath = tempDir.resolve("test.tap");
        TapFileFormat.save(tapPath, List.of(block));

        List<TapFileFormat.TapBlock> loaded = TapFileFormat.load(tapPath);
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getPayload()).containsExactly(payload);
    }

    @Test
    void testSna48kSaveAndLoad() throws IOException {
        var r = cpu.getRegisters();
        r.setPC(0x8000);
        r.setSP(0xFFFE);
        r.setA(0x42);
        r.setB(0x13);
        ula.setBorderColor(2); // Red border
        memory.writeByte(0x8000, 0xAA);

        Path snaPath = tempDir.resolve("test48k.sna");
        SnaSnapshot.save(snaPath, cpu, memory, ula, false);

        // Reset state
        cpu.reset();
        memory.clearRam();
        ula.setBorderColor(0);

        // Load back
        SnaSnapshot.load(snaPath, cpu, memory, ula);

        assertThat(cpu.getRegisters().getA()).isEqualTo(0x42);
        assertThat(cpu.getRegisters().getB()).isEqualTo(0x13);
        assertThat(cpu.getRegisters().getPC()).isEqualTo(0x8000);
        assertThat(ula.getBorderColor()).isEqualTo(2);
        assertThat(memory.readByte(0x8000)).isEqualTo(0xAA);
    }

    @Test
    void testSna128kSaveAndLoad() throws IOException {
        var r = cpu.getRegisters();
        r.setPC(0xC000);
        r.setSP(0xFAAA);
        r.setHL(0x1234);
        memory.writePort7ffd(0x03); // Page bank 3
        memory.writeByte(0xC000, 0x77);

        Path snaPath = tempDir.resolve("test128k.sna");
        SnaSnapshot.save(snaPath, cpu, memory, ula, true);

        // Reset state
        cpu.reset();
        memory.clearRam();
        memory.reset();

        // Load back
        SnaSnapshot.load(snaPath, cpu, memory, ula);

        assertThat(cpu.getRegisters().getHL()).isEqualTo(0x1234);
        assertThat(cpu.getRegisters().getPC()).isEqualTo(0xC000);
        assertThat(memory.getActiveRamBank()).isEqualTo(3);
        assertThat(memory.readByte(0xC000)).isEqualTo(0x77);
    }
}
