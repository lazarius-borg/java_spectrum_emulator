package nl.invokedynamic.spectrum.io;

import nl.invokedynamic.spectrum.memory.Spectrum128Memory;
import nl.invokedynamic.spectrum.sound.Ay38912;
import nl.invokedynamic.spectrum.sound.Beeper;
import nl.invokedynamic.spectrum.storage.TapePlayer;
import nl.invokedynamic.spectrum.ula.UlaDisplay;

/**
 * Dispatches I/O port operations for the ZX Spectrum 128K:
 * - Port 0xFE: Keyboard (bits 0-4), MIC/Tape out (bit 3), Beeper (bit 4), EAR/Tape in (bit 6), Border (bits 0-2)
 * - Port 0x7FFD: 128K Memory Paging
 * - Port 0xFFFD: AY-3-8912 Register Select
 * - Port 0xBFFD: AY-3-8912 Data Write / Read
 * - Port 0x1F: Kempston Joystick
 */
public final class SpectrumIoBus implements IoBus {
    private final Keyboard keyboard;
    private final Joystick joystick;
    private final UlaDisplay ula;
    private final Beeper beeper;
    private final Ay38912 psg;
    private final Spectrum128Memory memory;
    private TapePlayer tapePlayer;
    private nl.invokedynamic.spectrum.cpu.Z80Cpu cpu;

    private int kempstonHits = 0;
    private int sinclair1Hits = 0;
    private int sinclair2Hits = 0;

    public SpectrumIoBus(Keyboard keyboard, Joystick joystick, UlaDisplay ula,
                         Beeper beeper, Ay38912 psg, Spectrum128Memory memory) {
        this.keyboard = keyboard;
        this.joystick = joystick;
        this.ula = ula;
        this.beeper = beeper;
        this.psg = psg;
        this.memory = memory;
    }

    public void setCpu(nl.invokedynamic.spectrum.cpu.Z80Cpu cpu) {
        this.cpu = cpu;
    }

    public void setTapePlayer(TapePlayer tapePlayer) {
        this.tapePlayer = tapePlayer;
    }

    public void resetPortDetection() {
        kempstonHits = 0;
        sinclair1Hits = 0;
        sinclair2Hits = 0;
    }

    @Override
    public int in(int port) {
        // Kempston Joystick: Port 0x1F
        if ((port & 0xFF) == 0x1F) {
            if (cpu != null && cpu.getState().getRegisters().getPC() >= 0x4000) {
                kempstonHits++;
                if (kempstonHits >= 3 && !joystick.isLockedByProgram()) {
                    joystick.lockTo(Joystick.JoystickType.KEMPSTON, "Port 0x1F");
                }
            }
            return joystick.readKempston();
        }

        // AY-3-8912 Data Read: Port 0xFFFD
        if ((port & 0xC002) == 0xC000) {
            return psg.readData();
        }

        // ULA Port 0xFE: (port & 0x01) == 0
        if ((port & 0x01) == 0) {
            int val = keyboard.read(port) & 0x1F; // Keys in bits 0-4
            val |= 0xA0; // Bits 5 and 7 usually 1

            // Bit 6: EAR tape input signal
            if (tapePlayer != null && tapePlayer.getEarSignal()) {
                val |= 0x40;
            }
            return val;
        }

        // Default floating bus
        return 0xFF;
    }

    @Override
    public void out(int port, int value) {
        value &= 0xFF;

        // Port 0x7FFD: 128K Memory paging (A15 low, A1 low)
        if ((port & 0x8002) == 0) {
            memory.writePort7ffd(value);
            return;
        }

        // AY-3-8912 Register Select: Port 0xFFFD (A15 high, A14 high, A1 low)
        if ((port & 0xC002) == 0xC000) {
            psg.selectRegister(value);
            return;
        }

        // AY-3-8912 Data Write: Port 0xBFFD (A15 high, A14 low, A1 low)
        if ((port & 0xC002) == 0x8000) {
            psg.writeData(value);
            return;
        }

        // ULA Port 0xFE: (port & 0x01) == 0
        if ((port & 0x01) == 0) {
            ula.setBorderColor(value & 0x07);
            beeper.update(value);
            if (tapePlayer != null) {
                tapePlayer.recordMic((value & 0x08) != 0);
            }
        }
    }
}
