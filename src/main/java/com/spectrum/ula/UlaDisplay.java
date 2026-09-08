package com.spectrum.ula;

/**
 * Emulates the ZX Spectrum ULA (Uncommitted Logic Array) video generation.
 *
 * Screen resolution: 256x192 pixels.
 * With border: 320x240 pixels (32px left/right, 24px top/bottom).
 * Frame timing: 70908 T-states per frame (128K model).
 */
public final class UlaDisplay {
    public static final int SCREEN_WIDTH = 256;
    public static final int SCREEN_HEIGHT = 192;
    public static final int BORDER_X = 32;
    public static final int BORDER_Y = 24;
    public static final int TOTAL_WIDTH = SCREEN_WIDTH + (BORDER_X * 2);   // 320
    public static final int TOTAL_HEIGHT = SCREEN_HEIGHT + (BORDER_Y * 2); // 240

    // Spectrum 128K Timing
    public static final int TSTATES_PER_LINE = 228;
    public static final int TOTAL_LINES = 311;
    public static final int TSTATES_PER_FRAME = TSTATES_PER_LINE * TOTAL_LINES; // 70908

    // ARGB Color Palettes: Normal [0..7] and Bright [8..15]
    public static final int[] PALETTE = new int[] {
        // Normal (0..7)
        0xFF000000, // 0: Black
        0xFF0000CD, // 1: Blue
        0xFFCD0000, // 2: Red
        0xFFCD00CD, // 3: Magenta
        0xFF00CD00, // 4: Green
        0xFF00CDCD, // 5: Cyan
        0xFFCDCD00, // 6: Yellow
        0xFFCDCDCD, // 7: White
        // Bright (8..15)
        0xFF000000, // 8: Bright Black
        0xFF0000FF, // 9: Bright Blue
        0xFFFF0000, // 10: Bright Red
        0xFFFF00FF, // 11: Bright Magenta
        0xFF00FF00, // 12: Bright Green
        0xFF00FFFF, // 13: Bright Cyan
        0xFFFFFF00, // 14: Bright Yellow
        0xFFFFFFFF  // 15: Bright White
    };

    // Framebuffer in ARGB format for direct rendering onto JavaFX Canvas / Image
    private final int[] frameBuffer = new int[TOTAL_WIDTH * TOTAL_HEIGHT];

    private int borderColor = 7; // Default white/gray
    private int flashFrameCounter = 0;
    private boolean flashInvert = false;

    public UlaDisplay() {
        clear(borderColor);
    }

    public void setBorderColor(int color) {
        this.borderColor = color & 7;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public int[] getFrameBuffer() {
        return frameBuffer;
    }

    public void clear(int border) {
        int color = PALETTE[border & 7];
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = color;
        }
    }

    /**
     * Renders a full frame from the 16KB RAM bank containing the active display file.
     * Offset 0x0000 - 0x17FF: 6144 bytes pixel bitmap.
     * Offset 0x1800 - 0x1AFF: 768 bytes attribute data.
     */
    public void renderFrame(byte[] screenBankRam) {
        // Update flash state every 16 frames (~1.56 Hz)
        flashFrameCounter++;
        if (flashFrameCounter >= 16) {
            flashFrameCounter = 0;
            flashInvert = !flashInvert;
        }

        int borderRgb = PALETTE[borderColor & 7];

        // 1. Fill top and bottom borders
        int topBottomLines = BORDER_Y;
        for (int y = 0; y < topBottomLines; y++) {
            int topOffset = y * TOTAL_WIDTH;
            int bottomOffset = (TOTAL_HEIGHT - 1 - y) * TOTAL_WIDTH;
            for (int x = 0; x < TOTAL_WIDTH; x++) {
                frameBuffer[topOffset + x] = borderRgb;
                frameBuffer[bottomOffset + x] = borderRgb;
            }
        }

        // 2. Render main screen and side borders line by line
        for (int y = 0; y < SCREEN_HEIGHT; y++) {
            int lineOffset = (y + BORDER_Y) * TOTAL_WIDTH;

            // Left border
            for (int bx = 0; bx < BORDER_X; bx++) {
                frameBuffer[lineOffset + bx] = borderRgb;
            }

            // Spectrum address calculation for scanline y:
            // y is 0..191: yThird (0..2), yCharRow (0..7), yCharLine (0..7)
            int yThird = y >> 6;
            int yCharLine = y & 7;
            int yCharRow = (y >> 3) & 7;
            int pixelLineAddr = (yThird << 11) | (yCharLine << 8) | (yCharRow << 5);
            int attrLineAddr = 0x1800 + ((y >> 3) * 32);

            int screenXOffset = lineOffset + BORDER_X;

            for (int col = 0; col < 32; col++) {
                int pixels = screenBankRam[pixelLineAddr + col] & 0xFF;
                int attr = screenBankRam[attrLineAddr + col] & 0xFF;

                int ink = attr & 0x07;
                int paper = (attr >> 3) & 0x07;
                int bright = ((attr & 0x40) != 0) ? 8 : 0;
                boolean flash = (attr & 0x80) != 0;

                if (flash && flashInvert) {
                    // Swap ink and paper on flash invert
                    int temp = ink;
                    ink = paper;
                    paper = temp;
                }

                int inkRgb = PALETTE[ink | bright];
                int paperRgb = PALETTE[paper | bright];

                int px = screenXOffset + (col << 3);
                frameBuffer[px]     = ((pixels & 0x80) != 0) ? inkRgb : paperRgb;
                frameBuffer[px + 1] = ((pixels & 0x40) != 0) ? inkRgb : paperRgb;
                frameBuffer[px + 2] = ((pixels & 0x20) != 0) ? inkRgb : paperRgb;
                frameBuffer[px + 3] = ((pixels & 0x10) != 0) ? inkRgb : paperRgb;
                frameBuffer[px + 4] = ((pixels & 0x08) != 0) ? inkRgb : paperRgb;
                frameBuffer[px + 5] = ((pixels & 0x04) != 0) ? inkRgb : paperRgb;
                frameBuffer[px + 6] = ((pixels & 0x02) != 0) ? inkRgb : paperRgb;
                frameBuffer[px + 7] = ((pixels & 0x01) != 0) ? inkRgb : paperRgb;
            }

            // Right border
            for (int bx = 0; bx < BORDER_X; bx++) {
                frameBuffer[lineOffset + BORDER_X + SCREEN_WIDTH + bx] = borderRgb;
            }
        }
    }
}
