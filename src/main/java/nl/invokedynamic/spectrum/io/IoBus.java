package nl.invokedynamic.spectrum.io;

/**
 * Interface representing the I/O bus for reading and writing ports.
 */
public interface IoBus {
    /**
     * Reads a byte from the specified 16-bit I/O port address.
     */
    int in(int port);

    /**
     * Writes a byte to the specified 16-bit I/O port address.
     */
    void out(int port, int value);
}
