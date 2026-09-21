package com.spectrum.ui;

import com.spectrum.io.Joystick;
import com.spectrum.io.Keyboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Translates host JavaFX KeyEvents into ZX Spectrum 8x5 keyboard matrix presses
 * and joystick inputs.
 */
public final class KeyboardMapper {
    private final Keyboard keyboard;
    private final Joystick joystick;

    public KeyboardMapper(Keyboard keyboard, Joystick joystick) {
        this.keyboard = keyboard;
        this.joystick = joystick;
    }

    public void handleKeyPressed(KeyEvent event) {
        handleKey(event.getCode(), true);
    }

    public void handleKeyReleased(KeyEvent event) {
        handleKey(event.getCode(), false);
    }

    private void handleKey(KeyCode code, boolean pressed) {
        // First check Joystick inputs
        switch (code) {
            case LEFT -> {
                joystick.setButton(pressed, false, false, false, false);
                // Also map to Caps Shift + 5
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 4, pressed);
                return;
            }
            case RIGHT -> {
                joystick.setButton(false, pressed, false, false, false);
                // Caps Shift + 8
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 2, pressed);
                return;
            }
            case UP -> {
                joystick.setButton(false, false, pressed, false, false);
                // Caps Shift + 7
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 3, pressed);
                return;
            }
            case DOWN -> {
                joystick.setButton(false, false, false, pressed, false);
                // Caps Shift + 6
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, pressed);
                return;
            }
            case CONTROL -> {
                joystick.setButton(false, false, false, false, pressed); // Fire
                // Also Symbol Shift
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                return;
            }
            default -> {}
        }

        // Standard Spectrum keyboard mapping
        switch (code) {
            // Row 0: Shift, Z, X, C, V
            case SHIFT -> keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
            case Z     -> keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 1, pressed);
            case X     -> keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 2, pressed);
            case C     -> keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 3, pressed);
            case V     -> keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 4, pressed);

            // Row 1: A, S, D, F, G
            case A -> keyboard.setKeyPressed(Keyboard.ROW_A_S_D_F_G, 0, pressed);
            case S -> keyboard.setKeyPressed(Keyboard.ROW_A_S_D_F_G, 1, pressed);
            case D -> keyboard.setKeyPressed(Keyboard.ROW_A_S_D_F_G, 2, pressed);
            case F -> keyboard.setKeyPressed(Keyboard.ROW_A_S_D_F_G, 3, pressed);
            case G -> keyboard.setKeyPressed(Keyboard.ROW_A_S_D_F_G, 4, pressed);

            // Row 2: Q, W, E, R, T
            case Q -> keyboard.setKeyPressed(Keyboard.ROW_Q_W_E_R_T, 0, pressed);
            case W -> keyboard.setKeyPressed(Keyboard.ROW_Q_W_E_R_T, 1, pressed);
            case E -> keyboard.setKeyPressed(Keyboard.ROW_Q_W_E_R_T, 2, pressed);
            case R -> keyboard.setKeyPressed(Keyboard.ROW_Q_W_E_R_T, 3, pressed);
            case T -> keyboard.setKeyPressed(Keyboard.ROW_Q_W_E_R_T, 4, pressed);

            // Row 3: 1, 2, 3, 4, 5
            case DIGIT1 -> keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 0, pressed);
            case DIGIT2 -> keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 1, pressed);
            case DIGIT3 -> keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 2, pressed);
            case DIGIT4 -> keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 3, pressed);
            case DIGIT5 -> keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 4, pressed);

            // Row 4: 0, 9, 8, 7, 6
            case DIGIT0 -> keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 0, pressed);
            case DIGIT9 -> keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 1, pressed);
            case DIGIT8 -> keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 2, pressed);
            case DIGIT7 -> keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 3, pressed);
            case DIGIT6 -> keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, pressed);

            // Row 5: P, O, I, U, Y
            case P -> keyboard.setKeyPressed(Keyboard.ROW_P_O_I_U_Y, 0, pressed);
            case O -> keyboard.setKeyPressed(Keyboard.ROW_P_O_I_U_Y, 1, pressed);
            case I -> keyboard.setKeyPressed(Keyboard.ROW_P_O_I_U_Y, 2, pressed);
            case U -> keyboard.setKeyPressed(Keyboard.ROW_P_O_I_U_Y, 3, pressed);
            case Y -> keyboard.setKeyPressed(Keyboard.ROW_P_O_I_U_Y, 4, pressed);

            // Row 6: Enter, L, K, J, H
            case ENTER -> keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, pressed);
            case L     -> keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 1, pressed);
            case K     -> keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 2, pressed);
            case J     -> keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 3, pressed);
            case H     -> keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 4, pressed);

            // Row 7: Space, Symbol Shift, M, N, B
            case SPACE -> keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 0, pressed);
            case ALT   -> keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
            case M     -> keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 2, pressed);
            case N     -> keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 3, pressed);
            case B     -> keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 4, pressed);

            // Backspace / Delete -> Caps Shift + 0 (Delete on Spectrum)
            case BACK_SPACE, DELETE -> {
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 0, pressed);
            }

            // Escape -> Caps Shift + Space (Break on Spectrum)
            case ESCAPE -> {
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 0, pressed);
            }

            // Caps Lock -> Caps Shift + 2
            case CAPS -> {
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 1, pressed);
            }

            // Punctuation & Symbols mapped via Symbol Shift:
            // " -> Symbol Shift + P
            case QUOTE -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_P_O_I_U_Y, 0, pressed);
            }
            // ; -> Symbol Shift + O
            case SEMICOLON -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_P_O_I_U_Y, 1, pressed);
            }
            // , -> Symbol Shift + N
            case COMMA -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 3, pressed);
            }
            // . -> Symbol Shift + M
            case PERIOD, DECIMAL -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 2, pressed);
            }
            // / -> Symbol Shift + V
            case SLASH, DIVIDE -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 4, pressed);
            }
            // - -> Symbol Shift + J
            case MINUS, SUBTRACT -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 3, pressed);
            }
            // = -> Symbol Shift + L
            case EQUALS -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 1, pressed);
            }
            // + -> Symbol Shift + K
            case PLUS, ADD -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 2, pressed);
            }
            // * -> Symbol Shift + B
            case MULTIPLY, ASTERISK -> {
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 4, pressed);
            }
            // ` -> Symbol Shift
            case BACK_QUOTE -> keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);

            default -> {}
        }
    }
}
