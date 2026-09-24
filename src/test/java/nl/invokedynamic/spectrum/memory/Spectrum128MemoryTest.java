package nl.invokedynamic.spectrum.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Spectrum128MemoryTest {

    private Spectrum128Memory memory;

    @BeforeEach
    void setUp() {
        memory = new Spectrum128Memory();
    }

    @Test
    void testRomIsReadOnly() {
        byte[] rom0 = new byte[16384];
        rom0[0x100] = 0x42;
        memory.loadRom(0, rom0);

        assertThat(memory.readByte(0x0100)).isEqualTo(0x42);

        // Attempt write
        memory.writeByte(0x0100, 0x99);
        assertThat(memory.readByte(0x0100)).isEqualTo(0x42); // Should not change
    }

    @Test
    void testRamPagingPort7ffd() {
        // Bank 0 is paged at 0xC000 initially
        memory.writeByte(0xC000, 0x11);
        assertThat(memory.getRamBank(0)[0]).isEqualTo((byte) 0x11);

        // Page in Bank 3 (Port 0x7FFD = 0x03)
        memory.writePort7ffd(0x03);
        assertThat(memory.getActiveRamBank()).isEqualTo(3);
        assertThat(memory.readByte(0xC000)).isEqualTo(0);

        memory.writeByte(0xC000, 0x33);
        assertThat(memory.getRamBank(3)[0]).isEqualTo((byte) 0x33);

        // Switch back to Bank 0
        memory.writePort7ffd(0x00);
        assertThat(memory.readByte(0xC000)).isEqualTo(0x11);
    }

    @Test
    void testRomPagingAndScreenPaging() {
        byte[] rom0 = new byte[16384];
        rom0[0] = 0x12; // 128K ROM signature
        byte[] rom1 = new byte[16384];
        rom1[0] = 0x48; // 48K ROM signature
        memory.loadRom(0, rom0);
        memory.loadRom(1, rom1);

        // Default ROM is ROM 0
        assertThat(memory.readByte(0x0000)).isEqualTo(0x12);
        assertThat(memory.getScreenBank()).isEqualTo(5);

        // Page ROM 1 (bit 4 = 1) and Screen 1 (bit 3 = 1) -> 0x18
        memory.writePort7ffd(0x18);
        assertThat(memory.readByte(0x0000)).isEqualTo(0x48);
        assertThat(memory.getScreenBank()).isEqualTo(7);
    }

    @Test
    void testPagingLock() {
        // Lock paging with bit 5 set (0x20)
        memory.writePort7ffd(0x21); // Bank 1 paged + Locked
        assertThat(memory.getActiveRamBank()).isEqualTo(1);
        assertThat(memory.isPagingLocked()).isTrue();

        // Further writes to 0x7FFD should have NO effect
        memory.writePort7ffd(0x04);
        assertThat(memory.getActiveRamBank()).isEqualTo(1); // Still Bank 1!
    }
}
