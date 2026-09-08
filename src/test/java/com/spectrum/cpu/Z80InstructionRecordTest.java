package com.spectrum.cpu;

import com.spectrum.io.IoBus;
import com.spectrum.memory.MemoryBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Z80InstructionRecordTest {

    private Z80Cpu cpu;
    private SimpleMemory memory;
    private SimpleIo io;

    @BeforeEach
    void setUp() {
        cpu = new Z80Cpu();
        memory = new SimpleMemory();
        io = new SimpleIo();
    }

    @Test
    void testNop() {
        memory.writeByte(0x0000, 0x00); // NOP
        int cycles = cpu.step(memory, io);

        assertThat(cycles).isEqualTo(4);
        assertThat(cpu.getState().getRegisters().getPC()).isEqualTo(0x0001);
    }

    @Test
    void testLdRegImmAndRegReg() {
        // LD A, 0x42
        memory.writeByte(0x0000, 0x3E);
        memory.writeByte(0x0001, 0x42);
        // LD B, A
        memory.writeByte(0x0002, 0x47);

        int cycles1 = cpu.step(memory, io);
        assertThat(cycles1).isEqualTo(7);
        assertThat(cpu.getState().getRegisters().getA()).isEqualTo(0x42);

        int cycles2 = cpu.step(memory, io);
        assertThat(cycles2).isEqualTo(4);
        assertThat(cpu.getState().getRegisters().getB()).isEqualTo(0x42);
    }

    @Test
    void testAddAndFlags() {
        var r = cpu.getState().getRegisters();
        r.setA(0x0F);
        r.setF(0);

        // ADD A, 0x01
        memory.writeByte(0x0000, 0xC6);
        memory.writeByte(0x0001, 0x01);

        int cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(7);
        assertThat(r.getA()).isEqualTo(0x10);
        // Half carry should be set
        assertThat(r.getF() & Flags.H_MASK).isNotZero();
        // Carry should be 0
        assertThat(r.getF() & Flags.C_MASK).isZero();
    }

    @Test
    void testSubAndZeroFlag() {
        var r = cpu.getState().getRegisters();
        r.setA(0x10);

        // SUB 0x10
        memory.writeByte(0x0000, 0xD6);
        memory.writeByte(0x0001, 0x10);

        cpu.step(memory, io);
        assertThat(r.getA()).isEqualTo(0);
        assertThat(r.getF() & Flags.Z_MASK).isNotZero();
        assertThat(r.getF() & Flags.N_MASK).isNotZero(); // N flag set on subtract
    }

    @Test
    void testJrCondTakenAndNotTaken() {
        var r = cpu.getState().getRegisters();
        r.setF(0); // Zero flag is clear

        // JR NZ, +5
        memory.writeByte(0x0000, 0x20);
        memory.writeByte(0x0001, 0x05);

        int cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(12); // Condition taken: 12 T-states
        assertThat(r.getPC()).isEqualTo(0x0007); // 2 bytes instruction + 5

        // Reset and test not taken
        r.setPC(0x0000);
        r.setF(Flags.Z_MASK); // Zero flag is now set
        cycles = cpu.step(memory, io);
        assertThat(cycles).isEqualTo(7); // Condition not taken: 7 T-states
        assertThat(r.getPC()).isEqualTo(0x0002);
    }

    @Test
    void testPushAndPop() {
        var r = cpu.getState().getRegisters();
        r.setSP(0x8000);
        r.setHL(0x1234);

        // PUSH HL
        memory.writeByte(0x0000, 0xE5);
        // POP BC
        memory.writeByte(0x0001, 0xC1);

        int c1 = cpu.step(memory, io);
        assertThat(c1).isEqualTo(11);
        assertThat(r.getSP()).isEqualTo(0x7FFE);
        assertThat(memory.readWord(0x7FFE)).isEqualTo(0x1234);

        int c2 = cpu.step(memory, io);
        assertThat(c2).isEqualTo(10);
        assertThat(r.getSP()).isEqualTo(0x8000);
        assertThat(r.getBC()).isEqualTo(0x1234);
    }

    // Helper classes for testing
    public static class SimpleMemory implements MemoryBus {
        private final byte[] ram = new byte[65536];

        @Override
        public int readByte(int address) {
            return ram[address & 0xFFFF] & 0xFF;
        }

        @Override
        public void writeByte(int address, int value) {
            ram[address & 0xFFFF] = (byte) value;
        }
    }

    public static class SimpleIo implements IoBus {
        private int lastPort;
        private int lastValue;

        @Override
        public int in(int port) {
            return 0xFF;
        }

        @Override
        public void out(int port, int value) {
            this.lastPort = port;
            this.lastValue = value;
        }

        public int getLastPort() { return lastPort; }
        public int getLastValue() { return lastValue; }
    }
}
