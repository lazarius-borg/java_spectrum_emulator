package com.spectrum.storage;

import com.spectrum.io.Keyboard;
import com.spectrum.machine.SpectrumMachine;
import org.junit.jupiter.api.Test;

public class KeyDiagnosticTest {

    @Test
    void testKeyboardScanning() {
        SpectrumMachine machine = new SpectrumMachine();

        // Run until it reaches the wait loop at 0x3683
        for (int i = 0; i < 50; i++) {
            machine.stepFrame();
        }

        System.out.println("PC before key: 0x" + Integer.toHexString(machine.getCpu().getState().getRegisters().getPC()));

        // Press ENTER
        machine.getKeyboard().setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, true);

        // Run frames and check flags at 0x5C3B and LAST-K at 0x5C08
        for (int f = 0; f < 30; f++) {
            machine.stepFrame();
            int flags = machine.getMemory().readByte(0x5C3B);
            int lastK = machine.getMemory().readByte(0x5C08);
            int pc = machine.getCpu().getState().getRegisters().getPC();
            System.out.printf("Frame %d: PC=0x%04X, FLAGS(5C3B)=0x%02X, LAST-K(5C08)=0x%02X%n", f, pc, flags, lastK);
            if ((flags & 0x20) != 0 || pc != 0x3685 && pc != 0x3683) {
                System.out.println("Key detected! PC branched to 0x" + Integer.toHexString(pc));
                break;
            }
        }
    }
}
