package nl.invokedynamic.spectrum.cpu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DaaInstructionTest {

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
    @DisplayName("DAA after addition: 0x15 + 0x27 = 0x3C -> DAA adjusts to 0x42 (H set)")
    void testDaaAdditionLowNibbleCorrection() {
        var r = cpu.getState().getRegisters();
        r.setA(0x15);
        // ADD A, 0x27 (0xC6 0x27)
        memory.writeByte(0x0000, 0xC6);
        memory.writeByte(0x0001, 0x27);
        // DAA (0x27)
        memory.writeByte(0x0002, 0x27);

        cpu.step(memory, io); // ADD A, 0x27 -> A = 0x3C, N=0, H=0, C=0
        assertThat(r.getA()).isEqualTo(0x3C);

        cpu.step(memory, io); // DAA -> A = 0x42, H=1, C=0
        assertThat(r.getA()).isEqualTo(0x42);
        assertThat(r.getF() & Flags.C_MASK).isZero();
        assertThat(r.getF() & Flags.H_MASK).isEqualTo(Flags.H_MASK);
        assertThat(r.getF() & Flags.N_MASK).isZero();
    }

    @Test
    @DisplayName("DAA after addition: 0x99 + 0x01 = 0x9A -> DAA adjusts to 0x00 (C set, Z set)")
    void testDaaAdditionDoubleNibbleCorrection() {
        var r = cpu.getState().getRegisters();
        r.setA(0x99);
        // ADD A, 0x01
        memory.writeByte(0x0000, 0xC6);
        memory.writeByte(0x0001, 0x01);
        // DAA
        memory.writeByte(0x0002, 0x27);

        cpu.step(memory, io); // ADD
        assertThat(r.getA()).isEqualTo(0x9A);

        cpu.step(memory, io); // DAA -> A = 0x00, C=1, Z=1
        assertThat(r.getA()).isEqualTo(0x00);
        assertThat(r.getF() & Flags.C_MASK).isEqualTo(Flags.C_MASK);
        assertThat(r.getF() & Flags.Z_MASK).isEqualTo(Flags.Z_MASK);
    }

    @Test
    @DisplayName("DAA after subtraction: 0x42 - 0x27 = 0x1B (H=1, N=1) -> DAA adjusts to 0x15")
    void testDaaSubtractionLowNibbleCorrection() {
        var r = cpu.getState().getRegisters();
        r.setA(0x42);
        // SUB 0x27 (0xD6 0x27)
        memory.writeByte(0x0000, 0xD6);
        memory.writeByte(0x0001, 0x27);
        // DAA
        memory.writeByte(0x0002, 0x27);

        cpu.step(memory, io); // SUB -> A = 0x1B, H=1, N=1
        assertThat(r.getA()).isEqualTo(0x1B);
        assertThat(r.getF() & Flags.H_MASK).isEqualTo(Flags.H_MASK);
        assertThat(r.getF() & Flags.N_MASK).isEqualTo(Flags.N_MASK);

        cpu.step(memory, io); // DAA -> A = 0x15, H=0, N=1
        assertThat(r.getA()).isEqualTo(0x15);
        assertThat(r.getF() & Flags.C_MASK).isZero();
        assertThat(r.getF() & Flags.H_MASK).isZero();
        assertThat(r.getF() & Flags.N_MASK).isEqualTo(Flags.N_MASK);
    }

    @Test
    @DisplayName("SCF, CCF, CPL flag transitions")
    void testScfCcfCplFlags() {
        var r = cpu.getState().getRegisters();
        r.setF(0);
        r.setA(0x55);

        // SCF (0x37)
        memory.writeByte(0x0000, 0x37);
        cpu.step(memory, io);
        assertThat(r.getF() & Flags.C_MASK).isEqualTo(Flags.C_MASK);
        assertThat(r.getF() & Flags.H_MASK).isZero();
        assertThat(r.getF() & Flags.N_MASK).isZero();

        // CCF (0x3F) with C=1 -> should invert C to 0, and H should receive previous C (1)
        memory.writeByte(0x0001, 0x3F);
        cpu.step(memory, io);
        assertThat(r.getF() & Flags.C_MASK).isZero();
        assertThat(r.getF() & Flags.H_MASK).isEqualTo(Flags.H_MASK);
        assertThat(r.getF() & Flags.N_MASK).isZero();

        // CPL (0x2F) -> inverts A, sets H and N
        memory.writeByte(0x0002, 0x2F);
        cpu.step(memory, io);
        assertThat(r.getA()).isEqualTo(0xAA);
        assertThat(r.getF() & Flags.H_MASK).isEqualTo(Flags.H_MASK);
        assertThat(r.getF() & Flags.N_MASK).isEqualTo(Flags.N_MASK);
    }

    @Test
    @DisplayName("Verify ZEXDOC <daa,cpl,scf,ccf> exerciser group in sub-second test")
    void testZexdocDaaExerciserGroup() throws java.io.IOException {
        var result = CpmTestHarness.runDaaOnly("/cpm/zexdoc.com", true);
        assertThat(result.completed()).isTrue();
        assertThat(result.failedTests()).isEmpty();
        assertThat(result.passedTests()).anyMatch(s -> s.contains("<daa,cpl,scf,ccf>") && s.contains("OK"));
    }

    @Test
    @DisplayName("Verify ZEXALL <daa,cpl,scf,ccf> exerciser group in sub-second test")
    void testZexallDaaExerciserGroup() throws java.io.IOException {
        var result = CpmTestHarness.runDaaOnly("/cpm/zexall.com", true);
        assertThat(result.completed()).isTrue();
        assertThat(result.failedTests()).isEmpty();
        assertThat(result.passedTests()).anyMatch(s -> s.contains("<daa,cpl,scf,ccf>") && s.contains("OK"));
    }
}
