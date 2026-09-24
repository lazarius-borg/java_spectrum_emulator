package nl.invokedynamic.spectrum.cpu;

/**
 * Fast 4 T-state Accumulator-only rotate operations of the Z80 CPU:
 * <ul>
 *   <li>{@code RLCA}: Rotate Left Circular Accumulator</li>
 *   <li>{@code RRCA}: Rotate Right Circular Accumulator</li>
 *   <li>{@code RLA}: Rotate Left Accumulator through Carry</li>
 *   <li>{@code RRA}: Rotate Right Accumulator through Carry</li>
 * </ul>
 */
public enum RotateAccOp {
    RLCA, RRCA, RLA, RRA
}
