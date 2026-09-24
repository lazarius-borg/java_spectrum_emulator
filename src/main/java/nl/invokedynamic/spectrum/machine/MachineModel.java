package nl.invokedynamic.spectrum.machine;

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
