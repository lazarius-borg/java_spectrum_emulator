package nl.invokedynamic.spectrum.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UndocumentedOpcodeTest {

    private Z80Cpu cpu;
    private Z80InstructionRecordTest.SimpleMemory memory;
    private Z80InstructionRecordTest.SimpleIo io;

    @BeforeEach
    void setUp() {
        cpu = new Z80Cpu();
        memory = new Z80InstructionRecordTest.SimpleMemory();
        io = new Z80InstructionRecordTest.SimpleIo();
    }

    @Test
    void testUndocumentedSllInstruction() {
        var r = cpu.getState().getRegisters();
        r.setB(0b10000001); // 129

        // SLL B -> opcode: CB 30
        memory.writeByte(0x0000, 0xCB);
        memory.writeByte(0x0001, 0x30);

        int cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(8);

        // SLL shifts left, bit 7 goes to Carry (1), and bit 0 is set to 1!
        // 0b10000001 << 1 with bit 0 set = 0b00000011 (3)
        assertThat(r.getB()).isEqualTo(0b00000011);
        assertThat(r.getF() & Flags.C_MASK).isEqualTo(Flags.C_MASK);
    }

    @Test
    void testUndocumentedIxhIxlOperations() {
        var r = cpu.getState().getRegisters();
        r.setIX(0x1234);

        // INC IXH -> opcode: DD 24
        memory.writeByte(0x0000, 0xDD);
        memory.writeByte(0x0001, 0x24);

        int cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(8);
        assertThat(r.getIXH()).isEqualTo(0x13);
        assertThat(r.getIX()).isEqualTo(0x1334);

        // LD IXL, 0xAA -> opcode: DD 2E AA
        memory.writeByte(0x0002, 0xDD);
        memory.writeByte(0x0003, 0x2E);
        memory.writeByte(0x0004, 0xAA);

        cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(11);
        assertThat(r.getIXL()).isEqualTo(0xAA);
        assertThat(r.getIX()).isEqualTo(0x13AA);
    }

    @Test
    void testUndocumentedDdcbBitOpWithRegisterWriteBack() {
        var r = cpu.getState().getRegisters();
        r.setIX(0xC000);
        memory.writeByte(0xC005, 0b00000001);

        // Opcode: DD CB 05 00
        // RLC (IX+5) AND store result in register B!
        memory.writeByte(0x0000, 0xDD);
        memory.writeByte(0x0001, 0xCB);
        memory.writeByte(0x0002, 0x05); // displacement d = +5
        memory.writeByte(0x0003, 0x00); // RLC (IX+d) + LD B, (IX+d)

        int cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(23);

        // Memory at 0xC005 should have RLC 0b00000001 = 0b00000010
        assertThat(memory.readByte(0xC005)).isEqualTo(0b00000010);
        // Register B should also be updated with 0b00000010!
        assertThat(r.getB()).isEqualTo(0b00000010);
    }

    @Test
    void testUndocumentedEd70InCNoRegister() {
        var r = cpu.getState().getRegisters();
        r.setBC(0x1234);

        // ED 70: IN (C) - tests flags without saving to register
        memory.writeByte(0x0000, 0xED);
        memory.writeByte(0x0001, 0x70);

        int cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(12);
        // Flags S, Z, P/V set based on input byte 0xFF
        assertThat(r.getF() & Flags.S_MASK).isNotZero(); // 0xFF has bit 7 set
        assertThat(r.getF() & Flags.Z_MASK).isZero();
    }
}
