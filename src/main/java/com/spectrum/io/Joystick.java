package com.spectrum.io;

/**
 * Emulates ZX Spectrum Joysticks:
 * - Kempston (Port 0x1F)
 * - Sinclair Interface II (Ports 1 & 2 mapped to number keys)
 * - Cursor / Protek (mapped to cursor keys)
 */
public final class Joystick {
    public enum JoystickType {
        KEMPSTON,
        SINCLAIR_1,
        SINCLAIR_2,
        CURSOR
    }

    private JoystickType type = JoystickType.KEMPSTON;
    private int kempstonState = 0; // Bit 0: Right, 1: Left, 2: Down, 3: Up, 4: Fire
    private final Keyboard keyboard;

    public Joystick(Keyboard keyboard) {
        this.keyboard = keyboard;
    }

    public void setType(JoystickType type) {
        this.type = type;
        reset();
    }

    public JoystickType getType() {
        return type;
    }

    public void reset() {
        kempstonState = 0;
    }

    public void setButton(boolean left, boolean right, boolean up, boolean down, boolean fire) {
        switch (type) {
            case KEMPSTON -> {
                int state = 0;
                if (right) state |= 0x01;
                if (left)  state |= 0x02;
                if (down)  state |= 0x04;
                if (up)    state |= 0x08;
                if (fire)  state |= 0x10;
                kempstonState = state;
            }
            case SINCLAIR_1 -> {
                // Keys: 6 (Left), 7 (Right), 8 (Down), 9 (Up), 0 (Fire) -> row 4 (0, 9, 8, 7, 6)
                // row 4 bits: 0=0, 1=9, 2=8, 3=7, 4=6
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, left);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 3, right);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 2, down);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 1, up);
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 0, fire);
            }
            case SINCLAIR_2 -> {
                // Keys: 1 (Left), 2 (Right), 3 (Down), 4 (Up), 5 (Fire) -> row 3 (1, 2, 3, 4, 5)
                // row 3 bits: 0=1, 1=2, 2=3, 3=4, 4=5
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 0, left);
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 1, right);
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 2, down);
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 3, up);
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 4, fire);
            }
            case CURSOR -> {
                // Keys: 5 (Left), 8 (Right), 6 (Down), 7 (Up), 0 (Fire)
                keyboard.setKeyPressed(Keyboard.ROW_1_2_3_4_5, 4, left);  // 5
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 2, right); // 8
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, down);  // 6
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 3, up);    // 7
                keyboard.setKeyPressed(Keyboard.ROW_0_9_8_7_6, 0, fire);  // 0
            }
        }
    }

    public int readKempston() {
        return kempstonState;
    }
}
