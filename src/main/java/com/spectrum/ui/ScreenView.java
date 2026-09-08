package com.spectrum.ui;

import com.spectrum.ula.UlaDisplay;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

/**
 * High-performance JavaFX Canvas for rendering the ZX Spectrum frame buffer
 * with pixel-perfect scaling.
 */
public final class ScreenView extends Pane {
    private final Canvas canvas;
    private final WritableImage image;
    private final PixelFormat<java.nio.IntBuffer> pixelFormat;

    public ScreenView() {
        this.canvas = new Canvas(UlaDisplay.TOTAL_WIDTH * 2, UlaDisplay.TOTAL_HEIGHT * 2);
        this.image = new WritableImage(UlaDisplay.TOTAL_WIDTH, UlaDisplay.TOTAL_HEIGHT);
        this.pixelFormat = PixelFormat.getIntArgbInstance();

        getChildren().add(canvas);

        // Keep canvas responsive and centered
        widthProperty().addListener((obs, oldV, newV) -> resizeCanvas());
        heightProperty().addListener((obs, oldV, newV) -> resizeCanvas());
    }

    private void resizeCanvas() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Preserve 4:3 aspect ratio
        double aspect = (double) UlaDisplay.TOTAL_WIDTH / UlaDisplay.TOTAL_HEIGHT;
        double targetW = w;
        double targetH = w / aspect;
        if (targetH > h) {
            targetH = h;
            targetW = h * aspect;
        }

        canvas.setWidth(targetW);
        canvas.setHeight(targetH);
        canvas.setLayoutX((w - targetW) / 2.0);
        canvas.setLayoutY((h - targetH) / 2.0);
    }

    /**
     * Updates the canvas from the 320x240 ARGB ULA frame buffer.
     */
    public void render(int[] frameBuffer) {
        image.getPixelWriter().setPixels(
            0, 0,
            UlaDisplay.TOTAL_WIDTH, UlaDisplay.TOTAL_HEIGHT,
            pixelFormat,
            frameBuffer, 0, UlaDisplay.TOTAL_WIDTH
        );

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false); // Pixel-perfect sharp retro look
        gc.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
