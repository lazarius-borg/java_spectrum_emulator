package nl.invokedynamic.spectrum.io;

import nl.invokedynamic.spectrum.machine.SpectrumMachine;
import nl.invokedynamic.spectrum.ui.KeyboardMapper;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JoystickTest {

    private Keyboard keyboard;
    private Joystick joystick;
    private SpectrumMachine machine;

    @BeforeEach
    void setUp() {
        keyboard = new Keyboard();
        joystick = new Joystick(keyboard);
        machine = new SpectrumMachine();
    }

    @Test
    void testKempstonDefaultState() {
        joystick.setType(Joystick.JoystickType.KEMPSTON);
        assertThat(joystick.readKempston()).isEqualTo(0x00);
        assertThat(joystick.isLeft()).isFalse();
        assertThat(joystick.isRight()).isFalse();
        assertThat(joystick.isUp()).isFalse();
        assertThat(joystick.isDown()).isFalse();
        assertThat(joystick.isFire()).isFalse();
    }

    @Test
    void testKempstonIndividualDirectionsAndFire() {
        joystick.setType(Joystick.JoystickType.KEMPSTON);

        joystick.setRight(true);
        assertThat(joystick.readKempston()).isEqualTo(0x01);
        joystick.setRight(false);

        joystick.setLeft(true);
        assertThat(joystick.readKempston()).isEqualTo(0x02);
        joystick.setLeft(false);

        joystick.setDown(true);
        assertThat(joystick.readKempston()).isEqualTo(0x04);
        joystick.setDown(false);

        joystick.setUp(true);
        assertThat(joystick.readKempston()).isEqualTo(0x08);
        joystick.setUp(false);

        joystick.setFire(true);
        assertThat(joystick.readKempston()).isEqualTo(0x10);
        joystick.setFire(false);

        assertThat(joystick.readKempston()).isEqualTo(0x00);
    }

    @Test
    void testKempstonDiagonalsAndSimultaneousFire() {
        joystick.setType(Joystick.JoystickType.KEMPSTON);

        // Up + Right (0x08 | 0x01 = 0x09)
        joystick.setUp(true);
        joystick.setRight(true);
        assertThat(joystick.readKempston()).isEqualTo(0x09);

        // Add Fire (0x09 | 0x10 = 0x19)
        joystick.setFire(true);
        assertThat(joystick.readKempston()).isEqualTo(0x19);

        // Change to Down + Left (0x04 | 0x02 | 0x10 = 0x16)
        joystick.setUp(false);
        joystick.setRight(false);
        joystick.setDown(true);
        joystick.setLeft(true);
        assertThat(joystick.readKempston()).isEqualTo(0x16);

        // Reset clears all
        joystick.reset();
        assertThat(joystick.readKempston()).isEqualTo(0x00);
        assertThat(joystick.isFire()).isFalse();
    }

    @Test
    void testMachinePort1FReadsKempston() {
        machine.getJoystick().setType(Joystick.JoystickType.KEMPSTON);
        machine.getJoystick().setRight(true);
        machine.getJoystick().setFire(true);

        int value = machine.getIoBus().in(0x001F);
        assertThat(value).isEqualTo(0x11); // Bit 0 (right) | Bit 4 (fire)
    }

    @Test
    void testSinclair1MatrixMapping() {
        joystick.setType(Joystick.JoystickType.SINCLAIR_1);

        // Sinclair 1: Keys 6 (Left), 7 (Right), 8 (Down), 9 (Up), 0 (Fire)
        // row 4 (ROW_0_9_8_7_6) bits: 0=0, 1=9, 2=8, 3=7, 4=6
        joystick.setLeft(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 4)).isTrue();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 3)).isFalse();

        joystick.setRight(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 3)).isTrue();

        joystick.setFire(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0)).isTrue();

        // Release Left and Fire
        joystick.setLeft(false);
        joystick.setFire(false);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 4)).isFalse();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 3)).isTrue();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0)).isFalse();
    }

    @Test
    void testSinclair2MatrixMapping() {
        joystick.setType(Joystick.JoystickType.SINCLAIR_2);

        // Sinclair 2: Keys 1 (Left), 2 (Right), 3 (Down), 4 (Up), 5 (Fire)
        // row 3 (ROW_1_2_3_4_5) bits: 0=1, 1=2, 2=3, 3=4, 4=5
        joystick.setLeft(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 0)).isTrue();

        joystick.setUp(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 3)).isTrue();

        joystick.reset();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 0)).isFalse();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 3)).isFalse();
    }

    @Test
    void testCursorMatrixMapping() {
        joystick.setType(Joystick.JoystickType.CURSOR);

        // Cursor: Left -> 5 (Row 3, Bit 4), Down -> 6 (Row 4, Bit 4), Up -> 7 (Row 4, Bit 3),
        // Right -> 8 (Row 4, Bit 2), Fire -> 0 (Row 4, Bit 0)
        joystick.setLeft(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 4)).isTrue();

        joystick.setUp(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 3)).isTrue();

        joystick.setFire(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0)).isTrue();

        joystick.reset();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 4)).isFalse();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 3)).isFalse();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0)).isFalse();
    }

    @Test
    void testTypeSwitchCleansUpMatrix() {
        joystick.setType(Joystick.JoystickType.SINCLAIR_1);
        joystick.setLeft(true);
        joystick.setFire(true);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 4)).isTrue();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0)).isTrue();

        // Switch to Kempston: should release matrix keys
        joystick.setType(Joystick.JoystickType.KEMPSTON);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 4)).isFalse();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0)).isFalse();

        // Kempston should now reflect the active left and fire
        assertThat(joystick.readKempston()).isEqualTo(0x12);
    }

    @Test
    void testKeyboardMapperProfiles() {
        KeyboardMapper mapper = new KeyboardMapper(keyboard, joystick);

        // 1. WASD Profile
        mapper.setProfile(KeyboardMapper.HostJoystickProfile.WASD_SPACE_J);
        mapper.handleKeyPressed(KeyCode.W);
        assertThat(joystick.isUp()).isTrue();

        mapper.handleKeyPressed(KeyCode.A);
        assertThat(joystick.isLeft()).isTrue();

        mapper.handleKeyPressed(KeyCode.J);
        assertThat(joystick.isFire()).isTrue();

        mapper.handleKeyReleased(KeyCode.W);
        assertThat(joystick.isUp()).isFalse();
        assertThat(joystick.isLeft()).isTrue();
        assertThat(joystick.isFire()).isTrue();

        mapper.handleKeyReleased(KeyCode.A);
        mapper.handleKeyReleased(KeyCode.J);
        assertThat(joystick.isLeft()).isFalse();
        assertThat(joystick.isFire()).isFalse();

        // 2. ARROWS Profile
        mapper.setProfile(KeyboardMapper.HostJoystickProfile.ARROWS_SPACE_CTRL);
        mapper.handleKeyPressed(KeyCode.RIGHT);
        assertThat(joystick.isRight()).isTrue();
        mapper.handleKeyPressed(KeyCode.SPACE);
        assertThat(joystick.isFire()).isTrue();

        mapper.handleKeyReleased(KeyCode.RIGHT);
        mapper.handleKeyReleased(KeyCode.SPACE);
        assertThat(joystick.isRight()).isFalse();
        assertThat(joystick.isFire()).isFalse();

        // 3. NUMPAD Profile
        mapper.setProfile(KeyboardMapper.HostJoystickProfile.NUMPAD);
        mapper.handleKeyPressed(KeyCode.NUMPAD8);
        assertThat(joystick.isUp()).isTrue();
        mapper.handleKeyPressed(KeyCode.NUMPAD0);
        assertThat(joystick.isFire()).isTrue();

        mapper.handleKeyReleased(KeyCode.NUMPAD8);
        mapper.handleKeyReleased(KeyCode.NUMPAD0);
        assertThat(joystick.isUp()).isFalse();
        assertThat(joystick.isFire()).isFalse();

        // 4. DISABLED Profile
        mapper.setProfile(KeyboardMapper.HostJoystickProfile.DISABLED);
        mapper.handleKeyPressed(KeyCode.UP);
        assertThat(joystick.isUp()).isFalse();
        mapper.handleKeyPressed(KeyCode.W);
        assertThat(joystick.isUp()).isFalse();
    }

    @Test
    void testNativeInterfaceKeysDriveJoystick() {
        KeyboardMapper mapper = new KeyboardMapper(keyboard, joystick);

        // 1. Sinclair 2: Keys 1, 2, 3, 4, 5
        joystick.setType(Joystick.JoystickType.SINCLAIR_2);
        mapper.handleKeyPressed(KeyCode.DIGIT1);
        assertThat(joystick.isLeft()).isTrue();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 0)).isTrue();

        mapper.handleKeyPressed(KeyCode.DIGIT5);
        assertThat(joystick.isFire()).isTrue();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 4)).isTrue();

        mapper.handleKeyReleased(KeyCode.DIGIT1);
        assertThat(joystick.isLeft()).isFalse();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 0)).isFalse();

        mapper.handleKeyReleased(KeyCode.DIGIT5);
        assertThat(joystick.isFire()).isFalse();

        // 2. Sinclair 1: Keys 6, 7, 8, 9, 0
        joystick.setType(Joystick.JoystickType.SINCLAIR_1);
        mapper.handleKeyPressed(KeyCode.DIGIT6);
        assertThat(joystick.isLeft()).isTrue();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 4)).isTrue();

        mapper.handleKeyPressed(KeyCode.DIGIT0);
        assertThat(joystick.isFire()).isTrue();
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_0_9_8_7_6, 0)).isTrue();

        mapper.handleKeyReleased(KeyCode.DIGIT6);
        mapper.handleKeyReleased(KeyCode.DIGIT0);
        assertThat(joystick.isLeft()).isFalse();
        assertThat(joystick.isFire()).isFalse();

        // 3. Kempston: Keys 1-5 do NOT drive joystick, they act as normal typing keys
        joystick.setType(Joystick.JoystickType.KEMPSTON);
        mapper.handleKeyPressed(KeyCode.DIGIT1);
        assertThat(joystick.isLeft()).isFalse(); // Joystick remains untouched
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 0)).isTrue(); // Keyboard matrix key is set
        mapper.handleKeyReleased(KeyCode.DIGIT1);
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_1_2_3_4_5, 0)).isFalse();
    }

    @Test
    void testIsJoystickKey() {
        KeyboardMapper mapper = new KeyboardMapper(keyboard, joystick);
        mapper.setProfile(KeyboardMapper.HostJoystickProfile.ARROWS_SPACE_CTRL);

        assertThat(mapper.isJoystickKey(KeyCode.LEFT)).isTrue();
        assertThat(mapper.isJoystickKey(KeyCode.SPACE)).isTrue();
        assertThat(mapper.isJoystickKey(KeyCode.CONTROL)).isTrue();
        assertThat(mapper.isJoystickKey(KeyCode.A)).isFalse();

        joystick.setType(Joystick.JoystickType.SINCLAIR_2);
        assertThat(mapper.isJoystickKey(KeyCode.DIGIT1)).isTrue();
        assertThat(mapper.isJoystickKey(KeyCode.DIGIT5)).isTrue();
        assertThat(mapper.isJoystickKey(KeyCode.DIGIT9)).isFalse();
    }

    @Test
    void testJoystickLockingAndUnlocking() {
        final boolean[] listenerFired = {false};
        joystick.setOnLockChanged(() -> listenerFired[0] = true);

        joystick.setType(Joystick.JoystickType.SINCLAIR_1);
        assertThat(joystick.isLockedByProgram()).isFalse();
        assertThat(joystick.getLockReason()).isEmpty();

        // Lock to Kempston
        joystick.lockTo(Joystick.JoystickType.KEMPSTON, "Port 0x1F");
        assertThat(joystick.isLockedByProgram()).isTrue();
        assertThat(joystick.getType()).isEqualTo(Joystick.JoystickType.KEMPSTON);
        assertThat(joystick.getLockReason()).isEqualTo("Port 0x1F");
        assertThat(listenerFired[0]).isTrue();

        // Attempting to change type while locked should be ignored
        joystick.setType(Joystick.JoystickType.SINCLAIR_2);
        assertThat(joystick.getType()).isEqualTo(Joystick.JoystickType.KEMPSTON);

        // Unlock
        listenerFired[0] = false;
        joystick.unlock();
        assertThat(joystick.isLockedByProgram()).isFalse();
        assertThat(joystick.getLockReason()).isEmpty();
        assertThat(listenerFired[0]).isTrue();

        // Now changing type should succeed
        joystick.setType(Joystick.JoystickType.SINCLAIR_2);
        assertThat(joystick.getType()).isEqualTo(Joystick.JoystickType.SINCLAIR_2);
    }

    @Test
    void testIoBusKempstonAutoLockingAndReset() {
        // Initially set to Sinclair 1
        machine.getJoystick().setType(Joystick.JoystickType.SINCLAIR_1);
        assertThat(machine.getJoystick().isLockedByProgram()).isFalse();

        // When PC is in ROM (< 0x4000), reading port 0x1F does not trigger lock
        machine.getCpu().getState().getRegisters().setPC(0x1000);
        for (int i = 0; i < 5; i++) {
            machine.getIoBus().in(0x1F);
        }
        assertThat(machine.getJoystick().isLockedByProgram()).isFalse();
        assertThat(machine.getJoystick().getType()).isEqualTo(Joystick.JoystickType.SINCLAIR_1);

        // When PC is in RAM (>= 0x4000), reading port 0x1F increments hit counter
        machine.getCpu().getState().getRegisters().setPC(0x8000);
        machine.getIoBus().in(0x1F); // hit 1
        machine.getIoBus().in(0x1F); // hit 2
        assertThat(machine.getJoystick().isLockedByProgram()).isFalse();

        machine.getIoBus().in(0x1F); // hit 3: triggers lock
        assertThat(machine.getJoystick().isLockedByProgram()).isTrue();
        assertThat(machine.getJoystick().getType()).isEqualTo(Joystick.JoystickType.KEMPSTON);
        assertThat(machine.getJoystick().getLockReason()).contains("0x1F");

        // Machine reset should unlock joystick and reset port detection
        machine.reset();
        assertThat(machine.getJoystick().isLockedByProgram()).isFalse();
        assertThat(machine.getJoystick().getLockReason()).isEmpty();
    }

    @Test
    void testTouchpadCoOpFireKeyMapping() {
        KeyboardMapper mapper = new KeyboardMapper(keyboard, joystick);
        mapper.setProfile(KeyboardMapper.HostJoystickProfile.DISABLED);
        joystick.setType(Joystick.JoystickType.KEMPSTON);

        // Test with CONTROL
        mapper.setTouchpadFireKey(KeyboardMapper.TouchpadFireKey.CONTROL);
        assertThat(joystick.isFire()).isFalse();
        mapper.handleKeyPressed(KeyCode.CONTROL);
        assertThat(joystick.isFire()).isTrue();
        // Since joystick is KEMPSTON, CONTROL should NOT leak Symbol Shift into keyboard matrix
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1)).isFalse();
        mapper.handleKeyReleased(KeyCode.CONTROL);
        assertThat(joystick.isFire()).isFalse();

        // Test with SPACE
        mapper.setTouchpadFireKey(KeyboardMapper.TouchpadFireKey.SPACE);
        mapper.handleKeyPressed(KeyCode.SPACE);
        assertThat(joystick.isFire()).isTrue();
        // Since joystick is KEMPSTON, SPACE should NOT leak Space key into keyboard matrix
        assertThat(keyboard.isKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 0)).isFalse();
        mapper.handleKeyReleased(KeyCode.SPACE);
        assertThat(joystick.isFire()).isFalse();

        // Test with ALT
        mapper.setTouchpadFireKey(KeyboardMapper.TouchpadFireKey.ALT);
        mapper.handleKeyPressed(KeyCode.ALT);
        assertThat(joystick.isFire()).isTrue();
        mapper.handleKeyReleased(KeyCode.ALT);
        assertThat(joystick.isFire()).isFalse();

        // Test with Z
        mapper.setTouchpadFireKey(KeyboardMapper.TouchpadFireKey.Z);
        mapper.handleKeyPressed(KeyCode.Z);
        assertThat(joystick.isFire()).isTrue();
        mapper.handleKeyReleased(KeyCode.Z);
        assertThat(joystick.isFire()).isFalse();

        // Test DISABLED
        mapper.setTouchpadFireKey(KeyboardMapper.TouchpadFireKey.DISABLED);
        mapper.handleKeyPressed(KeyCode.CONTROL);
        mapper.handleKeyPressed(KeyCode.SPACE);
        assertThat(joystick.isFire()).isFalse();
    }
}

