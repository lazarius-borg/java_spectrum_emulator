package com.spectrum.cpu;

import com.spectrum.memory.Spectrum128Memory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DisassemblerTest {

    @Test
    void testDisassemblyBasicInstructions() {
        Spectrum128Memory memory = new Spectrum128Memory();

        // 0x8000: NOP (0x00)
        // 0x8001: LD A, 0x42 (0x3E 0x42)
        // 0x8003: ADD A, B (0x80)
        // 0x8004: RET (0xC9)
        memory.writeByte(0x8000, 0x00);
        memory.writeByte(0x8001, 0x3E);
        memory.writeByte(0x8002, 0x42);
        memory.writeByte(0x8003, 0x80);
        memory.writeByte(0x8004, 0xC9);

        List<Disassembler.DisassembledInstruction> lines = Disassembler.disassemble(memory, 0x8000, 4);
        assertThat(lines).hasSize(4);

        assertThat(lines.get(0).address()).isEqualTo(0x8000);
        assertThat(lines.get(0).mnemonic()).contains("NOP");

        assertThat(lines.get(1).address()).isEqualTo(0x8001);
        assertThat(lines.get(1).mnemonic()).contains("LD   A, 0x42");

        assertThat(lines.get(2).address()).isEqualTo(0x8003);
        assertThat(lines.get(2).mnemonic()).contains("ADD");

        assertThat(lines.get(3).address()).isEqualTo(0x8004);
        assertThat(lines.get(3).mnemonic()).contains("RET");
    }
}
