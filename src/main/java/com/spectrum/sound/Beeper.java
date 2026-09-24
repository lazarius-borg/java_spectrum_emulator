package com.spectrum.sound;

/**
 * 1-bit ULA sound beeper emulation.
 * Captures speaker state transitions from Port 0xFE writes.
 */
public final class Beeper {
    private int speakerState = 0; // 0 or 1
    private int micState = 0;
    private int earState = 0;

    public void update(int portFeValue) {
        speakerState = (portFeValue & 0x10) != 0 ? 1 : 0;
        micState = (portFeValue & 0x08) != 0 ? 1 : 0;
    }

    public void setEarState(boolean ear) {
        this.earState = ear ? 1 : 0;
    }

    public float getSample() {
        return (speakerState != 0 ? 0.65f : 0.0f)
             + (micState != 0 ? 0.10f : 0.0f)
             + (earState != 0 ? 0.55f : 0.0f);
    }

    public int getSpeakerState() { return speakerState; }
    public int getMicState() { return micState; }
    public int getEarState() { return earState; }

    public void reset() {
        speakerState = 0;
        micState = 0;
        earState = 0;
    }
}
