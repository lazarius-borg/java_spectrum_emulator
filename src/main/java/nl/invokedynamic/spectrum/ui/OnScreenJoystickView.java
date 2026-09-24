package nl.invokedynamic.spectrum.ui;

import nl.invokedynamic.spectrum.io.Joystick;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Interactive retro arcade joystick & fire button widget.
 *
 * Supports:
 * - Direct mouse/trackpad drag-to-steer in 8 directions with authentic deadzone.
 * - Interactive arcade FIRE button.
 * - Real-time visual deflection reflecting active joystick state (from keyboard or mouse).
 */
public final class OnScreenJoystickView extends HBox {
    private static final double STICK_CANVAS_SIZE = 96.0;
    private static final double BASE_RADIUS = 38.0;
    private static final double BALL_RADIUS = 16.0;
    private static final double MAX_DEFLECTION = 18.0;

    private final Joystick joystick;
    private final Canvas stickCanvas;
    private final Button fireButton;
    private final Label infoLabel;

    private boolean isDragging = false;

    public OnScreenJoystickView(Joystick joystick) {
        this.joystick = joystick;
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(6, 16, 6, 16));
        setStyle("-fx-background-color: #1A1A1E; -fx-border-color: #333338; -fx-border-width: 1 0 0 0;");

        // 1. Left: Info & Mode Label
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("ARCADE JOYSTICK");
        titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #00E5FF;");
        infoLabel = new Label("Mode: " + joystick.getType());
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #AAAAAA;");
        Label hintLabel = new Label("Drag stick or click FIRE");
        hintLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #777777;");
        infoBox.getChildren().addAll(titleLabel, infoLabel, hintLabel);

        // 2. Center: Interactive Stick Canvas
        stickCanvas = new Canvas(STICK_CANVAS_SIZE, STICK_CANVAS_SIZE);
        setupStickMouseHandling();

        // 3. Right: Arcade Fire Button
        fireButton = new Button("FIRE");
        fireButton.setPrefSize(68, 68);
        setupFireButton();

        getChildren().addAll(infoBox, stickCanvas, fireButton);
        redrawStick(0, 0);
    }

    public void updateInfo(String modeText) {
        infoLabel.setText(modeText);
    }

    private void setupStickMouseHandling() {
        stickCanvas.setOnMousePressed(e -> {
            isDragging = true;
            processMouseVector(e.getX(), e.getY());
        });

        stickCanvas.setOnMouseDragged(e -> {
            if (isDragging) {
                processMouseVector(e.getX(), e.getY());
            }
        });

        stickCanvas.setOnMouseReleased(e -> {
            isDragging = false;
            joystick.setDirection(false, false, false, false);
            redrawStick(0, 0);
        });
    }

    private void processMouseVector(double mouseX, double mouseY) {
        double cx = STICK_CANVAS_SIZE / 2.0;
        double cy = STICK_CANVAS_SIZE / 2.0;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);

        if (dist < 8.0) {
            joystick.setDirection(false, false, false, false);
            redrawStick(0, 0);
            return;
        }

        // Clamp stick visual deflection
        double scale = Math.min(1.0, MAX_DEFLECTION / dist);
        double stickX = dx * scale;
        double stickY = dy * scale;

        // Angle in radians: 0 = East/Right, PI/2 = South/Down
        double angle = Math.atan2(dy, dx);
        double deg = Math.toDegrees(angle);
        if (deg < 0) deg += 360.0;

        // 8-way directional decode (45-degree sectors):
        // Sector 0: Right (337.5° - 22.5°)
        // Sector 1: Down-Right (22.5° - 67.5°)
        // Sector 2: Down (67.5° - 112.5°)
        // Sector 3: Down-Left (112.5° - 157.5°)
        // Sector 4: Left (157.5° - 202.5°)
        // Sector 5: Up-Left (202.5° - 247.5°)
        // Sector 6: Up (247.5° - 292.5°)
        // Sector 7: Up-Right (292.5° - 337.5°)
        boolean left = false;
        boolean right = false;
        boolean up = false;
        boolean down = false;

        if (deg >= 337.5 || deg < 22.5) {
            right = true;
        } else if (deg >= 22.5 && deg < 67.5) {
            right = true;
            down = true;
        } else if (deg >= 67.5 && deg < 112.5) {
            down = true;
        } else if (deg >= 112.5 && deg < 157.5) {
            left = true;
            down = true;
        } else if (deg >= 157.5 && deg < 202.5) {
            left = true;
        } else if (deg >= 202.5 && deg < 247.5) {
            left = true;
            up = true;
        } else if (deg >= 247.5 && deg < 292.5) {
            up = true;
        } else {
            right = true;
            up = true;
        }

        joystick.setDirection(left, right, up, down);
        redrawStick(stickX, stickY);
    }

    private void setupFireButton() {
        updateFireButtonStyle(false);
        fireButton.setOnMousePressed(e -> {
            joystick.setFire(true);
            updateFireButtonStyle(true);
        });
        fireButton.setOnMouseReleased(e -> {
            joystick.setFire(false);
            updateFireButtonStyle(false);
        });
    }

    private void updateFireButtonStyle(boolean pressed) {
        if (pressed) {
            fireButton.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 50%, #FF1744, #B71C1C); " +
                "-fx-background-radius: 34px; " +
                "-fx-border-color: #FFEA00; " +
                "-fx-border-width: 3px; " +
                "-fx-border-radius: 34px; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13px; " +
                "-fx-effect: dropshadow(gaussian, #FFEA00, 10, 0.4, 0, 0);"
            );
        } else {
            fireButton.setStyle(
                "-fx-background-color: radial-gradient(center 40% 40%, radius 60%, #E53935, #8E0000); " +
                "-fx-background-radius: 34px; " +
                "-fx-border-color: #551111; " +
                "-fx-border-width: 3px; " +
                "-fx-border-radius: 34px; " +
                "-fx-text-fill: #EEEEEE; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13px; " +
                "-fx-cursor: hand;"
            );
        }
    }

    /**
     * Polls the live joystick state (e.g. from keyboard) and updates the visual stick and button.
     */
    public void updateState() {
        if (!isDragging) {
            double dx = 0;
            double dy = 0;
            if (joystick.isLeft())  dx -= MAX_DEFLECTION;
            if (joystick.isRight()) dx += MAX_DEFLECTION;
            if (joystick.isUp())    dy -= MAX_DEFLECTION;
            if (joystick.isDown())  dy += MAX_DEFLECTION;

            if (dx != 0 && dy != 0) {
                dx *= 0.7071;
                dy *= 0.7071;
            }
            redrawStick(dx, dy);
        }

        updateFireButtonStyle(joystick.isFire());
    }

    private void redrawStick(double stickOffsetX, double stickOffsetY) {
        GraphicsContext gc = stickCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, STICK_CANVAS_SIZE, STICK_CANVAS_SIZE);

        double cx = STICK_CANVAS_SIZE / 2.0;
        double cy = STICK_CANVAS_SIZE / 2.0;

        // 1. Joystick outer base & bezel
        gc.setFill(Color.rgb(35, 35, 42));
        gc.fillOval(cx - BASE_RADIUS, cy - BASE_RADIUS, BASE_RADIUS * 2, BASE_RADIUS * 2);

        gc.setStroke(Color.rgb(65, 65, 75));
        gc.setLineWidth(2.0);
        gc.strokeOval(cx - BASE_RADIUS, cy - BASE_RADIUS, BASE_RADIUS * 2, BASE_RADIUS * 2);

        // Direction tick marks (N, S, E, W)
        gc.setStroke(Color.rgb(100, 100, 120));
        gc.setLineWidth(1.5);
        gc.strokeLine(cx - BASE_RADIUS + 4, cy, cx - BASE_RADIUS + 10, cy);
        gc.strokeLine(cx + BASE_RADIUS - 10, cy, cx + BASE_RADIUS - 4, cy);
        gc.strokeLine(cx, cy - BASE_RADIUS + 4, cx, cy - BASE_RADIUS + 10);
        gc.strokeLine(cx, cy + BASE_RADIUS - 10, cx, cy + BASE_RADIUS - 4);

        // Dust washer collar (black inner disk)
        double collarRadius = 22.0;
        gc.setFill(Color.rgb(15, 15, 18));
        gc.fillOval(cx - collarRadius + stickOffsetX * 0.4, cy - collarRadius + stickOffsetY * 0.4, collarRadius * 2, collarRadius * 2);

        // Shaft (metallic line from base to ball)
        double ballX = cx + stickOffsetX;
        double ballY = cy + stickOffsetY;
        gc.setStroke(Color.rgb(180, 180, 190));
        gc.setLineWidth(6.0);
        gc.strokeLine(cx, cy, ballX, ballY);

        // Ball-top (glossy retro red arcade sphere)
        RadialGradient ballGrad = new RadialGradient(
            0, 0,
            ballX - BALL_RADIUS * 0.35, ballY - BALL_RADIUS * 0.35,
            BALL_RADIUS * 1.2, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(255, 120, 120)),
            new Stop(0.4, Color.rgb(220, 20, 40)),
            new Stop(0.85, Color.rgb(140, 10, 25)),
            new Stop(1.0, Color.rgb(80, 5, 15))
        );
        gc.setFill(ballGrad);
        gc.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Specular highlight spot on ball
        gc.setFill(Color.rgb(255, 255, 255, 0.65));
        gc.fillOval(ballX - BALL_RADIUS * 0.5, ballY - BALL_RADIUS * 0.55, BALL_RADIUS * 0.5, BALL_RADIUS * 0.35);
    }
}
