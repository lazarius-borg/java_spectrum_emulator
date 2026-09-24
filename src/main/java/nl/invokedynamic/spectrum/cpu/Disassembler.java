package nl.invokedynamic.spectrum.cpu;

import nl.invokedynamic.spectrum.memory.MemoryBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Disassembles bytes from memory at a given PC address into human-readable Z80 assembly lines.
 */
public final class Disassembler {

    public record DisassembledInstruction(int address, int length, String mnemonic) {
        @Override
        public String toString() {
            return String.format("0x%04X:  %s", address, mnemonic);
        }
    }

    private Disassembler() {}

    /**
     * Decodes a list of consecutive instructions starting from the given PC.
     */
    public static List<DisassembledInstruction> disassemble(MemoryBus memory, int startPc, int count) {
        List<DisassembledInstruction> result = new ArrayList<>();
        InstructionDecoder decoder = new InstructionDecoder();
        CpuState tempCpu = new CpuState();
        tempCpu.getRegisters().setPC(startPc & 0xFFFF);

        for (int i = 0; i < count; i++) {
            int addr = tempCpu.getRegisters().getPC();
            try {
                Instruction instr = decoder.decode(tempCpu, memory);
                int nextPc = tempCpu.getRegisters().getPC();
                int len = (nextPc - addr) & 0xFFFF;
                if (len <= 0) len = 1;
                result.add(new DisassembledInstruction(addr, len, format(instr)));
            } catch (Exception ex) {
                result.add(new DisassembledInstruction(addr, 1, String.format("DB   0x%02X", memory.readByte(addr))));
                tempCpu.getRegisters().setPC((addr + 1) & 0xFFFF);
            }
        }
        return result;
    }

    private static String formatDisp(int d) {
        if (d >= 0) return "+" + d;
        return String.valueOf(d);
    }

    public static String format(Instruction instr) {
        return switch (instr) {
            case Instruction.Nop _ -> "NOP";
            case Instruction.Halt _ -> "HALT";
            case Instruction.Di _ -> "DI";
            case Instruction.Ei _ -> "EI";
            case Instruction.Im(int mode, _) -> "IM   " + mode;
            case Instruction.Scf _ -> "SCF";
            case Instruction.Ccf _ -> "CCF";
            case Instruction.Cpl _ -> "CPL";
            case Instruction.Neg _ -> "NEG";
            case Instruction.Daa _ -> "DAA";
            case Instruction.ExDeHl _ -> "EX   DE, HL";
            case Instruction.ExAfAfPrime _ -> "EX   AF, AF'";
            case Instruction.Exx _ -> "EXX";
            case Instruction.ExSpHl _ -> "EX   (SP), HL";
            case Instruction.ExSpIndex(var reg, _) -> "EX   (SP), " + reg;

            // 8-bit Load
            case Instruction.LdRegReg(var dst, var src, _) -> "LD   " + dst + ", " + src;
            case Instruction.LdRegImm(var dst, int imm, _) -> String.format("LD   %s, 0x%02X", dst, imm);
            case Instruction.LdRegIndHl(var dst, _) -> "LD   " + dst + ", (HL)";
            case Instruction.LdIndHlReg(var src, _) -> "LD   (HL), " + src;
            case Instruction.LdRegIndOffset(var dst, var idx, int d, _) -> "LD   " + dst + ", (" + idx + formatDisp(d) + ")";
            case Instruction.LdIndOffsetReg(var idx, int d, var src, _) -> "LD   (" + idx + formatDisp(d) + "), " + src;
            case Instruction.LdIndHlImm(int imm, _) -> String.format("LD   (HL), 0x%02X", imm);
            case Instruction.LdIndOffsetImm(var idx, int d, int imm, _) -> String.format("LD   (%s%s), 0x%02X", idx, formatDisp(d), imm);
            case Instruction.LdAccIndBc _ -> "LD   A, (BC)";
            case Instruction.LdAccIndDe _ -> "LD   A, (DE)";
            case Instruction.LdIndBcAcc _ -> "LD   (BC), A";
            case Instruction.LdIndDeAcc _ -> "LD   (DE), A";
            case Instruction.LdAccInd16(int addr, _) -> String.format("LD   A, (0x%04X)", addr);
            case Instruction.LdInd16Acc(int addr, _) -> String.format("LD   (0x%04X), A", addr);
            case Instruction.LdRegI _ -> "LD   A, I";
            case Instruction.LdRegR _ -> "LD   A, R";
            case Instruction.LdIReg _ -> "LD   I, A";
            case Instruction.LdRReg _ -> "LD   R, A";

            // 16-bit Load & Stack
            case Instruction.Ld16RegImm(var dst, int imm16, _) -> String.format("LD   %s, 0x%04X", dst, imm16);
            case Instruction.Ld16Ind16Reg(int addr, var src, _) -> String.format("LD   (0x%04X), %s", addr, src);
            case Instruction.Ld16RegInd16(var dst, int addr, _) -> String.format("LD   %s, (0x%04X)", dst, addr);
            case Instruction.Ld16SpIndex(var src, _) -> "LD   SP, " + src;
            case Instruction.Push(var reg, _) -> "PUSH " + reg;
            case Instruction.Pop(var reg, _) -> "POP  " + reg;

            // 8-bit Arithmetic / Logic
            case Instruction.AluReg(var op, var src, _) -> op + "  " + src;
            case Instruction.AluImm(var op, int imm, _) -> String.format("%-4s 0x%02X", op, imm);
            case Instruction.AluIndHl(var op, _) -> op + "  (HL)";
            case Instruction.AluIndOffset(var op, var idx, int d, _) -> op + "  (" + idx + formatDisp(d) + ")";
            case Instruction.Inc8Reg(var reg, _) -> "INC  " + reg;
            case Instruction.Inc8IndHl _ -> "INC  (HL)";
            case Instruction.Inc8IndOffset(var idx, int d, _) -> "INC  (" + idx + formatDisp(d) + ")";
            case Instruction.Dec8Reg(var reg, _) -> "DEC  " + reg;
            case Instruction.Dec8IndHl _ -> "DEC  (HL)";
            case Instruction.Dec8IndOffset(var idx, int d, _) -> "DEC  (" + idx + formatDisp(d) + ")";

            // 16-bit Arithmetic
            case Instruction.Add16(var dst, var src, _) -> "ADD  " + dst + ", " + src;
            case Instruction.Adc16Hl(var src, _) -> "ADC  HL, " + src;
            case Instruction.Sbc16Hl(var src, _) -> "SBC  HL, " + src;
            case Instruction.Inc16(var reg, _) -> "INC  " + reg;
            case Instruction.Dec16(var reg, _) -> "DEC  " + reg;

            // Rotates and Shifts
            case Instruction.RotateReg(var op, var reg, _) -> op + "  " + reg;
            case Instruction.RotateIndHl(var op, _) -> op + "  (HL)";
            case Instruction.RotateIndOffset(var op, var idx, int d, _) -> op + "  (" + idx + formatDisp(d) + ")";
            case Instruction.RotateAcc(var op, _) -> op.toString();
            case Instruction.Rld _ -> "RLD";
            case Instruction.Rrd _ -> "RRD";

            // Bit operations
            case Instruction.BitReg(int bit, var reg, _) -> "BIT  " + bit + ", " + reg;
            case Instruction.BitIndHl(int bit, _) -> "BIT  " + bit + ", (HL)";
            case Instruction.BitIndOffset(int bit, var idx, int d, _) -> "BIT  " + bit + ", (" + idx + formatDisp(d) + ")";
            case Instruction.SetReg(int bit, var reg, _) -> "SET  " + bit + ", " + reg;
            case Instruction.SetIndHl(int bit, _) -> "SET  " + bit + ", (HL)";
            case Instruction.SetIndOffset(int bit, var idx, int d, _) -> "SET  " + bit + ", (" + idx + formatDisp(d) + ")";
            case Instruction.ResReg(int bit, var reg, _) -> "RES  " + bit + ", " + reg;
            case Instruction.ResIndHl(int bit, _) -> "RES  " + bit + ", (HL)";
            case Instruction.ResIndOffset(int bit, var idx, int d, _) -> "RES  " + bit + ", (" + idx + formatDisp(d) + ")";
            case Instruction.BitOpOffsetWithReg(_, var rot, int bit, var idx, int d, var tgt, _) ->
                (rot != null ? rot.toString() : "BITOP " + bit) + " (" + idx + formatDisp(d) + ")," + tgt;

            // Jumps, Calls, Returns
            case Instruction.JpImm(int target, _) -> String.format("JP   0x%04X", target);
            case Instruction.JpCondImm(var cond, int target, _, _) -> String.format("JP   %s, 0x%04X", cond, target);
            case Instruction.JrImm(int d, _) -> String.format("JR   %+d", d);
            case Instruction.JrCondImm(var cond, int d, _, _) -> String.format("JR   %s, %+d", cond, d);
            case Instruction.JpIndIndex(var idx, _) -> "JP   (" + idx + ")";
            case Instruction.Djnz(int d, _, _) -> String.format("DJNZ %+d", d);
            case Instruction.CallImm(int target, _) -> String.format("CALL 0x%04X", target);
            case Instruction.CallCondImm(var cond, int target, _, _) -> String.format("CALL %s, 0x%04X", cond, target);
            case Instruction.Ret _ -> "RET";
            case Instruction.RetCond(var cond, _, _) -> "RET  " + cond;
            case Instruction.Reti _ -> "RETI";
            case Instruction.Retn _ -> "RETN";
            case Instruction.Rst(int target, _) -> String.format("RST  0x%02X", target);

            // Input / Output
            case Instruction.InAccImm(int port, _) -> String.format("IN   A, (0x%02X)", port);
            case Instruction.InRegC(var reg, _) -> "IN   " + reg + ", (C)";
            case Instruction.InIndCNoReg _ -> "IN   (C)";
            case Instruction.OutImmAcc(int port, _) -> String.format("OUT  (0x%02X), A", port);
            case Instruction.OutRegC(var reg, _) -> "OUT  (C), " + reg;
            case Instruction.OutCZero _ -> "OUT  (C), 0";

            // Block Instructions
            case Instruction.Ldi _ -> "LDI";
            case Instruction.Ldir _ -> "LDIR";
            case Instruction.Ldd _ -> "LDD";
            case Instruction.Lddr _ -> "LDDR";
            case Instruction.Cpi _ -> "CPI";
            case Instruction.Cpir _ -> "CPIR";
            case Instruction.Cpd _ -> "CPD";
            case Instruction.Cpdr _ -> "CPDR";
            case Instruction.Ini _ -> "INI";
            case Instruction.Inir _ -> "INIR";
            case Instruction.Ind _ -> "IND";
            case Instruction.Indr _ -> "INDR";
            case Instruction.Outi _ -> "OUTI";
            case Instruction.Otir _ -> "OTIR";
            case Instruction.Outd _ -> "OUTD";
            case Instruction.Otdr _ -> "OTDR";
        };
    }
}
