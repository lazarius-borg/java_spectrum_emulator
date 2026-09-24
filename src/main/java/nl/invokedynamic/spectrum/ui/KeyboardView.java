package nl.invokedynamic.spectrum.ui;

import nl.invokedynamic.spectrum.io.Keyboard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive on-screen representation of the 40-key ZX Spectrum keyboard.
 *
 * Supports:
 * - Full 40-key matrix layout with authentic rubber-key aesthetics.
 * - Primary letters/digits, red Symbol Shift characters, and green/grey BASIC keywords.
 * - Direction arrows on 5, 6, 7, 8 and Delete on 0.
 * - Mouse click-to-type with latching for Shift keys.
 * - Real-time visual highlighting when host keyboard keys are pressed.
 */
public final class KeyboardView extends VBox {

    public record KeySpec(String mainLabel, String symLabel, String keyword, int row, int bit, double flexWidth) {}

    private final Keyboard keyboard;
    private final List<KeyButton> keyButtons = new ArrayList<>();

    // Latch state for on-screen clicking of shift keys
    private boolean capsShiftLatched = false;
    private boolean symbolShiftLatched = false;

    public KeyboardView(Keyboard keyboard) {
        this.keyboard = keyboard;
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setPadding(new Insets(8, 12, 8, 12));
        setStyle("-fx-background-color: #18181A; -fx-border-color: #333338; -fx-border-width: 1 0 0 0;");

        buildKeyboard();
    }

    private void buildKeyboard() {
        // Row 1: 1, 2, 3, 4, 5, 6, 7, 8, 9, 0
        List<KeySpec> row1 = List.of(
            new KeySpec("1", "!", "EDIT", Keyboard.ROW_1_2_3_4_5, 0, 1.0),
            new KeySpec("2", "@", "CAPS", Keyboard.ROW_1_2_3_4_5, 1, 1.0),
            new KeySpec("3", "#", "TRUE", Keyboard.ROW_1_2_3_4_5, 2, 1.0),
            new KeySpec("4", "$", "INV", Keyboard.ROW_1_2_3_4_5, 3, 1.0),
            new KeySpec("5 ←", "%", "LEFT", Keyboard.ROW_1_2_3_4_5, 4, 1.0),
            new KeySpec("6 ↓", "&", "DOWN", Keyboard.ROW_0_9_8_7_6, 4, 1.0),
            new KeySpec("7 ↑", "'", "UP", Keyboard.ROW_0_9_8_7_6, 3, 1.0),
            new KeySpec("8 →", "(", "RIGHT", Keyboard.ROW_0_9_8_7_6, 2, 1.0),
            new KeySpec("9", ")", "GRAPH", Keyboard.ROW_0_9_8_7_6, 1, 1.0),
            new KeySpec("0", "_", "DEL", Keyboard.ROW_0_9_8_7_6, 0, 1.0)
        );

        // Row 2: Q, W, E, R, T, Y, U, I, O, P
        List<KeySpec> row2 = List.of(
            new KeySpec("Q", "<=", "SIN", Keyboard.ROW_Q_W_E_R_T, 0, 1.0),
            new KeySpec("W", "<>", "COS", Keyboard.ROW_Q_W_E_R_T, 1, 1.0),
            new KeySpec("E", ">=", "TAN", Keyboard.ROW_Q_W_E_R_T, 2, 1.0),
            new KeySpec("R", "<", "INT", Keyboard.ROW_Q_W_E_R_T, 3, 1.0),
            new KeySpec("T", ">", "RND", Keyboard.ROW_Q_W_E_R_T, 4, 1.0),
            new KeySpec("Y", "AND", "STR$", Keyboard.ROW_P_O_I_U_Y, 4, 1.0),
            new KeySpec("U", "OR", "CHR$", Keyboard.ROW_P_O_I_U_Y, 3, 1.0),
            new KeySpec("I", "AT", "CODE", Keyboard.ROW_P_O_I_U_Y, 2, 1.0),
            new KeySpec("O", ";", "PEEK", Keyboard.ROW_P_O_I_U_Y, 1, 1.0),
            new KeySpec("P", "\"", "TAB", Keyboard.ROW_P_O_I_U_Y, 0, 1.0)
        );

        // Row 3: A, S, D, F, G, H, J, K, L, ENTER
        List<KeySpec> row3 = List.of(
            new KeySpec("A", "~", "READ", Keyboard.ROW_A_S_D_F_G, 0, 1.0),
            new KeySpec("S", "|", "REST", Keyboard.ROW_A_S_D_F_G, 1, 1.0),
            new KeySpec("D", "\\", "DATA", Keyboard.ROW_A_S_D_F_G, 2, 1.0),
            new KeySpec("F", "{", "SGN", Keyboard.ROW_A_S_D_F_G, 3, 1.0),
            new KeySpec("G", "}", "ABS", Keyboard.ROW_A_S_D_F_G, 4, 1.0),
            new KeySpec("H", "SQ", "SQR", Keyboard.ROW_ENTER_L_K_J_H, 4, 1.0),
            new KeySpec("J", "-", "VAL", Keyboard.ROW_ENTER_L_K_J_H, 3, 1.0),
            new KeySpec("K", "+", "LEN", Keyboard.ROW_ENTER_L_K_J_H, 2, 1.0),
            new KeySpec("L", "=", "USR", Keyboard.ROW_ENTER_L_K_J_H, 1, 1.0),
            new KeySpec("ENTER", "LOAD", "RETURN", Keyboard.ROW_ENTER_L_K_J_H, 0, 1.3)
        );

        // Row 4: CAPS SHIFT, Z, X, C, V, B, N, M, SYMBOL SHIFT, SPACE
        List<KeySpec> row4 = List.of(
            new KeySpec("CAPS SHIFT", "", "LOCK", Keyboard.ROW_CS_Z_X_C_V, 0, 1.4),
            new KeySpec("Z", ":", "LN", Keyboard.ROW_CS_Z_X_C_V, 1, 1.0),
            new KeySpec("X", "£", "EXP", Keyboard.ROW_CS_Z_X_C_V, 2, 1.0),
            new KeySpec("C", "?", "BIN", Keyboard.ROW_CS_Z_X_C_V, 3, 1.0),
            new KeySpec("V", "/", "CLS", Keyboard.ROW_CS_Z_X_C_V, 4, 1.0),
            new KeySpec("B", "*", "BORDER", Keyboard.ROW_SPACE_SS_M_N_B, 4, 1.0),
            new KeySpec("N", ",", "NEXT", Keyboard.ROW_SPACE_SS_M_N_B, 3, 1.0),
            new KeySpec("M", ".", "PAUSE", Keyboard.ROW_SPACE_SS_M_N_B, 2, 1.0),
            new KeySpec("SYM SHIFT", "", "EXT", Keyboard.ROW_SPACE_SS_M_N_B, 1, 1.4),
            new KeySpec("SPACE", "", "BREAK", Keyboard.ROW_SPACE_SS_M_N_B, 0, 1.6)
        );

        getChildren().addAll(createRow(row1), createRow(row2), createRow(row3), createRow(row4));
    }

    private HBox createRow(List<KeySpec> specs) {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        for (KeySpec spec : specs) {
            KeyButton btn = new KeyButton(spec);
            keyButtons.add(btn);
            HBox.setHgrow(btn, Priority.ALWAYS);
            row.getChildren().add(btn);
        }
        return row;
    }

    /**
     * Polls the keyboard matrix state to update live highlights.
     */
    public void updateState() {
        for (KeyButton btn : keyButtons) {
            boolean active = keyboard.isKeyPressed(btn.spec.row(), btn.spec.bit());
            btn.setActive(active);
        }
    }

    private final class KeyButton extends VBox {
        private final KeySpec spec;
        private final Label topLabel;
        private final Label mainLabel;
        private final Label bottomLabel;
        private boolean active = false;

        public KeyButton(KeySpec spec) {
            this.spec = spec;
            setAlignment(Pos.CENTER);
            setPadding(new Insets(2, 2, 2, 2));
            setPrefHeight(42);
            setMinHeight(42);
            setMaxHeight(42);

            topLabel = new Label(spec.symLabel().isEmpty() ? " " : spec.symLabel());
            topLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #FF4444; -fx-font-weight: bold;");

            mainLabel = new Label(spec.mainLabel());
            mainLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

            bottomLabel = new Label(spec.keyword().isEmpty() ? " " : spec.keyword());
            bottomLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #88CC88;");

            getChildren().addAll(topLabel, mainLabel, bottomLabel);

            updateStyle();

            // Mouse handling
            setOnMousePressed(e -> {
                handleMousePress();
            });

            setOnMouseReleased(e -> {
                handleMouseRelease();
            });
        }

        private void handleMousePress() {
            if (spec.row() == Keyboard.ROW_CS_Z_X_C_V && spec.bit() == 0) {
                // Caps Shift toggle latch
                capsShiftLatched = !capsShiftLatched;
                keyboard.setKeyPressed(spec.row(), spec.bit(), capsShiftLatched);
            } else if (spec.row() == Keyboard.ROW_SPACE_SS_M_N_B && spec.bit() == 1) {
                // Symbol Shift toggle latch
                symbolShiftLatched = !symbolShiftLatched;
                keyboard.setKeyPressed(spec.row(), spec.bit(), symbolShiftLatched);
            } else {
                keyboard.setKeyPressed(spec.row(), spec.bit(), true);
            }
            updateState();
        }

        private void handleMouseRelease() {
            if ((spec.row() == Keyboard.ROW_CS_Z_X_C_V && spec.bit() == 0) ||
                (spec.row() == Keyboard.ROW_SPACE_SS_M_N_B && spec.bit() == 1)) {
                // Latched keys stay in latched state
                return;
            }
            keyboard.setKeyPressed(spec.row(), spec.bit(), false);

            // If a regular key was pressed while a shift was latched, release the shift latch
            if (capsShiftLatched) {
                capsShiftLatched = false;
                keyboard.setKeyPressed(Keyboard.ROW_CS_Z_X_C_V, 0, false);
            }
            if (symbolShiftLatched) {
                symbolShiftLatched = false;
                keyboard.setKeyPressed(Keyboard.ROW_SPACE_SS_M_N_B, 1, false);
            }
            updateState();
        }

        public void setActive(boolean active) {
            if (this.active != active) {
                this.active = active;
                updateStyle();
            }
        }

        private void updateStyle() {
            if (active) {
                setStyle(
                    "-fx-background-color: #00E5FF; " +
                    "-fx-background-radius: 4px; " +
                    "-fx-border-color: #FFFFFF; " +
                    "-fx-border-radius: 4px; " +
                    "-fx-border-width: 1.5px; " +
                    "-fx-cursor: hand;"
                );
                mainLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000; -fx-font-weight: bold;");
                topLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #990000; -fx-font-weight: bold;");
                bottomLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #004400;");
            } else {
                // Special styling for Shift keys
                if (spec.row() == Keyboard.ROW_CS_Z_X_C_V && spec.bit() == 0) {
                    setStyle(
                        "-fx-background-color: #383842; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-border-color: #555566; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-border-width: 1px; " +
                        "-fx-cursor: hand;"
                    );
                    mainLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #E0E0FF; -fx-font-weight: bold;");
                } else if (spec.row() == Keyboard.ROW_SPACE_SS_M_N_B && spec.bit() == 1) {
                    setStyle(
                        "-fx-background-color: #4A2020; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-border-color: #AA4444; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-border-width: 1px; " +
                        "-fx-cursor: hand;"
                    );
                    mainLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #FF9999; -fx-font-weight: bold;");
                } else {
                    setStyle(
                        "-fx-background-color: #26262B; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-border-color: #3C3C44; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-border-width: 1px; " +
                        "-fx-cursor: hand;"
                    );
                    mainLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
                }
                topLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #FF5555; -fx-font-weight: bold;");
                bottomLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: #88CC88;");
            }
        }
    }
}
