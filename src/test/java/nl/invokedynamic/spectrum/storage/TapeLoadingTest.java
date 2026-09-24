package nl.invokedynamic.spectrum.storage;

import nl.invokedynamic.spectrum.machine.SpectrumMachine;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TapeLoadingTest {

    @Test
    void testTapeLoadingRoutine() throws Exception {
        SpectrumMachine machine = new SpectrumMachine();
        List<TapFileFormat.TapBlock> blocks = loadTestTap("ACEACE.TAP");
        if (blocks.isEmpty()) return;

        machine.getTapePlayer().loadTape(blocks);

        // Run until boot completes and menu wait loop (0x3683 in ROM 0) is entered
        int bootFrames = 0;
        while (bootFrames < 300) {
            machine.stepFrame();
            bootFrames++;
            int pc = machine.getCpu().getState().getRegisters().getPC();
            int rom = machine.getMemory().getActiveRomBank();
            if (rom == 0 && (pc == 0x3683 || pc == 0x3685)) {
                System.out.printf("Reached menu loop at frame %d! PC=0x%04X, ROM=%d%n", bootFrames, pc, rom);
                break;
            }
        }

        // Press ENTER on "Tape Loader"
        machine.getTapePlayer().play();
        machine.getKeyboard().setKeyPressed(nl.invokedynamic.spectrum.io.Keyboard.ROW_ENTER_L_K_J_H, 0, true);
        for (int f = 0; f < 10; f++) {
            machine.stepFrame();
        }
        machine.getKeyboard().setKeyPressed(nl.invokedynamic.spectrum.io.Keyboard.ROW_ENTER_L_K_J_H, 0, false);

        int hit0556Count = 0;
        for (int f = 0; f < 100; f++) {
            // Step instruction by instruction in this frame to see if 0x0556 is reached
            int frameCycles = 0;
            int targetCycles = machine.getModel().getTstatesPerFrame();
            while (frameCycles < targetCycles) {
                int pc = machine.getCpu().getState().getRegisters().getPC();
                int rom = machine.getMemory().getActiveRomBank();
                if (rom == 1 && pc == 0x0556) {
                    hit0556Count++;
                    System.out.printf("Hit 0x0556 at frame %d, block=%d, A=%02X, IX=%04X, DE=%04X, AF'=%04X%n",
                        f, machine.getTapePlayer().getCurrentBlockIndex(),
                        machine.getCpu().getState().getRegisters().getA(),
                        machine.getCpu().getState().getRegisters().getIX(),
                        machine.getCpu().getState().getRegisters().getDE(),
                        machine.getCpu().getState().getRegisters().getAFPrime());
                }
                if (machine.getTapePlayer().checkRomTrap(machine.getCpu().getState(), machine.getMemory())) {
                    System.out.printf("TRAP SUCCEEDED at frame %d! New block index: %d%n",
                        f, machine.getTapePlayer().getCurrentBlockIndex());
                    continue;
                }
                int cycles = machine.getCpu().step(machine.getMemory(), machine.getIoBus());
                frameCycles += cycles;
                machine.getPsg().step(cycles);
                machine.getTapePlayer().step(cycles);
            }
            if (f % 10 == 0 || machine.getMemory().getActiveRomBank() == 1) {
                System.out.printf("Frame %d: PC=0x%04X, ROM=%d, Border=%d, TapeBlk=%d%n",
                    f, machine.getCpu().getState().getRegisters().getPC(),
                    machine.getMemory().getActiveRomBank(),
                    machine.getUla().getBorderColor(),
                    machine.getTapePlayer().getCurrentBlockIndex());
            }
        }
        System.out.println("Total 0x0556 hits: " + hit0556Count);
        System.out.println("Final Tape block index: " + machine.getTapePlayer().getCurrentBlockIndex());
        assertThat(machine.getTapePlayer().getCurrentBlockIndex()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void testAHarvestLoading() throws Exception {
        SpectrumMachine machine = new SpectrumMachine();
        List<TapFileFormat.TapBlock> blocks = loadTestTap("AHARVEST.TAP");
        if (blocks.isEmpty()) return;

        machine.getTapePlayer().loadTape(blocks);

        // Run until boot completes and menu wait loop (0x3683 in ROM 0) is entered
        int bootFrames = 0;
        while (bootFrames < 300) {
            machine.stepFrame();
            bootFrames++;
            int pc = machine.getCpu().getState().getRegisters().getPC();
            int rom = machine.getMemory().getActiveRomBank();
            if (rom == 0 && (pc == 0x3683 || pc == 0x3685)) {
                break;
            }
        }

        // Press ENTER on "Tape Loader"
        machine.getKeyboard().setKeyPressed(nl.invokedynamic.spectrum.io.Keyboard.ROW_ENTER_L_K_J_H, 0, true);
        for (int f = 0; f < 10; f++) {
            machine.stepFrame();
        }
        machine.getKeyboard().setKeyPressed(nl.invokedynamic.spectrum.io.Keyboard.ROW_ENTER_L_K_J_H, 0, false);

        for (int f = 0; f < 100; f++) {
            machine.stepFrame();
        }

        System.out.println("AHARVEST Tape block index after 100 frames: " + machine.getTapePlayer().getCurrentBlockIndex());
        assertThat(machine.getTapePlayer().getCurrentBlockIndex()).isGreaterThanOrEqualTo(4);
    }

    static List<TapFileFormat.TapBlock> loadTestTap(String name) throws Exception {
        try (var is = TapeLoadingTest.class.getResourceAsStream("/taps/" + name)) {
            if (is != null) {
                return TapFileFormat.load(is);
            }
        }
        File tapFile = new File("taps/" + name);
        if (tapFile.exists()) {
            return TapFileFormat.load(tapFile.toPath());
        }
        return List.of();
    }
}
