package nl.invokedynamic.spectrum.ui;

import nl.invokedynamic.spectrum.io.Joystick;
import nl.invokedynamic.spectrum.io.Keyboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Translates host JavaFX KeyEvents into ZX Spectrum 8x5 keyboard matrix presses
 * and joystick inputs.
 */
public class KeyboardMapper {
    private final Keyboard keyboard;
    private final Joystick joystick;

    public enum HostJoystickProfile {
        ARROWS_SPACE_CTRL("Arrow Keys + Space / Ctrl"),
        WASD_SPACE_J("WASD + Space / J (Two-Handed)"),
        NUMPAD("Numpad 8462 + 0 / Enter"),
        INTERFACE_NATIVE("Interface Native Keys (1-5 / 6-0 / 5-8,0)"),
        DISABLED("Disabled (Pure Keyboard)");

        private final String displayName;

        HostJoystickProfile(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private HostJoystickProfile profile = HostJoystickProfile.ARROWS_SPACE_CTRL;
    private boolean mapArrowsToCursorKeys = true;

    public KeyboardMapper(Keyboard keyboard, Joystick joystick) {
        this.keyboard = keyboard;
        this.joystick = joystick;
    }

    public HostJoystickProfile getProfile() {
        return profile;
    }

    public void setProfile(HostJoystickProfile profile) {
        this.profile = profile != null ? profile : HostJoystickProfile.ARROWS_SPACE_CTRL;
        joystick.reset();
    }

    public boolean isMapArrowsToCursorKeys() {
        return mapArrowsToCursorKeys;
    }

    public void setMapArrowsToCursorKeys(boolean map) {
        this.mapArrowsToCursorKeys = map;
    }

    public boolean isJoystickKey(KeyCode code) {
        if (profile == HostJoystickProfile.DISABLED) return false;
        if (profile == HostJoystickProfile.ARROWS_SPACE_CTRL) {
            if (code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT ||
                code == KeyCode.RIGHT || code == KeyCode.SPACE || code == KeyCode.CONTROL) return true;
        }
        if (profile == HostJoystickProfile.WASD_SPACE_J) {
            if (code == KeyCode.W || code == KeyCode.A || code == KeyCode.S ||
                code == KeyCode.D || code == KeyCode.SPACE || code == KeyCode.J || code == KeyCode.K) return true;
        }
        if (profile == HostJoystickProfile.NUMPAD) {
            if (code.name().startsWith("NUMPAD")) return true;
        }
        if (joystick.getType() == Joystick.JoystickType.SINCLAIR_1) {
            return code == KeyCode.DIGIT6 || code == KeyCode.DIGIT7 || code == KeyCode.DIGIT8 ||
                   code == KeyCode.DIGIT9 || code == KeyCode.DIGIT0;
        }
        if (joystick.getType() == Joystick.JoystickType.SINCLAIR_2) {
            return code == KeyCode.DIGIT1 || code == KeyCode.DIGIT2 || code == KeyCode.DIGIT3 ||
                   code == KeyCode.DIGIT4 || code == KeyCode.DIGIT5;
        }
        if (joystick.getType() == Joystick.JoystickType.CURSOR) {
            return code == KeyCode.DIGIT5 || code == KeyCode.DIGIT6 || code == KeyCode.DIGIT7 ||
                   code == KeyCode.DIGIT8 || code == KeyCode.DIGIT0;
        }
        return false;
    }

    public void handleKeyPressed(KeyEvent event) {
        handleKey(event.getCode(), true);
    }

    public void handleKeyReleased(KeyEvent event) {
        handleKey(event.getCode(), false);
    }

    public void handleKeyPressed(KeyCode code) {
        handleKey(code, true);
    }

    public void handleKeyReleased(KeyCode code) {
        handleKey(code, false);
    }

    private void handleKey(KeyCode code, boolean pressed) {
        // 1. Check Profile-specific Joystick mappings
        if (profile == HostJoystickProfile.WASD_SPACE_J) {
            switch (code) {
                case A -> { joystick.setLeft(pressed); return; }
                case D -> { joystick.setRight(pressed); return; }
                case W -> { joystick.setUp(pressed); return; }
                case S -> { joystick.setDown(pressed); return; }
                case J, K -> { joystick.setFire(pressed); return; }
                case SPACE -> {
                    joystick.setFire(pressed);
                    keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 0, pressed);
                    return;
                }
                default -> {}
            }
        } else if (profile == HostJoystickProfile.NUMPAD) {
            switch (code) {
                case NUMPAD4 -> { joystick.setLeft(pressed); return; }
                case NUMPAD6 -> { joystick.setRight(pressed); return; }
                case NUMPAD8 -> { joystick.setUp(pressed); return; }
                case NUMPAD2 -> { joystick.setDown(pressed); return; }
                case NUMPAD7 -> { joystick.setUp(pressed); joystick.setLeft(pressed); return; }
                case NUMPAD9 -> { joystick.setUp(pressed); joystick.setRight(pressed); return; }
                case NUMPAD1 -> { joystick.setDown(pressed); joystick.setLeft(pressed); return; }
                case NUMPAD3 -> { joystick.setDown(pressed); joystick.setRight(pressed); return; }
                case NUMPAD0, DECIMAL -> { joystick.setFire(pressed); return; }
                default -> {}
            }
        }

        // 2. Global Arrow Keys & Control (active unless DISABLED or INTERFACE_NATIVE)
        boolean steerWithArrows = (profile == HostJoystickProfile.ARROWS_SPACE_CTRL);
        if (profile != HostJoystickProfile.DISABLED) {
            switch (code) {
                case LEFT -> {
                    if (steerWithArrows) joystick.setLeft(pressed);
                    if (mapArrowsToCursorKeys) {
                        keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                        keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 4, pressed);
                    }
                    return;
                }
                case RIGHT -> {
                    if (steerWithArrows) joystick.setRight(pressed);
                    if (mapArrowsToCursorKeys) {
                        keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                        keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 2, pressed);
                    }
                    return;
                }
                case UP -> {
                    if (steerWithArrows) joystick.setUp(pressed);
                    if (mapArrowsToCursorKeys) {
                        keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                        keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 3, pressed);
                    }
                    return;
                }
                case DOWN -> {
                    if (steerWithArrows) joystick.setDown(pressed);
                    if (mapArrowsToCursorKeys) {
                        keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, pressed);
                        keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, pressed);
                    }
                    return;
                }
                case CONTROL -> {
                    if (steerWithArrows) joystick.setFire(pressed);
                    keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, pressed);
                    return;
                }
                case SPACE -> {
                    if (steerWithArrows) {
                        joystick.setFire(pressed);
                        keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 0, pressed);
                        return;
                    }
                }
                default -> {}
            }
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
            case DIGIT1 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_2) {
                    joystick.setLeft(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 0, pressed);
                }
            }
            case DIGIT2 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_2) {
                    joystick.setRight(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 1, pressed);
                }
            }
            case DIGIT3 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_2) {
                    joystick.setDown(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 2, pressed);
                }
            }
            case DIGIT4 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_2) {
                    joystick.setUp(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 3, pressed);
                }
            }
            case DIGIT5 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_2) {
                    joystick.setFire(pressed);
                } else if (joystick.getType() == Joystick.JoystickType.CURSOR) {
                    joystick.setLeft(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 4, pressed);
                }
            }

            // Row 4: 0, 9, 8, 7, 6
            case DIGIT0 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_1 || joystick.getType() == Joystick.JoystickType.CURSOR) {
                    joystick.setFire(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 0, pressed);
                }
            }
            case DIGIT9 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_1) {
                    joystick.setUp(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 1, pressed);
                }
            }
            case DIGIT8 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_1) {
                    joystick.setDown(pressed);
                } else if (joystick.getType() == Joystick.JoystickType.CURSOR) {
                    joystick.setRight(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 2, pressed);
                }
            }
            case DIGIT7 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_1) {
                    joystick.setRight(pressed);
                } else if (joystick.getType() == Joystick.JoystickType.CURSOR) {
                    joystick.setUp(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 3, pressed);
                }
            }
            case DIGIT6 -> {
                if (joystick.getType() == Joystick.JoystickType.SINCLAIR_1) {
                    joystick.setLeft(pressed);
                } else if (joystick.getType() == Joystick.JoystickType.CURSOR) {
                    joystick.setDown(pressed);
                } else {
                    keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, pressed);
                }
            }

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
