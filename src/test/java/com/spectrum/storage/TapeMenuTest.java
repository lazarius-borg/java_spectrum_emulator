package com.spectrum.storage;

import com.spectrum.io.Keyboard;
import com.spectrum.machine.SpectrumMachine;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class TapeMenuTest {

    @Test
    void testTapeLoaderFullTrace() throws Exception {
        SpectrumMachine machine = new SpectrumMachine();
        File tapFile = new File("taps/ACEACE.TAP");
        if (!tapFile.exists()) return;

        List<TapFileFormat.TapBlock> blocks = TapFileFormat.load(tapFile.toPath());
        machine.getTapePlayer().loadTape(blocks);

        // Run until menu wait loop
        for (int i = 0; i < 50; i++) {
            machine.stepFrame();
        }

        System.out.println("Starting tape and key press...");
        machine.getTapePlayer().play();

        // Press ENTER
        machine.getKeyboard().setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, true);
        machine.stepFrame();
        machine.stepFrame();
        // Release ENTER
        machine.getKeyboard().setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, false);

        boolean hit0556 = false;
        for (int f = 0; f < 200; f++) {
            // Track inside the frame if PC hits 0x0556
            machine.stepFrame();
            int pc = machine.getCpu().getState().getRegisters().getPC();
            int rom = machine.getMemory().getActiveRomBank();
            int border = machine.getUla().getBorderColor();
            int tapeBlk = machine.getTapePlayer().getCurrentBlockIndex();

            if (f % 10 == 0 || rom == 1 || border != 7) {
                System.out.printf("Frame %d: PC=0x%04X, ROM=%d, Border=%d, TapeState=%s, TapeBlock=%d%n",
                    f, pc, rom, border, machine.getTapePlayer().getState(), tapeBlk);
            }
        }
    }

    @Test
    void testTapeTesterAdvancement() throws Exception {
        SpectrumMachine machine = new SpectrumMachine();
        File tapFile = new File("taps/ACEACE.TAP");
        if (!tapFile.exists()) return;

        List<TapFileFormat.TapBlock> blocks = TapFileFormat.load(tapFile.toPath());
        machine.getTapePlayer().loadTape(blocks);

        // Wait until menu loop 0x3683
        for (int i = 0; i < 70; i++) {
            machine.stepFrame();
        }

        // Navigate down to Tape Tester (option 4): press DOWN (Caps Shift + 6) 4 times
        for (int down = 0; down < 4; down++) {
            machine.getKeyboard().setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, true); // Caps Shift
            machine.getKeyboard().setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, true);  // key 6
            machine.stepFrame();
            machine.stepFrame();
            machine.getKeyboard().setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, false);
            machine.getKeyboard().setKeyPressed(Keyboard.ROW_0_9_8_7_6, 4, false);
            for (int w = 0; w < 10; w++) machine.stepFrame();
        }

        // Press ENTER to select Tape Tester
        machine.getKeyboard().setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, true);
        machine.stepFrame();
        machine.stepFrame();
        machine.getKeyboard().setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, false);

        for (int w = 0; w < 10; w++) machine.stepFrame();

        System.out.println("Selected Tape Tester. PC = 0x" + Integer.toHexString(machine.getCpu().getState().getRegisters().getPC()));

        // Start tape playing
        machine.getTapePlayer().play();

        // Run 50 frames and check if PC is in 0x3BE9..0x3C55 (Tape Tester loop) and border/screen changes
        int testerLoopCount = 0;
        for (int f = 0; f < 50; f++) {
            machine.stepFrame();
            int pc = machine.getCpu().getState().getRegisters().getPC();
            if (pc >= 0x3BE9 && pc <= 0x3C60) {
                testerLoopCount++;
            }
            if (f % 10 == 0) {
                System.out.printf("Tape Tester frame %d: PC=0x%04X, TapeState=%s, TapeBlock=%d%n",
                    f, pc, machine.getTapePlayer().getState(), machine.getTapePlayer().getCurrentBlockIndex());
            }
        }
        System.out.println("Frames inside Tape Tester loop: " + testerLoopCount);
    }
}
