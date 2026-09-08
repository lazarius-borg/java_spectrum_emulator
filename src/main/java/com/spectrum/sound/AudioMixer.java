package com.spectrum.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Mixes Beeper and AY-3-8912 channels into 44.1 kHz, 16-bit stereo audio.
 * Automatically gracefully degrades if host audio output is unavailable.
 */
public final class AudioMixer {
    public static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 4096;

    private final Beeper beeper;
    private final Ay38912 psg;
    private SourceDataLine line;
    private boolean audioEnabled = true;
    private float masterVolume = 0.8f;

    private final byte[] audioBuffer = new byte[BUFFER_SIZE];
    private int bufferIndex = 0;

    public AudioMixer(Beeper beeper, Ay38912 psg) {
        this.beeper = beeper;
        this.psg = psg;
        initAudioLine();
    }

    private void initAudioLine() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, BUFFER_SIZE * 4);
                line.start();
            } else {
                audioEnabled = false;
            }
        } catch (Exception e) {
            audioEnabled = false;
        }
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public boolean isAudioEnabled() {
        return audioEnabled;
    }

    public void setAudioEnabled(boolean enabled) {
        this.audioEnabled = enabled;
    }

    /**
     * Synthesizes and buffers one stereo sample.
     * When buffer fills, it writes to the audio line.
     */
    public void generateSample() {
        if (!audioEnabled || line == null) {
            return;
        }

        float beep = beeper.getSample();
        float chA = psg.getChannelA();
        float chB = psg.getChannelB();
        float chC = psg.getChannelC();

        // Stereo mixing:
        // Left: 0.75 * A + 0.5 * B + 0.25 * C + beep
        // Right: 0.25 * A + 0.5 * B + 0.75 * C + beep
        float left = (chA * 0.75f + chB * 0.50f + chC * 0.25f + beep * 0.4f) * masterVolume;
        float right = (chA * 0.25f + chB * 0.50f + chC * 0.75f + beep * 0.4f) * masterVolume;

        // Clamp to [-1.0, 1.0]
        left = Math.max(-1.0f, Math.min(1.0f, left));
        right = Math.max(-1.0f, Math.min(1.0f, right));

        short leftSample = (short) (left * 32767.0f);
        short rightSample = (short) (right * 32767.0f);

        // 16-bit little-endian
        audioBuffer[bufferIndex++] = (byte) (leftSample & 0xFF);
        audioBuffer[bufferIndex++] = (byte) ((leftSample >> 8) & 0xFF);
        audioBuffer[bufferIndex++] = (byte) (rightSample & 0xFF);
        audioBuffer[bufferIndex++] = (byte) ((rightSample >> 8) & 0xFF);

        if (bufferIndex >= audioBuffer.length) {
            flushBuffer();
        }
    }

    public void flushBuffer() {
        if (line != null && bufferIndex > 0) {
            line.write(audioBuffer, 0, bufferIndex);
            bufferIndex = 0;
        }
    }

    public void close() {
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {}
        }
    }
}
