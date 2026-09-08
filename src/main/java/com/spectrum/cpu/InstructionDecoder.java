package com.spectrum.cpu;

import com.spectrum.memory.MemoryBus;

/**
 * Decodes bytes from memory at PC into Instruction records.
 * Supports standard, extended (ED), bit (CB), index (DD/FD),
 * and indexed bit (DDCB/FDCB) instructions including all undocumented variants.
 */
public final class InstructionDecoder {

    /**
     * Decodes the next instruction at the current PC, advancing PC and R accordingly.
     */
    public Instruction decode(CpuState cpu, MemoryBus memory) {
        var r = cpu.getRegisters();

        Register16 indexReg = null;
        int op;

        // Consume prefix loop (DD, FD)
        while (true) {
            op = fetchByte(cpu, memory);
            if (op == 0xDD) {
                indexReg = Register16.IX;
            } else if (op == 0xFD) {
                indexReg = Register16.IY;
            } else {
                break;
            }
        }

        if (op == 0xCB) {
            if (indexReg != null) {
                return decodeDDCB(cpu, memory, indexReg);
            } else {
                return decodeCB(cpu, memory);
            }
        }

        if (op == 0xED) {
            // ED cancels any preceding DD/FD prefix
            return decodeED(cpu, memory);
        }

        if (indexReg != null) {
            Instruction instr = decodeIndex(cpu, memory, indexReg, op);
            if (instr != null) {
                return instr;
            }
            // If the opcode is unaffected by DD/FD, execute it as a normal opcode!
        }

        return decodeBase(cpu, memory, op);
    }

    private int fetchByte(CpuState cpu, MemoryBus memory) {
        var r = cpu.getRegisters();
        int pc = r.getPC();
        int val = memory.readByte(pc);
        r.setPC((pc + 1) & 0xFFFF);
        r.incR();
        return val;
    }

    private int fetchWord(CpuState cpu, MemoryBus memory) {
        int low = fetchByte(cpu, memory);
        int high = fetchByte(cpu, memory);
        return (high << 8) | low;
    }

    private Instruction decodeBase(CpuState cpu, MemoryBus memory, int op) {
        // 0x40 - 0x7F: LD r, r' and HALT
        if (op >= 0x40 && op <= 0x7F) {
            if (op == 0x76) return new Instruction.Halt(4);
            int dstCode = (op >> 3) & 7;
            int srcCode = op & 7;
            if (dstCode == 6) {
                return new Instruction.LdIndHlReg(Register8.fromCode(srcCode), 7);
            } else if (srcCode == 6) {
                return new Instruction.LdRegIndHl(Register8.fromCode(dstCode), 7);
            } else {
                return new Instruction.LdRegReg(Register8.fromCode(dstCode), Register8.fromCode(srcCode), 4);
            }
        }

        // 0x80 - 0xBF: 8-bit ALU on r or (HL)
        if (op >= 0x80 && op <= 0xBF) {
            AluOp aluOp = AluOp.fromCode((op >> 3) & 7);
            int srcCode = op & 7;
            if (srcCode == 6) {
                return new Instruction.AluIndHl(aluOp, 7);
            } else {
                return new Instruction.AluReg(aluOp, Register8.fromCode(srcCode), 4);
            }
        }

        return switch (op) {
            case 0x00 -> new Instruction.Nop(4);
            case 0x01 -> new Instruction.Ld16RegImm(Register16.BC, fetchWord(cpu, memory), 10);
            case 0x02 -> new Instruction.LdIndBcAcc(7);
            case 0x03 -> new Instruction.Inc16(Register16.BC, 6);
            case 0x04 -> new Instruction.Inc8Reg(Register8.B, 4);
            case 0x05 -> new Instruction.Dec8Reg(Register8.B, 4);
            case 0x06 -> new Instruction.LdRegImm(Register8.B, fetchByte(cpu, memory), 7);
            case 0x07 -> new Instruction.RotateAcc(RotateAccOp.RLCA, 4);
            case 0x08 -> new Instruction.ExAfAfPrime(4);
            case 0x09 -> new Instruction.Add16(Register16.HL, Register16.BC, 11);
            case 0x0A -> new Instruction.LdAccIndBc(7);
            case 0x0B -> new Instruction.Dec16(Register16.BC, 6);
            case 0x0C -> new Instruction.Inc8Reg(Register8.C, 4);
            case 0x0D -> new Instruction.Dec8Reg(Register8.C, 4);
            case 0x0E -> new Instruction.LdRegImm(Register8.C, fetchByte(cpu, memory), 7);
            case 0x0F -> new Instruction.RotateAcc(RotateAccOp.RRCA, 4);

            case 0x10 -> new Instruction.Djnz((byte) fetchByte(cpu, memory), 8, 13);
            case 0x11 -> new Instruction.Ld16RegImm(Register16.DE, fetchWord(cpu, memory), 10);
            case 0x12 -> new Instruction.LdIndDeAcc(7);
            case 0x13 -> new Instruction.Inc16(Register16.DE, 6);
            case 0x14 -> new Instruction.Inc8Reg(Register8.D, 4);
            case 0x15 -> new Instruction.Dec8Reg(Register8.D, 4);
            case 0x16 -> new Instruction.LdRegImm(Register8.D, fetchByte(cpu, memory), 7);
            case 0x17 -> new Instruction.RotateAcc(RotateAccOp.RLA, 4);
            case 0x18 -> new Instruction.JrImm((byte) fetchByte(cpu, memory), 12);
            case 0x19 -> new Instruction.Add16(Register16.HL, Register16.DE, 11);
            case 0x1A -> new Instruction.LdAccIndDe(7);
            case 0x1B -> new Instruction.Dec16(Register16.DE, 6);
            case 0x1C -> new Instruction.Inc8Reg(Register8.E, 4);
            case 0x1D -> new Instruction.Dec8Reg(Register8.E, 4);
            case 0x1E -> new Instruction.LdRegImm(Register8.E, fetchByte(cpu, memory), 7);
            case 0x1F -> new Instruction.RotateAcc(RotateAccOp.RRA, 4);

            case 0x20 -> new Instruction.JrCondImm(Condition.NZ, (byte) fetchByte(cpu, memory), 7, 12);
            case 0x21 -> new Instruction.Ld16RegImm(Register16.HL, fetchWord(cpu, memory), 10);
            case 0x22 -> new Instruction.Ld16Ind16Reg(fetchWord(cpu, memory), Register16.HL, 16);
            case 0x23 -> new Instruction.Inc16(Register16.HL, 6);
            case 0x24 -> new Instruction.Inc8Reg(Register8.H, 4);
            case 0x25 -> new Instruction.Dec8Reg(Register8.H, 4);
            case 0x26 -> new Instruction.LdRegImm(Register8.H, fetchByte(cpu, memory), 7);
            case 0x27 -> new Instruction.Daa(4);
            case 0x28 -> new Instruction.JrCondImm(Condition.Z, (byte) fetchByte(cpu, memory), 7, 12);
            case 0x29 -> new Instruction.Add16(Register16.HL, Register16.HL, 11);
            case 0x2A -> new Instruction.Ld16RegInd16(Register16.HL, fetchWord(cpu, memory), 16);
            case 0x2B -> new Instruction.Dec16(Register16.HL, 6);
            case 0x2C -> new Instruction.Inc8Reg(Register8.L, 4);
            case 0x2D -> new Instruction.Dec8Reg(Register8.L, 4);
            case 0x2E -> new Instruction.LdRegImm(Register8.L, fetchByte(cpu, memory), 7);
            case 0x2F -> new Instruction.Cpl(4);

            case 0x30 -> new Instruction.JrCondImm(Condition.NC, (byte) fetchByte(cpu, memory), 7, 12);
            case 0x31 -> new Instruction.Ld16RegImm(Register16.SP, fetchWord(cpu, memory), 10);
            case 0x32 -> new Instruction.LdInd16Acc(fetchWord(cpu, memory), 13);
            case 0x33 -> new Instruction.Inc16(Register16.SP, 6);
            case 0x34 -> new Instruction.Inc8IndHl(11);
            case 0x35 -> new Instruction.Dec8IndHl(11);
            case 0x36 -> new Instruction.LdIndHlImm(fetchByte(cpu, memory), 10);
            case 0x37 -> new Instruction.Scf(4);
            case 0x38 -> new Instruction.JrCondImm(Condition.C, (byte) fetchByte(cpu, memory), 7, 12);
            case 0x39 -> new Instruction.Add16(Register16.HL, Register16.SP, 11);
            case 0x3A -> new Instruction.LdAccInd16(fetchWord(cpu, memory), 13);
            case 0x3B -> new Instruction.Dec16(Register16.SP, 6);
            case 0x3C -> new Instruction.Inc8Reg(Register8.A, 4);
            case 0x3D -> new Instruction.Dec8Reg(Register8.A, 4);
            case 0x3E -> new Instruction.LdRegImm(Register8.A, fetchByte(cpu, memory), 7);
            case 0x3F -> new Instruction.Ccf(4);

            case 0xC0 -> new Instruction.RetCond(Condition.NZ, 5, 11);
            case 0xC1 -> new Instruction.Pop(Register16.BC, 10);
            case 0xC2 -> new Instruction.JpCondImm(Condition.NZ, fetchWord(cpu, memory), 10, 10);
            case 0xC3 -> new Instruction.JpImm(fetchWord(cpu, memory), 10);
            case 0xC4 -> new Instruction.CallCondImm(Condition.NZ, fetchWord(cpu, memory), 10, 17);
            case 0xC5 -> new Instruction.Push(Register16.BC, 11);
            case 0xC6 -> new Instruction.AluImm(AluOp.ADD, fetchByte(cpu, memory), 7);
            case 0xC7 -> new Instruction.Rst(0x00, 11);
            case 0xC8 -> new Instruction.RetCond(Condition.Z, 5, 11);
            case 0xC9 -> new Instruction.Ret(10);
            case 0xCA -> new Instruction.JpCondImm(Condition.Z, fetchWord(cpu, memory), 10, 10);
            case 0xCC -> new Instruction.CallCondImm(Condition.Z, fetchWord(cpu, memory), 10, 17);
            case 0xCD -> new Instruction.CallImm(fetchWord(cpu, memory), 17);
            case 0xCE -> new Instruction.AluImm(AluOp.ADC, fetchByte(cpu, memory), 7);
            case 0xCF -> new Instruction.Rst(0x08, 11);

            case 0xD0 -> new Instruction.RetCond(Condition.NC, 5, 11);
            case 0xD1 -> new Instruction.Pop(Register16.DE, 10);
            case 0xD2 -> new Instruction.JpCondImm(Condition.NC, fetchWord(cpu, memory), 10, 10);
            case 0xD3 -> new Instruction.OutImmAcc(fetchByte(cpu, memory), 11);
            case 0xD4 -> new Instruction.CallCondImm(Condition.NC, fetchWord(cpu, memory), 10, 17);
            case 0xD5 -> new Instruction.Push(Register16.DE, 11);
            case 0xD6 -> new Instruction.AluImm(AluOp.SUB, fetchByte(cpu, memory), 7);
            case 0xD7 -> new Instruction.Rst(0x10, 11);
            case 0xD8 -> new Instruction.RetCond(Condition.C, 5, 11);
            case 0xD9 -> new Instruction.Exx(4);
            case 0xDA -> new Instruction.JpCondImm(Condition.C, fetchWord(cpu, memory), 10, 10);
            case 0xDB -> new Instruction.InAccImm(fetchByte(cpu, memory), 11);
            case 0xDC -> new Instruction.CallCondImm(Condition.C, fetchWord(cpu, memory), 10, 17);
            case 0xDE -> new Instruction.AluImm(AluOp.SBC, fetchByte(cpu, memory), 7);
            case 0xDF -> new Instruction.Rst(0x18, 11);

            case 0xE0 -> new Instruction.RetCond(Condition.PO, 5, 11);
            case 0xE1 -> new Instruction.Pop(Register16.HL, 10);
            case 0xE2 -> new Instruction.JpCondImm(Condition.PO, fetchWord(cpu, memory), 10, 10);
            case 0xE3 -> new Instruction.ExSpHl(19);
            case 0xE4 -> new Instruction.CallCondImm(Condition.PO, fetchWord(cpu, memory), 10, 17);
            case 0xE5 -> new Instruction.Push(Register16.HL, 11);
            case 0xE6 -> new Instruction.AluImm(AluOp.AND, fetchByte(cpu, memory), 7);
            case 0xE7 -> new Instruction.Rst(0x20, 11);
            case 0xE8 -> new Instruction.RetCond(Condition.PE, 5, 11);
            case 0xE9 -> new Instruction.JpIndIndex(Register16.HL, 4);
            case 0xEA -> new Instruction.JpCondImm(Condition.PE, fetchWord(cpu, memory), 10, 10);
            case 0xEB -> new Instruction.ExDeHl(4);
            case 0xEC -> new Instruction.CallCondImm(Condition.PE, fetchWord(cpu, memory), 10, 17);
            case 0xEE -> new Instruction.AluImm(AluOp.XOR, fetchByte(cpu, memory), 7);
            case 0xEF -> new Instruction.Rst(0x28, 11);

            case 0xF0 -> new Instruction.RetCond(Condition.P, 5, 11);
            case 0xF1 -> new Instruction.Pop(Register16.AF, 10);
            case 0xF2 -> new Instruction.JpCondImm(Condition.P, fetchWord(cpu, memory), 10, 10);
            case 0xF3 -> new Instruction.Di(4);
            case 0xF4 -> new Instruction.CallCondImm(Condition.P, fetchWord(cpu, memory), 10, 17);
            case 0xF5 -> new Instruction.Push(Register16.AF, 11);
            case 0xF6 -> new Instruction.AluImm(AluOp.OR, fetchByte(cpu, memory), 7);
            case 0xF7 -> new Instruction.Rst(0x30, 11);
            case 0xF8 -> new Instruction.RetCond(Condition.M, 5, 11);
            case 0xF9 -> new Instruction.Ld16SpIndex(Register16.HL, 6);
            case 0xFA -> new Instruction.JpCondImm(Condition.M, fetchWord(cpu, memory), 10, 10);
            case 0xFB -> new Instruction.Ei(4);
            case 0xFC -> new Instruction.CallCondImm(Condition.M, fetchWord(cpu, memory), 10, 17);
            case 0xFE -> new Instruction.AluImm(AluOp.CP, fetchByte(cpu, memory), 7);
            case 0xFF -> new Instruction.Rst(0x38, 11);

            default -> new Instruction.Nop(4);
        };
    }

    private Instruction decodeCB(CpuState cpu, MemoryBus memory) {
        int op = fetchByte(cpu, memory);
        int regCode = op & 7;

        if (op < 0x40) {
            // Rotates and Shifts (including undocumented SLL at op 0x30-0x37)
            RotateOp rotOp = RotateOp.fromCode((op >> 3) & 7);
            if (regCode == 6) {
                return new Instruction.RotateIndHl(rotOp, 15);
            } else {
                return new Instruction.RotateReg(rotOp, Register8.fromCode(regCode), 8);
            }
        } else if (op < 0x80) {
            // BIT b, r
            int bit = (op >> 3) & 7;
            if (regCode == 6) {
                return new Instruction.BitIndHl(bit, 12);
            } else {
                return new Instruction.BitReg(bit, Register8.fromCode(regCode), 8);
            }
        } else if (op < 0xC0) {
            // RES b, r
            int bit = (op >> 3) & 7;
            if (regCode == 6) {
                return new Instruction.ResIndHl(bit, 15);
            } else {
                return new Instruction.ResReg(bit, Register8.fromCode(regCode), 8);
            }
        } else {
            // SET b, r
            int bit = (op >> 3) & 7;
            if (regCode == 6) {
                return new Instruction.SetIndHl(bit, 15);
            } else {
                return new Instruction.SetReg(bit, Register8.fromCode(regCode), 8);
            }
        }
    }

    private Instruction decodeED(CpuState cpu, MemoryBus memory) {
        int op = fetchByte(cpu, memory);

        return switch (op) {
            // 16-bit Load with indirect address
            case 0x43 -> new Instruction.Ld16Ind16Reg(fetchWord(cpu, memory), Register16.BC, 20);
            case 0x53 -> new Instruction.Ld16Ind16Reg(fetchWord(cpu, memory), Register16.DE, 20);
            case 0x63 -> new Instruction.Ld16Ind16Reg(fetchWord(cpu, memory), Register16.HL, 20); // Undocumented alias of 0x22
            case 0x73 -> new Instruction.Ld16Ind16Reg(fetchWord(cpu, memory), Register16.SP, 20);

            case 0x4B -> new Instruction.Ld16RegInd16(Register16.BC, fetchWord(cpu, memory), 20);
            case 0x5B -> new Instruction.Ld16RegInd16(Register16.DE, fetchWord(cpu, memory), 20);
            case 0x6B -> new Instruction.Ld16RegInd16(Register16.HL, fetchWord(cpu, memory), 20); // Undocumented alias of 0x2A
            case 0x7B -> new Instruction.Ld16RegInd16(Register16.SP, fetchWord(cpu, memory), 20);

            // ADC HL, ss and SBC HL, ss
            case 0x4A -> new Instruction.Adc16Hl(Register16.BC, 15);
            case 0x5A -> new Instruction.Adc16Hl(Register16.DE, 15);
            case 0x6A -> new Instruction.Adc16Hl(Register16.HL, 15);
            case 0x7A -> new Instruction.Adc16Hl(Register16.SP, 15);

            case 0x42 -> new Instruction.Sbc16Hl(Register16.BC, 15);
            case 0x52 -> new Instruction.Sbc16Hl(Register16.DE, 15);
            case 0x62 -> new Instruction.Sbc16Hl(Register16.HL, 15);
            case 0x72 -> new Instruction.Sbc16Hl(Register16.SP, 15);

            // IN r, (C) and OUT (C), r
            case 0x40 -> new Instruction.InRegC(Register8.B, 12);
            case 0x48 -> new Instruction.InRegC(Register8.C, 12);
            case 0x50 -> new Instruction.InRegC(Register8.D, 12);
            case 0x58 -> new Instruction.InRegC(Register8.E, 12);
            case 0x60 -> new Instruction.InRegC(Register8.H, 12);
            case 0x68 -> new Instruction.InRegC(Register8.L, 12);
            case 0x70 -> new Instruction.InIndCNoReg(12); // Undocumented ED 70: tests flags without storing
            case 0x78 -> new Instruction.InRegC(Register8.A, 12);

            case 0x41 -> new Instruction.OutRegC(Register8.B, 12);
            case 0x49 -> new Instruction.OutRegC(Register8.C, 12);
            case 0x51 -> new Instruction.OutRegC(Register8.D, 12);
            case 0x59 -> new Instruction.OutRegC(Register8.E, 12);
            case 0x61 -> new Instruction.OutRegC(Register8.H, 12);
            case 0x69 -> new Instruction.OutRegC(Register8.L, 12);
            case 0x71 -> new Instruction.OutCZero(12); // Undocumented ED 71: OUT (C), 0
            case 0x79 -> new Instruction.OutRegC(Register8.A, 12);

            // System registers
            case 0x47 -> new Instruction.LdIReg(9);
            case 0x4F -> new Instruction.LdRReg(9);
            case 0x57 -> new Instruction.LdRegI(9);
            case 0x5F -> new Instruction.LdRegR(9);

            // Rotates & Math
            case 0x67 -> new Instruction.Rrd(18);
            case 0x6F -> new Instruction.Rld(18);
            case 0x44, 0x4C, 0x54, 0x5C, 0x64, 0x6C, 0x74, 0x7C -> new Instruction.Neg(8);

            // Returns
            case 0x4D -> new Instruction.Reti(14);
            case 0x45, 0x55, 0x5D, 0x65, 0x6D, 0x75, 0x7D -> new Instruction.Retn(14);

            // Interrupt Modes
            case 0x46, 0x66 -> new Instruction.Im(0, 8);
            case 0x56, 0x76 -> new Instruction.Im(1, 8);
            case 0x5E, 0x7E -> new Instruction.Im(2, 8);

            // Block instructions
            case 0xA0 -> new Instruction.Ldi(16);
            case 0xB0 -> new Instruction.Ldir(16, 21);
            case 0xA8 -> new Instruction.Ldd(16);
            case 0xB8 -> new Instruction.Lddr(16, 21);
            case 0xA1 -> new Instruction.Cpi(16);
            case 0xB1 -> new Instruction.Cpir(16, 21);
            case 0xA9 -> new Instruction.Cpd(16);
            case 0xB9 -> new Instruction.Cpdr(16, 21);
            case 0xA2 -> new Instruction.Ini(16);
            case 0xB2 -> new Instruction.Inir(16, 21);
            case 0xAA -> new Instruction.Ind(16);
            case 0xBA -> new Instruction.Indr(16, 21);
            case 0xA3 -> new Instruction.Outi(16);
            case 0xB3 -> new Instruction.Otir(16, 21);
            case 0xAB -> new Instruction.Outd(16);
            case 0xBB -> new Instruction.Otdr(16, 21);

            default -> new Instruction.Nop(8);
        };
    }

    private Instruction decodeIndex(CpuState cpu, MemoryBus memory, Register16 idx, int op) {
        boolean isIx = (idx == Register16.IX);
        Register8 hReg = isIx ? Register8.IXH : Register8.IYH;
        Register8 lReg = isIx ? Register8.IXL : Register8.IYL;

        // 0x40 - 0x7F with index displacement or undocumented half-registers
        if (op >= 0x40 && op <= 0x7F) {
            if (op == 0x76) return new Instruction.Halt(4);
            int dstCode = (op >> 3) & 7;
            int srcCode = op & 7;

            if (dstCode == 6) { // LD (IX+d), r
                int d = fetchByte(cpu, memory);
                return new Instruction.LdIndOffsetReg(idx, d, Register8.fromCode(srcCode), 19);
            } else if (srcCode == 6) { // LD r, (IX+d)
                int d = fetchByte(cpu, memory);
                return new Instruction.LdRegIndOffset(Register8.fromCode(dstCode), idx, d, 19);
            } else {
                // Undocumented register-to-register moves involving IXH/IXL or IYH/IYL
                Register8 dst = mapIndexReg(dstCode, hReg, lReg);
                Register8 src = mapIndexReg(srcCode, hReg, lReg);
                if (dst != Register8.fromCode(dstCode) || src != Register8.fromCode(srcCode)) {
                    return new Instruction.LdRegReg(dst, src, 8);
                } else {
                    return null; // Unaffected by index prefix
                }
            }
        }

        // 0x80 - 0xBF with index displacement or half-registers
        if (op >= 0x80 && op <= 0xBF) {
            AluOp aluOp = AluOp.fromCode((op >> 3) & 7);
            int srcCode = op & 7;
            if (srcCode == 6) {
                int d = fetchByte(cpu, memory);
                return new Instruction.AluIndOffset(aluOp, idx, d, 19);
            } else {
                Register8 src = mapIndexReg(srcCode, hReg, lReg);
                if (src != Register8.fromCode(srcCode)) {
                    return new Instruction.AluReg(aluOp, src, 8);
                } else {
                    return null;
                }
            }
        }

        return switch (op) {
            case 0x21 -> new Instruction.Ld16RegImm(idx, fetchWord(cpu, memory), 14);
            case 0x22 -> new Instruction.Ld16Ind16Reg(fetchWord(cpu, memory), idx, 20);
            case 0x2A -> new Instruction.Ld16RegInd16(idx, fetchWord(cpu, memory), 20);
            case 0x23 -> new Instruction.Inc16(idx, 10);
            case 0x2B -> new Instruction.Dec16(idx, 10);

            case 0x09 -> new Instruction.Add16(idx, Register16.BC, 15);
            case 0x19 -> new Instruction.Add16(idx, Register16.DE, 15);
            case 0x29 -> new Instruction.Add16(idx, idx, 15);
            case 0x39 -> new Instruction.Add16(idx, Register16.SP, 15);

            case 0xE1 -> new Instruction.Pop(idx, 14);
            case 0xE5 -> new Instruction.Push(idx, 15);
            case 0xE3 -> new Instruction.ExSpIndex(idx, 23);
            case 0xE9 -> new Instruction.JpIndIndex(idx, 8);
            case 0xF9 -> new Instruction.Ld16SpIndex(idx, 10);

            // Undocumented 8-bit operations on IXH/IXL or IYH/IYL
            case 0x24 -> new Instruction.Inc8Reg(hReg, 8);
            case 0x25 -> new Instruction.Dec8Reg(hReg, 8);
            case 0x26 -> new Instruction.LdRegImm(hReg, fetchByte(cpu, memory), 11);
            case 0x2C -> new Instruction.Inc8Reg(lReg, 8);
            case 0x2D -> new Instruction.Dec8Reg(lReg, 8);
            case 0x2E -> new Instruction.LdRegImm(lReg, fetchByte(cpu, memory), 11);

            // Indexed memory (IX+d) / (IY+d)
            case 0x34 -> new Instruction.Inc8IndOffset(idx, fetchByte(cpu, memory), 23);
            case 0x35 -> new Instruction.Dec8IndOffset(idx, fetchByte(cpu, memory), 23);
            case 0x36 -> {
                int d = fetchByte(cpu, memory);
                int imm = fetchByte(cpu, memory);
                yield new Instruction.LdIndOffsetImm(idx, d, imm, 19);
            }

            default -> null; // Unaffected by prefix, execute as standard base instruction
        };
    }

    private Register8 mapIndexReg(int code, Register8 hReg, Register8 lReg) {
        return switch (code) {
            case 4 -> hReg;
            case 5 -> lReg;
            default -> Register8.fromCode(code);
        };
    }

    /**
     * Decodes DDCB / FDCB indexed bit/shift instructions.
     * Note: In Z80, the displacement byte 'd' is fetched BEFORE the opcode!
     * Pattern: [DD/FD] [CB] [d] [opcode]
     */
    private Instruction decodeDDCB(CpuState cpu, MemoryBus memory, Register16 idx) {
        int d = fetchByte(cpu, memory);
        int op = fetchByte(cpu, memory);
        int regCode = op & 7;

        if (op < 0x40) {
            // Shift/Rotate on (IX+d)
            RotateOp rotOp = RotateOp.fromCode((op >> 3) & 7);
            if (regCode == 6) {
                return new Instruction.RotateIndOffset(rotOp, idx, d, 23);
            } else {
                // Undocumented: shift/rotate (IX+d) AND load into register regCode!
                return new Instruction.BitOpOffsetWithReg(BitShiftOp.ROTATE, rotOp, 0, idx, d, Register8.fromCode(regCode), 23);
            }
        } else if (op < 0x80) {
            // BIT b, (IX+d) - does NOT write to a register even if regCode != 6
            int bit = (op >> 3) & 7;
            return new Instruction.BitIndOffset(bit, idx, d, 20);
        } else if (op < 0xC0) {
            // RES b, (IX+d)
            int bit = (op >> 3) & 7;
            if (regCode == 6) {
                return new Instruction.ResIndOffset(bit, idx, d, 23);
            } else {
                // Undocumented: RES b, (IX+d) AND load into register regCode!
                return new Instruction.BitOpOffsetWithReg(BitShiftOp.RES, RotateOp.RLC, bit, idx, d, Register8.fromCode(regCode), 23);
            }
        } else {
            // SET b, (IX+d)
            int bit = (op >> 3) & 7;
            if (regCode == 6) {
                return new Instruction.SetIndOffset(bit, idx, d, 23);
            } else {
                // Undocumented: SET b, (IX+d) AND load into register regCode!
                return new Instruction.BitOpOffsetWithReg(BitShiftOp.SET, RotateOp.RLC, bit, idx, d, Register8.fromCode(regCode), 23);
            }
        }
    }
}
