package com.spectrum.cpu;

/**
 * Sealed interface representing all decoded Z80 instructions as records.
 * Supports full standard and undocumented instruction sets.
 */
public sealed interface Instruction permits
    // Control & CPU State
    Instruction.Nop,
    Instruction.Halt,
    Instruction.Di,
    Instruction.Ei,
    Instruction.Im,
    Instruction.Scf,
    Instruction.Ccf,
    Instruction.Cpl,
    Instruction.Neg,
    Instruction.Daa,
    Instruction.ExDeHl,
    Instruction.ExAfAfPrime,
    Instruction.Exx,
    Instruction.ExSpHl,
    Instruction.ExSpIndex,

    // 8-bit Load
    Instruction.LdRegReg,
    Instruction.LdRegImm,
    Instruction.LdRegIndHl,
    Instruction.LdIndHlReg,
    Instruction.LdRegIndOffset,
    Instruction.LdIndOffsetReg,
    Instruction.LdIndHlImm,
    Instruction.LdIndOffsetImm,
    Instruction.LdAccIndBc,
    Instruction.LdAccIndDe,
    Instruction.LdIndBcAcc,
    Instruction.LdIndDeAcc,
    Instruction.LdAccInd16,
    Instruction.LdInd16Acc,
    Instruction.LdRegI,
    Instruction.LdRegR,
    Instruction.LdIReg,
    Instruction.LdRReg,

    // 16-bit Load & Stack
    Instruction.Ld16RegImm,
    Instruction.Ld16Ind16Reg,
    Instruction.Ld16RegInd16,
    Instruction.Ld16SpIndex,
    Instruction.Push,
    Instruction.Pop,

    // 8-bit Arithmetic / Logic
    Instruction.AluReg,
    Instruction.AluImm,
    Instruction.AluIndHl,
    Instruction.AluIndOffset,
    Instruction.Inc8Reg,
    Instruction.Inc8IndHl,
    Instruction.Inc8IndOffset,
    Instruction.Dec8Reg,
    Instruction.Dec8IndHl,
    Instruction.Dec8IndOffset,

    // 16-bit Arithmetic
    Instruction.Add16,
    Instruction.Adc16Hl,
    Instruction.Sbc16Hl,
    Instruction.Inc16,
    Instruction.Dec16,

    // Rotates and Shifts (including undocumented SLL)
    Instruction.RotateReg,
    Instruction.RotateIndHl,
    Instruction.RotateIndOffset,
    Instruction.RotateAcc,
    Instruction.Rld,
    Instruction.Rrd,

    // Bit operations & Undocumented DDCB/FDCB register write-back
    Instruction.BitReg,
    Instruction.BitIndHl,
    Instruction.BitIndOffset,
    Instruction.SetReg,
    Instruction.SetIndHl,
    Instruction.SetIndOffset,
    Instruction.ResReg,
    Instruction.ResIndHl,
    Instruction.ResIndOffset,
    Instruction.BitOpOffsetWithReg,

    // Jumps, Calls, Returns
    Instruction.JpImm,
    Instruction.JpCondImm,
    Instruction.JrImm,
    Instruction.JrCondImm,
    Instruction.JpIndIndex,
    Instruction.Djnz,
    Instruction.CallImm,
    Instruction.CallCondImm,
    Instruction.Ret,
    Instruction.RetCond,
    Instruction.Reti,
    Instruction.Retn,
    Instruction.Rst,

    // Input / Output
    Instruction.InAccImm,
    Instruction.InRegC,
    Instruction.InIndCNoReg,
    Instruction.OutImmAcc,
    Instruction.OutRegC,
    Instruction.OutCZero,

    // Block Instructions
    Instruction.Ldi, Instruction.Ldir,
    Instruction.Ldd, Instruction.Lddr,
    Instruction.Cpi, Instruction.Cpir,
    Instruction.Cpd, Instruction.Cpdr,
    Instruction.Ini, Instruction.Inir,
    Instruction.Ind, Instruction.Indr,
    Instruction.Outi, Instruction.Otir,
    Instruction.Outd, Instruction.Otdr
{
    int cycles();

    // --- Control & CPU State Records ---
    record Nop(int cycles) implements Instruction {}
    record Halt(int cycles) implements Instruction {}
    record Di(int cycles) implements Instruction {}
    record Ei(int cycles) implements Instruction {}
    record Im(int mode, int cycles) implements Instruction {}
    record Scf(int cycles) implements Instruction {}
    record Ccf(int cycles) implements Instruction {}
    record Cpl(int cycles) implements Instruction {}
    record Neg(int cycles) implements Instruction {}
    record Daa(int cycles) implements Instruction {}
    record ExDeHl(int cycles) implements Instruction {}
    record ExAfAfPrime(int cycles) implements Instruction {}
    record Exx(int cycles) implements Instruction {}
    record ExSpHl(int cycles) implements Instruction {}
    record ExSpIndex(Register16 indexReg, int cycles) implements Instruction {}

    // --- 8-bit Load Records ---
    record LdRegReg(Register8 dst, Register8 src, int cycles) implements Instruction {}
    record LdRegImm(Register8 dst, int imm, int cycles) implements Instruction {}
    record LdRegIndHl(Register8 dst, int cycles) implements Instruction {}
    record LdIndHlReg(Register8 src, int cycles) implements Instruction {}
    record LdRegIndOffset(Register8 dst, Register16 indexReg, int displacement, int cycles) implements Instruction {}
    record LdIndOffsetReg(Register16 indexReg, int displacement, Register8 src, int cycles) implements Instruction {}
    record LdIndHlImm(int imm, int cycles) implements Instruction {}
    record LdIndOffsetImm(Register16 indexReg, int displacement, int imm, int cycles) implements Instruction {}
    record LdAccIndBc(int cycles) implements Instruction {}
    record LdAccIndDe(int cycles) implements Instruction {}
    record LdIndBcAcc(int cycles) implements Instruction {}
    record LdIndDeAcc(int cycles) implements Instruction {}
    record LdAccInd16(int address, int cycles) implements Instruction {}
    record LdInd16Acc(int address, int cycles) implements Instruction {}
    record LdRegI(int cycles) implements Instruction {}
    record LdRegR(int cycles) implements Instruction {}
    record LdIReg(int cycles) implements Instruction {}
    record LdRReg(int cycles) implements Instruction {}

    // --- 16-bit Load & Stack Records ---
    record Ld16RegImm(Register16 dst, int imm16, int cycles) implements Instruction {}
    record Ld16Ind16Reg(int address, Register16 src, int cycles) implements Instruction {}
    record Ld16RegInd16(Register16 dst, int address, int cycles) implements Instruction {}
    record Ld16SpIndex(Register16 src, int cycles) implements Instruction {}
    record Push(Register16 reg, int cycles) implements Instruction {}
    record Pop(Register16 reg, int cycles) implements Instruction {}

    // --- 8-bit Arithmetic / Logic Records ---
    record AluReg(AluOp op, Register8 src, int cycles) implements Instruction {}
    record AluImm(AluOp op, int imm, int cycles) implements Instruction {}
    record AluIndHl(AluOp op, int cycles) implements Instruction {}
    record AluIndOffset(AluOp op, Register16 indexReg, int displacement, int cycles) implements Instruction {}
    record Inc8Reg(Register8 reg, int cycles) implements Instruction {}
    record Inc8IndHl(int cycles) implements Instruction {}
    record Inc8IndOffset(Register16 indexReg, int displacement, int cycles) implements Instruction {}
    record Dec8Reg(Register8 reg, int cycles) implements Instruction {}
    record Dec8IndHl(int cycles) implements Instruction {}
    record Dec8IndOffset(Register16 indexReg, int displacement, int cycles) implements Instruction {}

    // --- 16-bit Arithmetic Records ---
    record Add16(Register16 dst, Register16 src, int cycles) implements Instruction {}
    record Adc16Hl(Register16 src, int cycles) implements Instruction {}
    record Sbc16Hl(Register16 src, int cycles) implements Instruction {}
    record Inc16(Register16 reg, int cycles) implements Instruction {}
    record Dec16(Register16 reg, int cycles) implements Instruction {}

    // --- Rotates and Shifts Records ---
    record RotateReg(RotateOp op, Register8 reg, int cycles) implements Instruction {}
    record RotateIndHl(RotateOp op, int cycles) implements Instruction {}
    record RotateIndOffset(RotateOp op, Register16 indexReg, int displacement, int cycles) implements Instruction {}
    record RotateAcc(RotateAccOp op, int cycles) implements Instruction {}
    record Rld(int cycles) implements Instruction {}
    record Rrd(int cycles) implements Instruction {}

    // --- Bit operations & Undocumented DDCB/FDCB with register write-back ---
    record BitReg(int bit, Register8 reg, int cycles) implements Instruction {}
    record BitIndHl(int bit, int cycles) implements Instruction {}
    record BitIndOffset(int bit, Register16 indexReg, int displacement, int cycles) implements Instruction {}
    record SetReg(int bit, Register8 reg, int cycles) implements Instruction {}
    record SetIndHl(int bit, int cycles) implements Instruction {}
    record SetIndOffset(int bit, Register16 indexReg, int displacement, int cycles) implements Instruction {}
    record ResReg(int bit, Register8 reg, int cycles) implements Instruction {}
    record ResIndHl(int bit, int cycles) implements Instruction {}
    record ResIndOffset(int bit, Register16 indexReg, int displacement, int cycles) implements Instruction {}
    record BitOpOffsetWithReg(BitShiftOp op, RotateOp rotOp, int bit, Register16 indexReg, int displacement, Register8 targetReg, int cycles) implements Instruction {}

    // --- Jumps, Calls, Returns Records ---
    record JpImm(int target, int cycles) implements Instruction {}
    record JpCondImm(Condition cond, int target, int cycles, int takenCycles) implements Instruction {}
    record JrImm(int displacement, int cycles) implements Instruction {}
    record JrCondImm(Condition cond, int displacement, int cycles, int takenCycles) implements Instruction {}
    record JpIndIndex(Register16 indexReg, int cycles) implements Instruction {}
    record Djnz(int displacement, int cycles, int takenCycles) implements Instruction {}
    record CallImm(int target, int cycles) implements Instruction {}
    record CallCondImm(Condition cond, int target, int cycles, int takenCycles) implements Instruction {}
    record Ret(int cycles) implements Instruction {}
    record RetCond(Condition cond, int cycles, int takenCycles) implements Instruction {}
    record Reti(int cycles) implements Instruction {}
    record Retn(int cycles) implements Instruction {}
    record Rst(int target, int cycles) implements Instruction {}

    // --- Input / Output Records ---
    record InAccImm(int port, int cycles) implements Instruction {}
    record InRegC(Register8 reg, int cycles) implements Instruction {}
    record InIndCNoReg(int cycles) implements Instruction {}
    record OutImmAcc(int port, int cycles) implements Instruction {}
    record OutRegC(Register8 reg, int cycles) implements Instruction {}
    record OutCZero(int cycles) implements Instruction {}

    // --- Block Instructions Records ---
    record Ldi(int cycles) implements Instruction {}
    record Ldir(int cycles, int repeatCycles) implements Instruction {}
    record Ldd(int cycles) implements Instruction {}
    record Lddr(int cycles, int repeatCycles) implements Instruction {}
    record Cpi(int cycles) implements Instruction {}
    record Cpir(int cycles, int repeatCycles) implements Instruction {}
    record Cpd(int cycles) implements Instruction {}
    record Cpdr(int cycles, int repeatCycles) implements Instruction {}
    record Ini(int cycles) implements Instruction {}
    record Inir(int cycles, int repeatCycles) implements Instruction {}
    record Ind(int cycles) implements Instruction {}
    record Indr(int cycles, int repeatCycles) implements Instruction {}
    record Outi(int cycles) implements Instruction {}
    record Otir(int cycles, int repeatCycles) implements Instruction {}
    record Outd(int cycles) implements Instruction {}
    record Otdr(int cycles, int repeatCycles) implements Instruction {}
}
