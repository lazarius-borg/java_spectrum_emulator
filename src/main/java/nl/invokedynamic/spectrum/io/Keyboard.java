package nl.invokedynamic.spectrum.io;

/**
 * Emulates the 40-key ZX Spectrum keyboard matrix (8 half-rows of 5 keys).
 *
 * Half-row mask mapped to A8-A15 (active low):
 * 0xFE: Caps Shift, Z, X, C, V
 * 0xFD: A, S, D, F, G
 * 0xFB: Q, W, E, R, T
 * 0xF7: 1, 2, 3, 4, 5
 * 0xEF: 0, 9, 8, 7, 6
 * 0xDF: P, O, I, U, Y
 * 0xBF: Enter, L, K, J, H
 * 0x7F: Space, Symbol Shift, M, N, B
 */
public final class Keyboard {
    // 8 half-rows, each 5 bits (bits 0-4: 0 = pressed, 1 = released)
    private final int[] halfRows = new int[8];

    public Keyboard() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < halfRows.length; i++) {
            halfRows[i] = 0x1F; // All keys released
        }
    }

    public void setKeyPressed(int row, int bit, boolean pressed) {
        if (row < 0 || row >= 8 || bit < 0 || bit >= 5) return;
        if (pressed) {
            halfRows[row] &= ~(1 << bit);
        } else {
            halfRows[row] |= (1 << bit);
        }
    }

    public boolean isKeyPressed(int row, int bit) {
        if (row < 0 || row >= 8 || bit < 0 || bit >= 5) return false;
        return (halfRows[row] & (1 << bit)) == 0;
    }

    /**
     * Reads keyboard matrix for a given 16-bit port where high byte A8-A15 selects rows.
     * Returns 5-bit result (bits 0-4).
     */
    public int read(int port) {
        int highByte = (port >> 8) & 0xFF;
        int result = 0x1F;

        for (int row = 0; row < 8; row++) {
            if ((highByte & (1 << row)) == 0) {
                result &= halfRows[row];
            }
        }
        return result;
    }

    // Key definition constants (row, bit)
    public static final int ROW_CS_Z_X_C_V    = 0;
    public static final int ROW_A_S_D_F_G     = 1;
    public static final int ROW_Q_W_E_R_T     = 2;
    public static final int ROW_1_2_3_4_5     = 3;
    public static final int ROW_0_9_8_7_6     = 4;
    public static final int ROW_P_O_I_U_Y     = 5;
    public static final int ROW_ENTER_L_K_J_H = 6;
    public static final int ROW_SPACE_SS_M_N_B = 7;
}
