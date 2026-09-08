package com.spectrum.cpu;

/**
 * Z80 CPU registers state including main registers, alternate registers,
 * index registers (IX, IY), pointers (SP, PC), system registers (I, R), and MEMPTR (WZ).
 */
public final class Registers {
    // 8-bit main registers
    private int a;
    private int f;
    private int b;
    private int c;
    private int d;
    private int e;
    private int h;
    private int l;

    // Alternate registers
    private int aPrime;
    private int fPrime;
    private int bPrime;
    private int cPrime;
    private int dPrime;
    private int ePrime;
    private int hPrime;
    private int lPrime;

    // Index registers
    private int ix;
    private int iy;

    // Pointers
    private int sp;
    private int pc;

    // Interrupt vector and memory refresh
    private int i;
    private int r;

    // Internal WZ / MEMPTR register (crucial for undocumented flag tests)
    private int memptr;

    public void reset() {
        a = 0xFF;
        f = 0xFF;
        b = 0xFF;
        c = 0xFF;
        d = 0xFF;
        e = 0xFF;
        h = 0xFF;
        l = 0xFF;

        aPrime = 0xFF;
        fPrime = 0xFF;
        bPrime = 0xFF;
        cPrime = 0xFF;
        dPrime = 0xFF;
        ePrime = 0xFF;
        hPrime = 0xFF;
        lPrime = 0xFF;

        ix = 0xFFFF;
        iy = 0xFFFF;
        sp = 0xFFFF;
        pc = 0x0000;

        i = 0;
        r = 0;
        memptr = 0;
    }

    // --- 8-bit Register accessors ---
    public int getA() { return a; }
    public void setA(int val) { this.a = val & 0xFF; }

    public int getF() { return f; }
    public void setF(int val) { this.f = val & 0xFF; }

    public int getB() { return b; }
    public void setB(int val) { this.b = val & 0xFF; }

    public int getC() { return c; }
    public void setC(int val) { this.c = val & 0xFF; }

    public int getD() { return d; }
    public void setD(int val) { this.d = val & 0xFF; }

    public int getE() { return e; }
    public void setE(int val) { this.e = val & 0xFF; }

    public int getH() { return h; }
    public void setH(int val) { this.h = val & 0xFF; }

    public int getL() { return l; }
    public void setL(int val) { this.l = val & 0xFF; }

    // Index half registers (undocumented)
    public int getIXH() { return (ix >> 8) & 0xFF; }
    public void setIXH(int val) { ix = ((val & 0xFF) << 8) | (ix & 0xFF); }

    public int getIXL() { return ix & 0xFF; }
    public void setIXL(int val) { ix = (ix & 0xFF00) | (val & 0xFF); }

    public int getIYH() { return (iy >> 8) & 0xFF; }
    public void setIYH(int val) { iy = ((val & 0xFF) << 8) | (iy & 0xFF); }

    public int getIYL() { return iy & 0xFF; }
    public void setIYL(int val) { iy = (iy & 0xFF00) | (val & 0xFF); }

    // --- 16-bit Register pairs ---
    public int getAF() { return (a << 8) | f; }
    public void setAF(int val) {
        a = (val >> 8) & 0xFF;
        f = val & 0xFF;
    }

    public int getBC() { return (b << 8) | c; }
    public void setBC(int val) {
        b = (val >> 8) & 0xFF;
        c = val & 0xFF;
    }

    public int getDE() { return (d << 8) | e; }
    public void setDE(int val) {
        d = (val >> 8) & 0xFF;
        e = val & 0xFF;
    }

    public int getHL() { return (h << 8) | l; }
    public void setHL(int val) {
        h = (val >> 8) & 0xFF;
        l = val & 0xFF;
    }

    public int getIX() { return ix; }
    public void setIX(int val) { this.ix = val & 0xFFFF; }

    public int getIY() { return iy; }
    public void setIY(int val) { this.iy = val & 0xFFFF; }

    public int getSP() { return sp; }
    public void setSP(int val) { this.sp = val & 0xFFFF; }

    public int getPC() { return pc; }
    public void setPC(int val) { this.pc = val & 0xFFFF; }

    public int getI() { return i; }
    public void setI(int val) { this.i = val & 0xFF; }

    public int getR() { return r; }
    public void setR(int val) { this.r = val & 0xFF; }

    /** Increments the lower 7 bits of R, keeping bit 7 intact */
    public void incR() {
        r = (r & 0x80) | ((r + 1) & 0x7F);
    }

    public int getMemptr() { return memptr; }
    public void setMemptr(int val) { this.memptr = val & 0xFFFF; }

    // --- Alternate registers ---
    public int getAPrime() { return aPrime; }
    public void setAPrime(int val) { this.aPrime = val & 0xFF; }

    public int getFPrime() { return fPrime; }
    public void setFPrime(int val) { this.fPrime = val & 0xFF; }

    public int getAFPrime() { return (aPrime << 8) | fPrime; }
    public void setAFPrime(int val) {
        aPrime = (val >> 8) & 0xFF;
        fPrime = val & 0xFF;
    }

    public int getBCPrime() { return (bPrime << 8) | cPrime; }
    public void setBCPrime(int val) {
        bPrime = (val >> 8) & 0xFF;
        cPrime = val & 0xFF;
    }

    public int getDEPrime() { return (dPrime << 8) | ePrime; }
    public void setDEPrime(int val) {
        dPrime = (val >> 8) & 0xFF;
        ePrime = val & 0xFF;
    }

    public int getHLPrime() { return (hPrime << 8) | lPrime; }
    public void setHLPrime(int val) {
        hPrime = (val >> 8) & 0xFF;
        lPrime = val & 0xFF;
    }

    // --- Register exchanges ---
    public void exAfAfPrime() {
        int tempA = a; int tempF = f;
        a = aPrime; f = fPrime;
        aPrime = tempA; fPrime = tempF;
    }

    public void exx() {
        int tb = b; int tc = c;
        int td = d; int te = e;
        int th = h; int tl = l;
        b = bPrime; c = cPrime;
        d = dPrime; e = ePrime;
        h = hPrime; l = lPrime;
        bPrime = tb; cPrime = tc;
        dPrime = td; ePrime = te;
        hPrime = th; lPrime = tl;
    }

    public void exDeHl() {
        int td = d; int te = e;
        d = h; e = l;
        h = td; l = te;
    }

    // --- Register access by Enum ---
    public int getReg8(Register8 reg) {
        return switch (reg) {
            case A -> a;
            case B -> b;
            case C -> c;
            case D -> d;
            case E -> e;
            case H -> h;
            case L -> l;
            case IXH -> getIXH();
            case IXL -> getIXL();
            case IYH -> getIYH();
            case IYL -> getIYL();
        };
    }

    public void setReg8(Register8 reg, int val) {
        switch (reg) {
            case A -> setA(val);
            case B -> setB(val);
            case C -> setC(val);
            case D -> setD(val);
            case E -> setE(val);
            case H -> setH(val);
            case L -> setL(val);
            case IXH -> setIXH(val);
            case IXL -> setIXL(val);
            case IYH -> setIYH(val);
            case IYL -> setIYL(val);
        }
    }

    public int getReg16(Register16 reg) {
        return switch (reg) {
            case BC -> getBC();
            case DE -> getDE();
            case HL -> getHL();
            case SP -> getSP();
            case AF -> getAF();
            case IX -> getIX();
            case IY -> getIY();
            case PC -> getPC();
        };
    }

    public void setReg16(Register16 reg, int val) {
        switch (reg) {
            case BC -> setBC(val);
            case DE -> setDE(val);
            case HL -> setHL(val);
            case SP -> setSP(val);
            case AF -> setAF(val);
            case IX -> setIX(val);
            case IY -> setIY(val);
            case PC -> setPC(val);
        }
    }
}
