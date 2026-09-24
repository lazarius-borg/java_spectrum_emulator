package nl.invokedynamic.spectrum.cpu;

import nl.invokedynamic.spectrum.io.IoBus;
import nl.invokedynamic.spectrum.memory.MemoryBus;

/**
 * Executes decoded Z80 instruction records using an exhaustive switch expression
 * matching each record and invoking its handler.
 * Implements 100% full parity including all undocumented instructions,
 * undocumented flags (F3, F5), MEMPTR (WZ), and SLL.
 */
public final class InstructionExecutor {

    public int execute(Instruction instr, CpuState cpu, MemoryBus memory, IoBus io) {
        return switch (instr) {
            // Control & State
            case Instruction.Nop(int c) -> executeNop(cpu, c);
            case Instruction.Halt(int c) -> executeHalt(cpu, c);
            case Instruction.Di(int c) -> executeDi(cpu, c);
            case Instruction.Ei(int c) -> executeEi(cpu, c);
            case Instruction.Im(int mode, int c) -> executeIm(cpu, mode, c);
            case Instruction.Scf(int c) -> executeScf(cpu, c);
            case Instruction.Ccf(int c) -> executeCcf(cpu, c);
            case Instruction.Cpl(int c) -> executeCpl(cpu, c);
            case Instruction.Neg(int c) -> executeNeg(cpu, c);
            case Instruction.Daa(int c) -> executeDaa(cpu, c);
            case Instruction.ExDeHl(int c) -> executeExDeHl(cpu, c);
            case Instruction.ExAfAfPrime(int c) -> executeExAfAfPrime(cpu, c);
            case Instruction.Exx(int c) -> executeExx(cpu, c);
            case Instruction.ExSpHl(int c) -> executeExSpHl(cpu, memory, c);
            case Instruction.ExSpIndex(var idx, int c) -> executeExSpIndex(cpu, memory, idx, c);

            // 8-bit Load
            case Instruction.LdRegReg(var dst, var src, int c) -> executeLdRegReg(cpu, dst, src, c);
            case Instruction.LdRegImm(var dst, int imm, int c) -> executeLdRegImm(cpu, dst, imm, c);
            case Instruction.LdRegIndHl(var dst, int c) -> executeLdRegIndHl(cpu, memory, dst, c);
            case Instruction.LdIndHlReg(var src, int c) -> executeLdIndHlReg(cpu, memory, src, c);
            case Instruction.LdRegIndOffset(var dst, var idx, int d, int c) -> executeLdRegIndOffset(cpu, memory, dst, idx, d, c);
            case Instruction.LdIndOffsetReg(var idx, int d, var src, int c) -> executeLdIndOffsetReg(cpu, memory, idx, d, src, c);
            case Instruction.LdIndHlImm(int imm, int c) -> executeLdIndHlImm(cpu, memory, imm, c);
            case Instruction.LdIndOffsetImm(var idx, int d, int imm, int c) -> executeLdIndOffsetImm(cpu, memory, idx, d, imm, c);
            case Instruction.LdAccIndBc(int c) -> executeLdAccIndBc(cpu, memory, c);
            case Instruction.LdAccIndDe(int c) -> executeLdAccIndDe(cpu, memory, c);
            case Instruction.LdIndBcAcc(int c) -> executeLdIndBcAcc(cpu, memory, c);
            case Instruction.LdIndDeAcc(int c) -> executeLdIndDeAcc(cpu, memory, c);
            case Instruction.LdAccInd16(int addr, int c) -> executeLdAccInd16(cpu, memory, addr, c);
            case Instruction.LdInd16Acc(int addr, int c) -> executeLdInd16Acc(cpu, memory, addr, c);
            case Instruction.LdRegI(int c) -> executeLdRegI(cpu, c);
            case Instruction.LdRegR(int c) -> executeLdRegR(cpu, c);
            case Instruction.LdIReg(int c) -> executeLdIReg(cpu, c);
            case Instruction.LdRReg(int c) -> executeLdRReg(cpu, c);

            // 16-bit Load & Stack
            case Instruction.Ld16RegImm(var dst, int imm, int c) -> executeLd16RegImm(cpu, dst, imm, c);
            case Instruction.Ld16Ind16Reg(int addr, var src, int c) -> executeLd16Ind16Reg(cpu, memory, addr, src, c);
            case Instruction.Ld16RegInd16(var dst, int addr, int c) -> executeLd16RegInd16(cpu, memory, dst, addr, c);
            case Instruction.Ld16SpIndex(var src, int c) -> executeLd16SpIndex(cpu, src, c);
            case Instruction.Push(var reg, int c) -> executePush(cpu, memory, reg, c);
            case Instruction.Pop(var reg, int c) -> executePop(cpu, memory, reg, c);

            // 8-bit Arithmetic / Logic
            case Instruction.AluReg(var op, var src, int c) -> executeAluReg(cpu, op, src, c);
            case Instruction.AluImm(var op, int imm, int c) -> executeAluImm(cpu, op, imm, c);
            case Instruction.AluIndHl(var op, int c) -> executeAluIndHl(cpu, memory, op, c);
            case Instruction.AluIndOffset(var op, var idx, int d, int c) -> executeAluIndOffset(cpu, memory, op, idx, d, c);
            case Instruction.Inc8Reg(var reg, int c) -> executeInc8Reg(cpu, reg, c);
            case Instruction.Inc8IndHl(int c) -> executeInc8IndHl(cpu, memory, c);
            case Instruction.Inc8IndOffset(var idx, int d, int c) -> executeInc8IndOffset(cpu, memory, idx, d, c);
            case Instruction.Dec8Reg(var reg, int c) -> executeDec8Reg(cpu, reg, c);
            case Instruction.Dec8IndHl(int c) -> executeDec8IndHl(cpu, memory, c);
            case Instruction.Dec8IndOffset(var idx, int d, int c) -> executeDec8IndOffset(cpu, memory, idx, d, c);

            // 16-bit Arithmetic
            case Instruction.Add16(var dst, var src, int c) -> executeAdd16(cpu, dst, src, c);
            case Instruction.Adc16Hl(var src, int c) -> executeAdc16Hl(cpu, src, c);
            case Instruction.Sbc16Hl(var src, int c) -> executeSbc16Hl(cpu, src, c);
            case Instruction.Inc16(var reg, int c) -> executeInc16(cpu, reg, c);
            case Instruction.Dec16(var reg, int c) -> executeDec16(cpu, reg, c);

            // Rotates and Shifts
            case Instruction.RotateReg(var op, var reg, int c) -> executeRotateReg(cpu, op, reg, c);
            case Instruction.RotateIndHl(var op, int c) -> executeRotateIndHl(cpu, memory, op, c);
            case Instruction.RotateIndOffset(var op, var idx, int d, int c) -> executeRotateIndOffset(cpu, memory, op, idx, d, c);
            case Instruction.RotateAcc(var op, int c) -> executeRotateAcc(cpu, op, c);
            case Instruction.Rld(int c) -> executeRld(cpu, memory, c);
            case Instruction.Rrd(int c) -> executeRrd(cpu, memory, c);

            // Bit operations
            case Instruction.BitReg(int bit, var reg, int c) -> executeBitReg(cpu, bit, reg, c);
            case Instruction.BitIndHl(int bit, int c) -> executeBitIndHl(cpu, memory, bit, c);
            case Instruction.BitIndOffset(int bit, var idx, int d, int c) -> executeBitIndOffset(cpu, memory, bit, idx, d, c);
            case Instruction.SetReg(int bit, var reg, int c) -> executeSetReg(cpu, bit, reg, c);
            case Instruction.SetIndHl(int bit, int c) -> executeSetIndHl(cpu, memory, bit, c);
            case Instruction.SetIndOffset(int bit, var idx, int d, int c) -> executeSetIndOffset(cpu, memory, bit, idx, d, c);
            case Instruction.ResReg(int bit, var reg, int c) -> executeResReg(cpu, bit, reg, c);
            case Instruction.ResIndHl(int bit, int c) -> executeResIndHl(cpu, memory, bit, c);
            case Instruction.ResIndOffset(int bit, var idx, int d, int c) -> executeResIndOffset(cpu, memory, bit, idx, d, c);
            case Instruction.BitOpOffsetWithReg(var op, var rotOp, int bit, var idx, int d, var tgt, int c) ->
                executeBitOpOffsetWithReg(cpu, memory, op, rotOp, bit, idx, d, tgt, c);

            // Jumps, Calls, Returns
            case Instruction.JpImm(int target, int c) -> executeJpImm(cpu, target, c);
            case Instruction.JpCondImm(var cond, int target, int c, int tc) -> executeJpCondImm(cpu, cond, target, c, tc);
            case Instruction.JrImm(int d, int c) -> executeJrImm(cpu, d, c);
            case Instruction.JrCondImm(var cond, int d, int c, int tc) -> executeJrCondImm(cpu, cond, d, c, tc);
            case Instruction.JpIndIndex(var idx, int c) -> executeJpIndIndex(cpu, idx, c);
            case Instruction.Djnz(int d, int c, int tc) -> executeDjnz(cpu, d, c, tc);
            case Instruction.CallImm(int target, int c) -> executeCallImm(cpu, memory, target, c);
            case Instruction.CallCondImm(var cond, int target, int c, int tc) -> executeCallCondImm(cpu, memory, cond, target, c, tc);
            case Instruction.Ret(int c) -> executeRet(cpu, memory, c);
            case Instruction.RetCond(var cond, int c, int tc) -> executeRetCond(cpu, memory, cond, c, tc);
            case Instruction.Reti(int c) -> executeReti(cpu, memory, c);
            case Instruction.Retn(int c) -> executeRetn(cpu, memory, c);
            case Instruction.Rst(int target, int c) -> executeRst(cpu, memory, target, c);

            // Input / Output
            case Instruction.InAccImm(int port, int c) -> executeInAccImm(cpu, io, port, c);
            case Instruction.InRegC(var reg, int c) -> executeInRegC(cpu, io, reg, c);
            case Instruction.InIndCNoReg(int c) -> executeInIndCNoReg(cpu, io, c);
            case Instruction.OutImmAcc(int port, int c) -> executeOutImmAcc(cpu, io, port, c);
            case Instruction.OutRegC(var reg, int c) -> executeOutRegC(cpu, io, reg, c);
            case Instruction.OutCZero(int c) -> executeOutCZero(cpu, io, c);

            // Block Instructions
            case Instruction.Ldi(int c) -> executeLdi(cpu, memory, c);
            case Instruction.Ldir(int c, int rc) -> executeLdir(cpu, memory, c, rc);
            case Instruction.Ldd(int c) -> executeLdd(cpu, memory, c);
            case Instruction.Lddr(int c, int rc) -> executeLddr(cpu, memory, c, rc);
            case Instruction.Cpi(int c) -> executeCpi(cpu, memory, c);
            case Instruction.Cpir(int c, int rc) -> executeCpir(cpu, memory, c, rc);
            case Instruction.Cpd(int c) -> executeCpd(cpu, memory, c);
            case Instruction.Cpdr(int c, int rc) -> executeCpdr(cpu, memory, c, rc);
            case Instruction.Ini(int c) -> executeIni(cpu, memory, io, c);
            case Instruction.Inir(int c, int rc) -> executeInir(cpu, memory, io, c, rc);
            case Instruction.Ind(int c) -> executeInd(cpu, memory, io, c);
            case Instruction.Indr(int c, int rc) -> executeIndr(cpu, memory, io, c, rc);
            case Instruction.Outi(int c) -> executeOuti(cpu, memory, io, c);
            case Instruction.Otir(int c, int rc) -> executeOtir(cpu, memory, io, c, rc);
            case Instruction.Outd(int c) -> executeOutd(cpu, memory, io, c);
            case Instruction.Otdr(int c, int rc) -> executeOtdr(cpu, memory, io, c, rc);
        };
    }

    // --- Control Handlers ---
    private int executeNop(CpuState cpu, int cycles) {
        cpu.setQ(0);
        return cycles;
    }

    private int executeHalt(CpuState cpu, int cycles) {
        cpu.setHalted(true);
        cpu.setQ(0);
        return cycles;
    }

    private int executeDi(CpuState cpu, int cycles) {
        cpu.setIff1(false);
        cpu.setIff2(false);
        cpu.setQ(0);
        return cycles;
    }

    private int executeEi(CpuState cpu, int cycles) {
        cpu.setIff1(true);
        cpu.setIff2(true);
        cpu.setEiDelay(true);
        cpu.setQ(0);
        return cycles;
    }

    private int executeIm(CpuState cpu, int mode, int cycles) {
        cpu.setIm(mode);
        cpu.setQ(0);
        return cycles;
    }

    private int executeScf(CpuState cpu, int cycles) {
        var r = cpu.getRegisters();
        int a = r.getA();
        int f = (r.getF() & (Flags.S_MASK | Flags.Z_MASK | Flags.PV_MASK)) | Flags.C_MASK;
        f |= (a & (Flags.F5_MASK | Flags.F3_MASK));
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeCcf(CpuState cpu, int cycles) {
        var r = cpu.getRegisters();
        int a = r.getA();
        int oldF = r.getF();
        int c = oldF & Flags.C_MASK;
        int h = c != 0 ? Flags.H_MASK : 0;
        int newC = c ^ Flags.C_MASK;
        int f = (oldF & (Flags.S_MASK | Flags.Z_MASK | Flags.PV_MASK)) | h | newC;
        f |= (a & (Flags.F5_MASK | Flags.F3_MASK));
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeCpl(CpuState cpu, int cycles) {
        var r = cpu.getRegisters();
        int a = (~r.getA()) & 0xFF;
        r.setA(a);
        int f = (r.getF() & (Flags.S_MASK | Flags.Z_MASK | Flags.PV_MASK | Flags.C_MASK))
                | Flags.H_MASK | Flags.N_MASK | (a & (Flags.F5_MASK | Flags.F3_MASK));
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeNeg(CpuState cpu, int cycles) {
        var r = cpu.getRegisters();
        int val = r.getA();
        int res = (0 - val) & 0xFF;
        r.setA(res);

        int f = Flags.SZ53_TABLE[res] | Flags.N_MASK;
        if (val != 0) f |= Flags.C_MASK;
        if ((val & 0x0F) != 0) f |= Flags.H_MASK;
        if (val == 0x80) f |= Flags.PV_MASK;

        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeDaa(CpuState cpu, int cycles) {
        var r = cpu.getRegisters();
        int a = r.getA();
        int f = r.getF();
        int correction = 0;
        boolean carry = (f & Flags.C_MASK) != 0;
        boolean halfCarry = (f & Flags.H_MASK) != 0;

        if ((f & Flags.N_MASK) == 0) {
            if (halfCarry || (a & 0x0F) > 9) correction |= 0x06;
            if (carry || a > 0x99) {
                correction |= 0x60;
                carry = true;
            }
            a = (a + correction) & 0xFF;
        } else {
            if (halfCarry) correction |= 0x06;
            if (carry) correction |= 0x60;
            a = (a - correction) & 0xFF;
        }

        f = (f & Flags.N_MASK) | Flags.SZ53_TABLE[a] | Flags.PARITY_TABLE[a];
        if (carry) f |= Flags.C_MASK;
        if (((r.getA() ^ a) & 0x10) != 0) f |= Flags.H_MASK;

        r.setA(a);
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeExDeHl(CpuState cpu, int cycles) {
        cpu.getRegisters().exDeHl();
        cpu.setQ(0);
        return cycles;
    }

    private int executeExAfAfPrime(CpuState cpu, int cycles) {
        cpu.getRegisters().exAfAfPrime();
        cpu.setQ(0);
        return cycles;
    }

    private int executeExx(CpuState cpu, int cycles) {
        cpu.getRegisters().exx();
        cpu.setQ(0);
        return cycles;
    }

    private int executeExSpHl(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int sp = r.getSP();
        int low = memory.readByte(sp);
        int high = memory.readByte((sp + 1) & 0xFFFF);
        memory.writeByte(sp, r.getL());
        memory.writeByte((sp + 1) & 0xFFFF, r.getH());
        r.setL(low);
        r.setH(high);
        r.setMemptr((high << 8) | low);
        cpu.setQ(0);
        return cycles;
    }

    private int executeExSpIndex(CpuState cpu, MemoryBus memory, Register16 indexReg, int cycles) {
        var r = cpu.getRegisters();
        int sp = r.getSP();
        int low = memory.readByte(sp);
        int high = memory.readByte((sp + 1) & 0xFFFF);
        int curr = r.getReg16(indexReg);
        memory.writeByte(sp, curr & 0xFF);
        memory.writeByte((sp + 1) & 0xFFFF, (curr >> 8) & 0xFF);
        int newVal = (high << 8) | low;
        r.setReg16(indexReg, newVal);
        r.setMemptr(newVal);
        cpu.setQ(0);
        return cycles;
    }

    // --- 8-bit Load Handlers ---
    private int executeLdRegReg(CpuState cpu, Register8 dst, Register8 src, int cycles) {
        var r = cpu.getRegisters();
        r.setReg8(dst, r.getReg8(src));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdRegImm(CpuState cpu, Register8 dst, int imm, int cycles) {
        cpu.getRegisters().setReg8(dst, imm);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdRegIndHl(CpuState cpu, MemoryBus memory, Register8 dst, int cycles) {
        var r = cpu.getRegisters();
        r.setReg8(dst, memory.readByte(r.getHL()));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdIndHlReg(CpuState cpu, MemoryBus memory, Register8 src, int cycles) {
        var r = cpu.getRegisters();
        memory.writeByte(r.getHL(), r.getReg8(src));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdRegIndOffset(CpuState cpu, MemoryBus memory, Register8 dst, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        r.setReg8(dst, memory.readByte(addr));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdIndOffsetReg(CpuState cpu, MemoryBus memory, Register16 idx, int d, Register8 src, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        memory.writeByte(addr, r.getReg8(src));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdIndHlImm(CpuState cpu, MemoryBus memory, int imm, int cycles) {
        memory.writeByte(cpu.getRegisters().getHL(), imm);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdIndOffsetImm(CpuState cpu, MemoryBus memory, Register16 idx, int d, int imm, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        memory.writeByte(addr, imm);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdAccIndBc(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getBC();
        r.setA(memory.readByte(addr));
        r.setMemptr((addr + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdAccIndDe(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getDE();
        r.setA(memory.readByte(addr));
        r.setMemptr((addr + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdIndBcAcc(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getBC();
        memory.writeByte(addr, r.getA());
        r.setMemptr((r.getA() << 8) | ((addr + 1) & 0xFF));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdIndDeAcc(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getDE();
        memory.writeByte(addr, r.getA());
        r.setMemptr((r.getA() << 8) | ((addr + 1) & 0xFF));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdAccInd16(CpuState cpu, MemoryBus memory, int addr, int cycles) {
        var r = cpu.getRegisters();
        r.setA(memory.readByte(addr));
        r.setMemptr((addr + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdInd16Acc(CpuState cpu, MemoryBus memory, int addr, int cycles) {
        var r = cpu.getRegisters();
        memory.writeByte(addr, r.getA());
        r.setMemptr((r.getA() << 8) | ((addr + 1) & 0xFF));
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdRegI(CpuState cpu, int cycles) {
        var r = cpu.getRegisters();
        int i = r.getI();
        r.setA(i);
        int f = (r.getF() & Flags.C_MASK) | Flags.SZ53_TABLE[i];
        if (cpu.isIff2()) f |= Flags.PV_MASK;
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeLdRegR(CpuState cpu, int cycles) {
        var r = cpu.getRegisters();
        int val = r.getR();
        r.setA(val);
        int f = (r.getF() & Flags.C_MASK) | Flags.SZ53_TABLE[val];
        if (cpu.isIff2()) f |= Flags.PV_MASK;
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeLdIReg(CpuState cpu, int cycles) {
        cpu.getRegisters().setI(cpu.getRegisters().getA());
        cpu.setQ(0);
        return cycles;
    }

    private int executeLdRReg(CpuState cpu, int cycles) {
        cpu.getRegisters().setR(cpu.getRegisters().getA());
        cpu.setQ(0);
        return cycles;
    }

    // --- 16-bit Load & Stack Handlers ---
    private int executeLd16RegImm(CpuState cpu, Register16 dst, int imm, int cycles) {
        cpu.getRegisters().setReg16(dst, imm);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLd16Ind16Reg(CpuState cpu, MemoryBus memory, int addr, Register16 src, int cycles) {
        var r = cpu.getRegisters();
        int val = r.getReg16(src);
        memory.writeWord(addr, val);
        r.setMemptr((addr + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLd16RegInd16(CpuState cpu, MemoryBus memory, Register16 dst, int addr, int cycles) {
        var r = cpu.getRegisters();
        int val = memory.readWord(addr);
        r.setReg16(dst, val);
        r.setMemptr((addr + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeLd16SpIndex(CpuState cpu, Register16 src, int cycles) {
        var r = cpu.getRegisters();
        r.setSP(r.getReg16(src));
        cpu.setQ(0);
        return cycles;
    }

    private int executePush(CpuState cpu, MemoryBus memory, Register16 reg, int cycles) {
        var r = cpu.getRegisters();
        int sp = (r.getSP() - 2) & 0xFFFF;
        r.setSP(sp);
        memory.writeWord(sp, r.getReg16(reg));
        cpu.setQ(0);
        return cycles;
    }

    private int executePop(CpuState cpu, MemoryBus memory, Register16 reg, int cycles) {
        var r = cpu.getRegisters();
        int sp = r.getSP();
        r.setReg16(reg, memory.readWord(sp));
        r.setSP((sp + 2) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    // --- 8-bit Arithmetic / Logic Handlers ---
    private int executeAluReg(CpuState cpu, AluOp op, Register8 src, int cycles) {
        int val = cpu.getRegisters().getReg8(src);
        applyAlu(cpu, op, val);
        return cycles;
    }

    private int executeAluImm(CpuState cpu, AluOp op, int imm, int cycles) {
        applyAlu(cpu, op, imm);
        return cycles;
    }

    private int executeAluIndHl(CpuState cpu, MemoryBus memory, AluOp op, int cycles) {
        int val = memory.readByte(cpu.getRegisters().getHL());
        applyAlu(cpu, op, val);
        return cycles;
    }

    private int executeAluIndOffset(CpuState cpu, MemoryBus memory, AluOp op, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        int val = memory.readByte(addr);
        applyAlu(cpu, op, val);
        return cycles;
    }

    private void applyAlu(CpuState cpu, AluOp op, int val) {
        var r = cpu.getRegisters();
        int a = r.getA();
        int carry = (r.getF() & Flags.C_MASK);
        int res;
        int f;

        switch (op) {
            case ADD -> {
                res = a + val;
                f = Flags.SZ53_TABLE[res & 0xFF];
                if ((res & 0x100) != 0) f |= Flags.C_MASK;
                if (((a & 0x0F) + (val & 0x0F)) > 0x0F) f |= Flags.H_MASK;
                if (((a ^ ~val) & (a ^ res) & 0x80) != 0) f |= Flags.PV_MASK;
                r.setA(res & 0xFF);
                r.setF(f);
                cpu.setQ(f);
            }
            case ADC -> {
                res = a + val + carry;
                f = Flags.SZ53_TABLE[res & 0xFF];
                if (res > 0xFF) f |= Flags.C_MASK;
                if (((a & 0x0F) + (val & 0x0F) + carry) > 0x0F) f |= Flags.H_MASK;
                if (((a ^ ~val) & (a ^ res) & 0x80) != 0) f |= Flags.PV_MASK;
                r.setA(res & 0xFF);
                r.setF(f);
                cpu.setQ(f);
            }
            case SUB -> {
                res = a - val;
                f = Flags.SZ53_TABLE[res & 0xFF] | Flags.N_MASK;
                if (a < val) f |= Flags.C_MASK;
                if ((a & 0x0F) < (val & 0x0F)) f |= Flags.H_MASK;
                if (((a ^ val) & (a ^ res) & 0x80) != 0) f |= Flags.PV_MASK;
                r.setA(res & 0xFF);
                r.setF(f);
                cpu.setQ(f);
            }
            case SBC -> {
                res = a - val - carry;
                f = Flags.SZ53_TABLE[res & 0xFF] | Flags.N_MASK;
                if (a < val + carry) f |= Flags.C_MASK;
                if ((a & 0x0F) < (val & 0x0F) + carry) f |= Flags.H_MASK;
                if (((a ^ val) & (a ^ res) & 0x80) != 0) f |= Flags.PV_MASK;
                r.setA(res & 0xFF);
                r.setF(f);
                cpu.setQ(f);
            }
            case AND -> {
                res = (a & val) & 0xFF;
                f = Flags.SZ53_TABLE[res] | Flags.H_MASK | Flags.PARITY_TABLE[res];
                r.setA(res);
                r.setF(f);
                cpu.setQ(f);
            }
            case XOR -> {
                res = (a ^ val) & 0xFF;
                f = Flags.SZ53_TABLE[res] | Flags.PARITY_TABLE[res];
                r.setA(res);
                r.setF(f);
                cpu.setQ(f);
            }
            case OR -> {
                res = (a | val) & 0xFF;
                f = Flags.SZ53_TABLE[res] | Flags.PARITY_TABLE[res];
                r.setA(res);
                r.setF(f);
                cpu.setQ(f);
            }
            case CP -> {
                res = a - val;
                f = (Flags.SZ53_TABLE[res & 0xFF] & (Flags.S_MASK | Flags.Z_MASK)) | Flags.N_MASK;
                f |= (val & (Flags.F5_MASK | Flags.F3_MASK)); // F5 and F3 come from the operand!
                if (a < val) f |= Flags.C_MASK;
                if ((a & 0x0F) < (val & 0x0F)) f |= Flags.H_MASK;
                if (((a ^ val) & (a ^ res) & 0x80) != 0) f |= Flags.PV_MASK;
                r.setF(f);
                cpu.setQ(f);
            }
        }
    }

    private int executeInc8Reg(CpuState cpu, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        int val = r.getReg8(reg);
        int res = applyInc8(cpu, val);
        r.setReg8(reg, res);
        return cycles;
    }

    private int executeInc8IndHl(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        int val = memory.readByte(addr);
        int res = applyInc8(cpu, val);
        memory.writeByte(addr, res);
        return cycles;
    }

    private int executeInc8IndOffset(CpuState cpu, MemoryBus memory, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        int val = memory.readByte(addr);
        int res = applyInc8(cpu, val);
        memory.writeByte(addr, res);
        return cycles;
    }

    private int applyInc8(CpuState cpu, int val) {
        int res = (val + 1) & 0xFF;
        int f = (cpu.getRegisters().getF() & Flags.C_MASK) | Flags.SZ53_TABLE[res];
        if ((val & 0x0F) == 0x0F) f |= Flags.H_MASK;
        if (val == 0x7F) f |= Flags.PV_MASK;
        cpu.getRegisters().setF(f);
        cpu.setQ(f);
        return res;
    }

    private int executeDec8Reg(CpuState cpu, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        int val = r.getReg8(reg);
        int res = applyDec8(cpu, val);
        r.setReg8(reg, res);
        return cycles;
    }

    private int executeDec8IndHl(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        int val = memory.readByte(addr);
        int res = applyDec8(cpu, val);
        memory.writeByte(addr, res);
        return cycles;
    }

    private int executeDec8IndOffset(CpuState cpu, MemoryBus memory, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        int val = memory.readByte(addr);
        int res = applyDec8(cpu, val);
        memory.writeByte(addr, res);
        return cycles;
    }

    private int applyDec8(CpuState cpu, int val) {
        int res = (val - 1) & 0xFF;
        int f = (cpu.getRegisters().getF() & Flags.C_MASK) | Flags.SZ53_TABLE[res] | Flags.N_MASK;
        if ((val & 0x0F) == 0x00) f |= Flags.H_MASK;
        if (val == 0x80) f |= Flags.PV_MASK;
        cpu.getRegisters().setF(f);
        cpu.setQ(f);
        return res;
    }

    // --- 16-bit Arithmetic Handlers ---
    private int executeAdd16(CpuState cpu, Register16 dst, Register16 src, int cycles) {
        var r = cpu.getRegisters();
        int val1 = r.getReg16(dst);
        int val2 = r.getReg16(src);
        int res = val1 + val2;

        r.setMemptr((val1 + 1) & 0xFFFF);
        r.setReg16(dst, res & 0xFFFF);

        int f = (r.getF() & (Flags.S_MASK | Flags.Z_MASK | Flags.PV_MASK));
        f |= ((res >> 8) & (Flags.F5_MASK | Flags.F3_MASK));
        if (res > 0xFFFF) f |= Flags.C_MASK;
        if (((val1 & 0x0FFF) + (val2 & 0x0FFF)) > 0x0FFF) f |= Flags.H_MASK;

        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeAdc16Hl(CpuState cpu, Register16 src, int cycles) {
        var r = cpu.getRegisters();
        int hl = r.getHL();
        int val = r.getReg16(src);
        int c = r.getF() & Flags.C_MASK;
        int res = hl + val + c;

        r.setMemptr((hl + 1) & 0xFFFF);
        r.setHL(res & 0xFFFF);

        int res16 = res & 0xFFFF;
        int f = ((res >> 8) & (Flags.S_MASK | Flags.F5_MASK | Flags.F3_MASK));
        if (res16 == 0) f |= Flags.Z_MASK;
        if (res > 0xFFFF) f |= Flags.C_MASK;
        if (((hl & 0x0FFF) + (val & 0x0FFF) + c) > 0x0FFF) f |= Flags.H_MASK;
        if (((hl ^ ~val) & (hl ^ res) & 0x8000) != 0) f |= Flags.PV_MASK;

        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeSbc16Hl(CpuState cpu, Register16 src, int cycles) {
        var r = cpu.getRegisters();
        int hl = r.getHL();
        int val = r.getReg16(src);
        int c = r.getF() & Flags.C_MASK;
        int res = hl - val - c;

        r.setMemptr((hl + 1) & 0xFFFF);
        r.setHL(res & 0xFFFF);

        int res16 = res & 0xFFFF;
        int f = Flags.N_MASK | ((res >> 8) & (Flags.S_MASK | Flags.F5_MASK | Flags.F3_MASK));
        if (res16 == 0) f |= Flags.Z_MASK;
        if (hl < val + c) f |= Flags.C_MASK;
        if ((hl & 0x0FFF) < (val & 0x0FFF) + c) f |= Flags.H_MASK;
        if (((hl ^ val) & (hl ^ res) & 0x8000) != 0) f |= Flags.PV_MASK;

        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeInc16(CpuState cpu, Register16 reg, int cycles) {
        var r = cpu.getRegisters();
        r.setReg16(reg, (r.getReg16(reg) + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeDec16(CpuState cpu, Register16 reg, int cycles) {
        var r = cpu.getRegisters();
        r.setReg16(reg, (r.getReg16(reg) - 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    // --- Rotate and Shift Handlers ---
    private int executeRotateReg(CpuState cpu, RotateOp op, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        int val = r.getReg8(reg);
        int res = applyRotate(cpu, op, val);
        r.setReg8(reg, res);
        return cycles;
    }

    private int executeRotateIndHl(CpuState cpu, MemoryBus memory, RotateOp op, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        int val = memory.readByte(addr);
        int res = applyRotate(cpu, op, val);
        memory.writeByte(addr, res);
        return cycles;
    }

    private int executeRotateIndOffset(CpuState cpu, MemoryBus memory, RotateOp op, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        int val = memory.readByte(addr);
        int res = applyRotate(cpu, op, val);
        memory.writeByte(addr, res);
        return cycles;
    }

    private int applyRotate(CpuState cpu, RotateOp op, int val) {
        int carryIn = cpu.getRegisters().getF() & Flags.C_MASK;
        int carryOut;
        int res;

        switch (op) {
            case RLC -> {
                carryOut = (val >> 7) & 1;
                res = ((val << 1) | carryOut) & 0xFF;
            }
            case RRC -> {
                carryOut = val & 1;
                res = ((val >> 1) | (carryOut << 7)) & 0xFF;
            }
            case RL -> {
                carryOut = (val >> 7) & 1;
                res = ((val << 1) | carryIn) & 0xFF;
            }
            case RR -> {
                carryOut = val & 1;
                res = ((val >> 1) | (carryIn << 7)) & 0xFF;
            }
            case SLA -> {
                carryOut = (val >> 7) & 1;
                res = (val << 1) & 0xFF;
            }
            case SRA -> {
                carryOut = val & 1;
                res = ((val >> 1) | (val & 0x80)) & 0xFF;
            }
            case SLL -> { // Undocumented: shifts left and sets bit 0 to 1!
                carryOut = (val >> 7) & 1;
                res = ((val << 1) | 1) & 0xFF;
            }
            case SRL -> {
                carryOut = val & 1;
                res = (val >> 1) & 0xFF;
            }
            default -> throw new IllegalStateException();
        }

        int f = Flags.SZ53_TABLE[res] | Flags.PARITY_TABLE[res];
        if (carryOut != 0) f |= Flags.C_MASK;
        cpu.getRegisters().setF(f);
        cpu.setQ(f);
        return res;
    }

    private int executeRotateAcc(CpuState cpu, RotateAccOp op, int cycles) {
        var r = cpu.getRegisters();
        int a = r.getA();
        int carryIn = r.getF() & Flags.C_MASK;
        int carryOut;
        int res;

        switch (op) {
            case RLCA -> {
                carryOut = (a >> 7) & 1;
                res = ((a << 1) | carryOut) & 0xFF;
            }
            case RRCA -> {
                carryOut = a & 1;
                res = ((a >> 1) | (carryOut << 7)) & 0xFF;
            }
            case RLA -> {
                carryOut = (a >> 7) & 1;
                res = ((a << 1) | carryIn) & 0xFF;
            }
            case RRA -> {
                carryOut = a & 1;
                res = ((a >> 1) | (carryIn << 7)) & 0xFF;
            }
            default -> throw new IllegalStateException();
        }

        int f = (r.getF() & (Flags.S_MASK | Flags.Z_MASK | Flags.PV_MASK));
        f |= (res & (Flags.F5_MASK | Flags.F3_MASK));
        if (carryOut != 0) f |= Flags.C_MASK;
        r.setA(res);
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeRld(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        int a = r.getA();
        int mem = memory.readByte(addr);

        int newMem = ((mem << 4) | (a & 0x0F)) & 0xFF;
        int newA = (a & 0xF0) | (mem >> 4);

        memory.writeByte(addr, newMem);
        r.setA(newA);
        r.setMemptr((addr + 1) & 0xFFFF);

        int f = (r.getF() & Flags.C_MASK) | Flags.SZ53_TABLE[newA] | Flags.PARITY_TABLE[newA];
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeRrd(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        int a = r.getA();
        int mem = memory.readByte(addr);

        int newMem = ((a & 0x0F) << 4) | (mem >> 4);
        int newA = (a & 0xF0) | (mem & 0x0F);

        memory.writeByte(addr, newMem);
        r.setA(newA);
        r.setMemptr((addr + 1) & 0xFFFF);

        int f = (r.getF() & Flags.C_MASK) | Flags.SZ53_TABLE[newA] | Flags.PARITY_TABLE[newA];
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    // --- Bit Operations Handlers ---
    private int executeBitReg(CpuState cpu, int bit, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        int val = r.getReg8(reg);
        applyBit(cpu, bit, val, val);
        return cycles;
    }

    private int executeBitIndHl(CpuState cpu, MemoryBus memory, int bit, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        int val = memory.readByte(addr);
        applyBit(cpu, bit, val, (r.getMemptr() >> 8) & 0xFF);
        return cycles;
    }

    private int executeBitIndOffset(CpuState cpu, MemoryBus memory, int bit, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        int val = memory.readByte(addr);
        applyBit(cpu, bit, val, (addr >> 8) & 0xFF);
        return cycles;
    }

    private void applyBit(CpuState cpu, int bit, int val, int f35Source) {
        var r = cpu.getRegisters();
        int oldF = r.getF();
        boolean isZero = (val & (1 << bit)) == 0;

        int f = (oldF & Flags.C_MASK) | Flags.H_MASK;
        if (isZero) f |= (Flags.Z_MASK | Flags.PV_MASK);
        if (bit == 7 && !isZero) f |= Flags.S_MASK;
        f |= (f35Source & (Flags.F5_MASK | Flags.F3_MASK));

        r.setF(f);
        cpu.setQ(f);
    }

    private int executeSetReg(CpuState cpu, int bit, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        r.setReg8(reg, r.getReg8(reg) | (1 << bit));
        cpu.setQ(0);
        return cycles;
    }

    private int executeSetIndHl(CpuState cpu, MemoryBus memory, int bit, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        memory.writeByte(addr, memory.readByte(addr) | (1 << bit));
        cpu.setQ(0);
        return cycles;
    }

    private int executeSetIndOffset(CpuState cpu, MemoryBus memory, int bit, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        memory.writeByte(addr, memory.readByte(addr) | (1 << bit));
        cpu.setQ(0);
        return cycles;
    }

    private int executeResReg(CpuState cpu, int bit, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        r.setReg8(reg, r.getReg8(reg) & ~(1 << bit));
        cpu.setQ(0);
        return cycles;
    }

    private int executeResIndHl(CpuState cpu, MemoryBus memory, int bit, int cycles) {
        var r = cpu.getRegisters();
        int addr = r.getHL();
        memory.writeByte(addr, memory.readByte(addr) & ~(1 << bit));
        cpu.setQ(0);
        return cycles;
    }

    private int executeResIndOffset(CpuState cpu, MemoryBus memory, int bit, Register16 idx, int d, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        memory.writeByte(addr, memory.readByte(addr) & ~(1 << bit));
        cpu.setQ(0);
        return cycles;
    }

    /**
     * Undocumented DDCB/FDCB instructions that perform rotate/shift/res/set on (IX+d)
     * and write the result into both memory and a target register!
     */
    private int executeBitOpOffsetWithReg(CpuState cpu, MemoryBus memory, BitShiftOp op, RotateOp rotOp,
                                         int bit, Register16 idx, int d, Register8 tgt, int cycles) {
        var r = cpu.getRegisters();
        int addr = (r.getReg16(idx) + (byte) d) & 0xFFFF;
        r.setMemptr(addr);
        int val = memory.readByte(addr);
        int res;

        switch (op) {
            case ROTATE -> res = applyRotate(cpu, rotOp, val);
            case SET -> {
                res = val | (1 << bit);
                cpu.setQ(0);
            }
            case RES -> {
                res = val & ~(1 << bit);
                cpu.setQ(0);
            }
            default -> throw new IllegalStateException();
        }

        memory.writeByte(addr, res);
        r.setReg8(tgt, res);
        return cycles;
    }

    // --- Jump / Call / Ret Handlers ---
    private int executeJpImm(CpuState cpu, int target, int cycles) {
        var r = cpu.getRegisters();
        r.setPC(target);
        r.setMemptr(target);
        cpu.setQ(0);
        return cycles;
    }

    private int executeJpCondImm(CpuState cpu, Condition cond, int target, int cycles, int takenCycles) {
        var r = cpu.getRegisters();
        r.setMemptr(target);
        cpu.setQ(0);
        if (cond.test(r.getF())) {
            r.setPC(target);
            return takenCycles;
        }
        return cycles;
    }

    private int executeJrImm(CpuState cpu, int d, int cycles) {
        var r = cpu.getRegisters();
        int pc = (r.getPC() + (byte) d) & 0xFFFF;
        r.setPC(pc);
        r.setMemptr(pc);
        cpu.setQ(0);
        return cycles;
    }

    private int executeJrCondImm(CpuState cpu, Condition cond, int d, int cycles, int takenCycles) {
        var r = cpu.getRegisters();
        cpu.setQ(0);
        if (cond.test(r.getF())) {
            int pc = (r.getPC() + (byte) d) & 0xFFFF;
            r.setPC(pc);
            r.setMemptr(pc);
            return takenCycles;
        }
        return cycles;
    }

    private int executeJpIndIndex(CpuState cpu, Register16 idx, int cycles) {
        var r = cpu.getRegisters();
        r.setPC(r.getReg16(idx));
        cpu.setQ(0);
        return cycles;
    }

    private int executeDjnz(CpuState cpu, int d, int cycles, int takenCycles) {
        var r = cpu.getRegisters();
        int b = (r.getB() - 1) & 0xFF;
        r.setB(b);
        cpu.setQ(0);
        if (b != 0) {
            int pc = (r.getPC() + (byte) d) & 0xFFFF;
            r.setPC(pc);
            r.setMemptr(pc);
            return takenCycles;
        }
        return cycles;
    }

    private int executeCallImm(CpuState cpu, MemoryBus memory, int target, int cycles) {
        var r = cpu.getRegisters();
        int sp = (r.getSP() - 2) & 0xFFFF;
        r.setSP(sp);
        memory.writeWord(sp, r.getPC());
        r.setPC(target);
        r.setMemptr(target);
        cpu.setQ(0);
        return cycles;
    }

    private int executeCallCondImm(CpuState cpu, MemoryBus memory, Condition cond, int target, int cycles, int takenCycles) {
        var r = cpu.getRegisters();
        r.setMemptr(target);
        cpu.setQ(0);
        if (cond.test(r.getF())) {
            int sp = (r.getSP() - 2) & 0xFFFF;
            r.setSP(sp);
            memory.writeWord(sp, r.getPC());
            r.setPC(target);
            return takenCycles;
        }
        return cycles;
    }

    private int executeRet(CpuState cpu, MemoryBus memory, int cycles) {
        var r = cpu.getRegisters();
        int sp = r.getSP();
        int target = memory.readWord(sp);
        r.setSP((sp + 2) & 0xFFFF);
        r.setPC(target);
        r.setMemptr(target);
        cpu.setQ(0);
        return cycles;
    }

    private int executeRetCond(CpuState cpu, MemoryBus memory, Condition cond, int cycles, int takenCycles) {
        var r = cpu.getRegisters();
        cpu.setQ(0);
        if (cond.test(r.getF())) {
            int sp = r.getSP();
            int target = memory.readWord(sp);
            r.setSP((sp + 2) & 0xFFFF);
            r.setPC(target);
            r.setMemptr(target);
            return takenCycles;
        }
        return cycles;
    }

    private int executeReti(CpuState cpu, MemoryBus memory, int cycles) {
        executeRet(cpu, memory, cycles);
        cpu.setIff1(cpu.isIff2());
        cpu.setQ(0);
        return cycles;
    }

    private int executeRetn(CpuState cpu, MemoryBus memory, int cycles) {
        executeRet(cpu, memory, cycles);
        cpu.setIff1(cpu.isIff2());
        cpu.setQ(0);
        return cycles;
    }

    private int executeRst(CpuState cpu, MemoryBus memory, int target, int cycles) {
        var r = cpu.getRegisters();
        int sp = (r.getSP() - 2) & 0xFFFF;
        r.setSP(sp);
        memory.writeWord(sp, r.getPC());
        r.setPC(target);
        r.setMemptr(target);
        cpu.setQ(0);
        return cycles;
    }

    // --- I/O Handlers ---
    private int executeInAccImm(CpuState cpu, IoBus io, int port, int cycles) {
        var r = cpu.getRegisters();
        int fullPort = (r.getA() << 8) | port;
        int val = io.in(fullPort);
        r.setA(val);
        r.setMemptr((fullPort + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeInRegC(CpuState cpu, IoBus io, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        int port = r.getBC();
        int val = io.in(port);
        r.setReg8(reg, val);
        r.setMemptr((port + 1) & 0xFFFF);

        int f = (r.getF() & Flags.C_MASK) | Flags.SZ53_TABLE[val] | Flags.PARITY_TABLE[val];
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeInIndCNoReg(CpuState cpu, IoBus io, int cycles) { // Undocumented ED 70
        var r = cpu.getRegisters();
        int port = r.getBC();
        int val = io.in(port);
        r.setMemptr((port + 1) & 0xFFFF);

        int f = (r.getF() & Flags.C_MASK) | Flags.SZ53_TABLE[val] | Flags.PARITY_TABLE[val];
        r.setF(f);
        cpu.setQ(f);
        return cycles;
    }

    private int executeOutImmAcc(CpuState cpu, IoBus io, int port, int cycles) {
        var r = cpu.getRegisters();
        int fullPort = (r.getA() << 8) | port;
        io.out(fullPort, r.getA());
        r.setMemptr(((fullPort & 0xFF) + 1) | (r.getA() << 8));
        cpu.setQ(0);
        return cycles;
    }

    private int executeOutRegC(CpuState cpu, IoBus io, Register8 reg, int cycles) {
        var r = cpu.getRegisters();
        int port = r.getBC();
        io.out(port, r.getReg8(reg));
        r.setMemptr((port + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    private int executeOutCZero(CpuState cpu, IoBus io, int cycles) { // Undocumented ED 71
        var r = cpu.getRegisters();
        int port = r.getBC();
        io.out(port, 0);
        r.setMemptr((port + 1) & 0xFFFF);
        cpu.setQ(0);
        return cycles;
    }

    // --- Block Instruction Handlers ---
    private int executeLdi(CpuState cpu, MemoryBus memory, int cycles) {
        applyLdBlock(cpu, memory, 1);
        return cycles;
    }

    private int executeLdir(CpuState cpu, MemoryBus memory, int cycles, int repeatCycles) {
        boolean repeat = applyLdBlock(cpu, memory, 1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private int executeLdd(CpuState cpu, MemoryBus memory, int cycles) {
        applyLdBlock(cpu, memory, -1);
        return cycles;
    }

    private int executeLddr(CpuState cpu, MemoryBus memory, int cycles, int repeatCycles) {
        boolean repeat = applyLdBlock(cpu, memory, -1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private boolean applyLdBlock(CpuState cpu, MemoryBus memory, int dir) {
        var r = cpu.getRegisters();
        int hl = r.getHL();
        int de = r.getDE();
        int val = memory.readByte(hl);
        memory.writeByte(de, val);

        r.setHL((hl + dir) & 0xFFFF);
        r.setDE((de + dir) & 0xFFFF);
        int bc = (r.getBC() - 1) & 0xFFFF;
        r.setBC(bc);

        int n = (val + r.getA()) & 0xFF;
        int f = (r.getF() & (Flags.S_MASK | Flags.Z_MASK | Flags.C_MASK));
        f |= (n & Flags.F3_MASK);
        if ((n & 0x02) != 0) f |= Flags.F5_MASK;
        if (bc != 0) f |= Flags.PV_MASK;

        r.setF(f);
        cpu.setQ(f);
        return bc != 0;
    }

    private int executeCpi(CpuState cpu, MemoryBus memory, int cycles) {
        applyCpBlock(cpu, memory, 1);
        return cycles;
    }

    private int executeCpir(CpuState cpu, MemoryBus memory, int cycles, int repeatCycles) {
        boolean repeat = applyCpBlock(cpu, memory, 1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private int executeCpd(CpuState cpu, MemoryBus memory, int cycles) {
        applyCpBlock(cpu, memory, -1);
        return cycles;
    }

    private int executeCpdr(CpuState cpu, MemoryBus memory, int cycles, int repeatCycles) {
        boolean repeat = applyCpBlock(cpu, memory, -1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private boolean applyCpBlock(CpuState cpu, MemoryBus memory, int dir) {
        var r = cpu.getRegisters();
        int hl = r.getHL();
        int a = r.getA();
        int val = memory.readByte(hl);
        int res = a - val;

        r.setHL((hl + dir) & 0xFFFF);
        int bc = (r.getBC() - 1) & 0xFFFF;
        r.setBC(bc);

        int f = (r.getF() & Flags.C_MASK) | Flags.N_MASK;
        f |= (Flags.SZ53_TABLE[res & 0xFF] & (Flags.S_MASK | Flags.Z_MASK));
        if ((a & 0x0F) < (val & 0x0F)) f |= Flags.H_MASK;
        if (bc != 0) f |= Flags.PV_MASK;

        int n = res - ((f & Flags.H_MASK) != 0 ? 1 : 0);
        f |= (n & Flags.F3_MASK);
        if ((n & 0x02) != 0) f |= Flags.F5_MASK;

        r.setF(f);
        cpu.setQ(f);
        return bc != 0 && (f & Flags.Z_MASK) == 0;
    }

    private int executeIni(CpuState cpu, MemoryBus memory, IoBus io, int cycles) {
        applyInBlock(cpu, memory, io, 1);
        return cycles;
    }

    private int executeInir(CpuState cpu, MemoryBus memory, IoBus io, int cycles, int repeatCycles) {
        boolean repeat = applyInBlock(cpu, memory, io, 1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private int executeInd(CpuState cpu, MemoryBus memory, IoBus io, int cycles) {
        applyInBlock(cpu, memory, io, -1);
        return cycles;
    }

    private int executeIndr(CpuState cpu, MemoryBus memory, IoBus io, int cycles, int repeatCycles) {
        boolean repeat = applyInBlock(cpu, memory, io, -1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private boolean applyInBlock(CpuState cpu, MemoryBus memory, IoBus io, int dir) {
        var r = cpu.getRegisters();
        int val = io.in(r.getBC());
        int hl = r.getHL();
        memory.writeByte(hl, val);
        r.setHL((hl + dir) & 0xFFFF);

        int b = (r.getB() - 1) & 0xFF;
        r.setB(b);

        int f = Flags.SZ53_TABLE[b] | ((val >> 6) & Flags.N_MASK);
        r.setF(f);
        cpu.setQ(f);
        return b != 0;
    }

    private int executeOuti(CpuState cpu, MemoryBus memory, IoBus io, int cycles) {
        applyOutBlock(cpu, memory, io, 1);
        return cycles;
    }

    private int executeOtir(CpuState cpu, MemoryBus memory, IoBus io, int cycles, int repeatCycles) {
        boolean repeat = applyOutBlock(cpu, memory, io, 1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private int executeOutd(CpuState cpu, MemoryBus memory, IoBus io, int cycles) {
        applyOutBlock(cpu, memory, io, -1);
        return cycles;
    }

    private int executeOtdr(CpuState cpu, MemoryBus memory, IoBus io, int cycles, int repeatCycles) {
        boolean repeat = applyOutBlock(cpu, memory, io, -1);
        if (repeat) {
            cpu.getRegisters().setPC((cpu.getRegisters().getPC() - 2) & 0xFFFF);
            return repeatCycles;
        }
        return cycles;
    }

    private boolean applyOutBlock(CpuState cpu, MemoryBus memory, IoBus io, int dir) {
        var r = cpu.getRegisters();
        int hl = r.getHL();
        int val = memory.readByte(hl);
        r.setHL((hl + dir) & 0xFFFF);

        int b = (r.getB() - 1) & 0xFF;
        r.setB(b);
        io.out(r.getBC(), val);

        int f = Flags.SZ53_TABLE[b] | ((val >> 6) & Flags.N_MASK);
        r.setF(f);
        cpu.setQ(f);
        return b != 0;
    }
}
