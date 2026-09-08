package com.spectrum.machine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SpectrumMachineTest {

    private SpectrumMachine machine;

    @BeforeEach
    void setUp() {
        machine = new SpectrumMachine();
    }

    @Test
    void testStepFrameExecutesFrameCycles() {
        long cyclesBefore = machine.getCpu().getState().getTotalCycles();
        machine.stepFrame();
        long cyclesAfter = machine.getCpu().getState().getTotalCycles();

        assertThat(cyclesAfter - cyclesBefore).isGreaterThanOrEqualTo(machine.getModel().getTstatesPerFrame());
    }

    @Test
    void testFrameBufferRendered() {
        // Run a frame and check that the ULA framebuffer is generated
        machine.stepFrame();
        int[] fb = machine.getUla().getFrameBuffer();
        assertThat(fb).isNotNull();
        assertThat(fb.length).isEqualTo(320 * 240);
    }
}
