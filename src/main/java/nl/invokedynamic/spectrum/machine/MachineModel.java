package nl.invokedynamic.spectrum.machine;

/**
 * Defines supported ZX Spectrum hardware architectures, clock frequencies,
 * and per-frame T-state cycle counts.
 * <ul>
 *   <li><b>128K:</b> 3.5469 MHz CPU, 70,908 T-states per frame (50.088 Hz).</li>
 *   <li><b>48K:</b> 3.5000 MHz CPU, 69,888 T-states per frame (50.000 Hz).</li>
 * </ul>
 */
public enum MachineModel {
    SPECTRUM_128K("ZX Spectrum 128K", 3546900, 70908),
    SPECTRUM_48K("ZX Spectrum 48K", 3500000, 69888);

    private final String displayName;
    private final int clockFrequencyHz;
    private final int tstatesPerFrame;

    MachineModel(String displayName, int clockFrequencyHz, int tstatesPerFrame) {
        this.displayName = displayName;
        this.clockFrequencyHz = clockFrequencyHz;
        this.tstatesPerFrame = tstatesPerFrame;
    }

    public String getDisplayName() { return displayName; }
    public int getClockFrequencyHz() { return clockFrequencyHz; }
    public int getTstatesPerFrame() { return tstatesPerFrame; }
}
