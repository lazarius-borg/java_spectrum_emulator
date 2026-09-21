package com.spectrum.storage;

import com.spectrum.io.Keyboard;
import com.spectrum.machine.SpectrumMachine;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class TapeMenuTraceTest {

    @Test
    void testTraceRom1() throws Exception {
        SpectrumMachine machine = new SpectrumMachine();
        File tapFile = new File("taps/ACEACE.TAP");
        if (!tapFile.exists()) return;

        List<TapFileFormat.TapBlock> blocks = TapFileFormat.load(tapFile.toPath());
        machine.getTapePlayer().loadTape(blocks);

        for (int i = 0; i < 50; i++) {
            machine.stepFrame();
        }

        machine.getTapePlayer().play();

        machine.getKeyboard().setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, true);
        machine.stepFrame();
        machine.stepFrame();
        machine.getKeyboard().setKeyPressed(Keyboard.ROW_ENTER_L_K_J_H, 0, false);

        // Now trace every instruction during the switch to ROM 1
        boolean inRom1 = false;
        for (int step = 0; step < 50000; step++) {
            int pc = machine.getCpu().getState().getRegisters().getPC();
            int rom = machine.getMemory().getActiveRomBank();

            if (rom == 1 && !inRom1) {
                inRom1 = true;
                System.out.println("--- Switched to ROM 1! ---");
            }

            if (inRom1) {
                if (step % 100 == 0 || pc < 0x0600 || pc > 0x5B00 && pc < 0x5B50) {
                    System.out.printf("Step %d: PC=0x%04X, ROM=%d, RAM=%d, SP=0x%04X, AF=0x%04X, BC=0x%04X, DE=0x%04X, HL=0x%04X%n",
                        step, pc, rom, machine.getMemory().getActiveRamBank(),
                        machine.getCpu().getState().getRegisters().getSP(),
                        machine.getCpu().getState().getRegisters().getAF(),
                        machine.getCpu().getState().getRegisters().getBC(),
                        machine.getCpu().getState().getRegisters().getDE(),
                        machine.getCpu().getState().getRegisters().getHL());
                }
                if (rom == 0 && inRom1) {
                    System.out.println("--- Returned to ROM 0 at step " + step + ", PC=0x" + Integer.toHexString(pc));
                    break;
                }
            }

            machine.getCpu().step(machine.getMemory(), machine.getIoBus());
        }
    }
}
