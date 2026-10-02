package nl.invokedynamic.spectrum.cpu;

import nl.invokedynamic.spectrum.io.IoBus;
import nl.invokedynamic.spectrum.memory.MemoryBus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless test harness for running CP/M-80 binaries (such as ZEXDOC and ZEXALL)
 * against the {@link Z80Cpu} emulator.
 *
 * <p>Emulates the minimal CP/M environment required by Frank Cringle's exercisers:
 * <ul>
 *   <li>Flat 64KB RAM layout with program loaded at {@code 0x0100}.</li>
 *   <li>CP/M warm boot / program termination trap at address {@code 0x0000} (HALT).</li>
 *   <li>BDOS system call vector at address {@code 0x0005} (RET opcode {@code 0xC9})
 *       intercepting console functions 2 (character output) and 9 (string output).</li>
 *   <li>TPA top of memory / stack initialization vector at address {@code 0x0006}.</li>
 * </ul>
 */
public final class CpmTestHarness {

    /**
     * Encapsulates the results of a CP/M execution run.
     *
     * @param fullOutput       the raw text output produced by BDOS console calls
     * @param passedTests      list of test names that reported OK
     * @param failedTests      list of test names that reported ERROR
     * @param completed        whether the program executed cleanly to warm boot (0x0000)
     * @param totalCycles      total T-states executed
     * @param executionTimeMs  wall-clock execution duration in milliseconds
     */
    public record ExecutionResult(
            String fullOutput,
            List<String> passedTests,
            List<String> failedTests,
            boolean completed,
            long totalCycles,
            long executionTimeMs
    ) {}

    private CpmTestHarness() {}

    /**
     * Executes a CP/M COM binary loaded from the classpath.
     *
     * @param resourcePath classpath path to the .com binary (e.g. {@code "/cpm/zexdoc.com"})
     * @param maxCycles    safety limit for total T-states before aborting (e.g. 5 billion)
     * @param printLive    whether to stream console output to {@link System#out} in real time
     * @return the execution results
     * @throws IOException if the resource binary cannot be read
     */
    public static ExecutionResult run(String resourcePath, long maxCycles, boolean printLive) throws IOException {
        return run(resourcePath, maxCycles, printLive, false);
    }

    /**
     * Executes only the {@code <daa,cpl,scf,ccf>} test group from the exerciser.
     * Takes ~20 milliseconds instead of ~2 minutes.
     */
    public static ExecutionResult runDaaOnly(String resourcePath, boolean printLive) throws IOException {
        return run(resourcePath, 2_000_000_000L, printLive, true);
    }

    public static ExecutionResult run(String resourcePath, long maxCycles, boolean printLive, boolean daaOnly) throws IOException {
        byte[] binary = loadBinary(resourcePath);

        SimpleMemory memory = new SimpleMemory();
        SimpleIo io = new SimpleIo();

        // Load binary into memory at 0x0100
        for (int i = 0; i < binary.length; i++) {
            memory.writeByte(0x0100 + i, binary[i] & 0xFF);
        }

        if (daaOnly) {
            // Patch tests table at 0x013A to point directly to DAA test (0x0642) followed by NULL terminator
            memory.writeByte(0x013A, 0x42);
            memory.writeByte(0x013B, 0x06);
            memory.writeByte(0x013C, 0x00);
            memory.writeByte(0x013D, 0x00);
        }

        // Setup CP/M Zero Page:
        // Address 0x0000: Warm boot trap -> 0x76 (HALT)
        memory.writeByte(0x0000, 0x76);

        // Address 0x0005: BDOS entry point -> 0xC9 (RET)
        memory.writeByte(0x0005, 0xC9);

        // Address 0x0006: Top of available memory (TPA limit / SP) -> 0xF000
        int tpaTop = 0xF000;
        memory.writeByte(0x0006, tpaTop & 0xFF);
        memory.writeByte(0x0007, (tpaTop >> 8) & 0xFF);

        Z80Cpu cpu = new Z80Cpu();
        cpu.getState().getRegisters().setPC(0x0100);

        StringBuilder output = new StringBuilder();
        long cycles = 0;
        boolean completed = false;
        long startTime = System.currentTimeMillis();

        while (!cpu.getState().isHalted() && cycles < maxCycles) {
            int pc = cpu.getState().getRegisters().getPC();

            if (pc == 0x0000) {
                completed = true;
                break;
            }

            if (pc == 0x0005) {
                // Intercept BDOS console output
                int func = cpu.getState().getRegisters().getC();
                if (func == 2) {
                    char ch = (char) (cpu.getState().getRegisters().getE() & 0xFF);
                    output.append(ch);
                    if (printLive) {
                        System.out.print(ch);
                        System.out.flush();
                    }
                } else if (func == 9) {
                    int addr = cpu.getState().getRegisters().getDE();
                    while (true) {
                        int b = memory.readByte(addr++);
                        if (b == '$') {
                            break;
                        }
                        char ch = (char) b;
                        output.append(ch);
                        if (printLive) {
                            System.out.print(ch);
                            System.out.flush();
                        }
                    }
                }
            }

            int stepCycles = cpu.step(memory, io);
            cycles += stepCycles;
        }

        if (cycles >= maxCycles) {
            var r = cpu.getState().getRegisters();
            System.err.printf("%n[TIMEOUT] PC=0x%04X, SP=0x%04X, AF=0x%04X, BC=0x%04X, DE=0x%04X, HL=0x%04X, IX=0x%04X, IY=0x%04X%n",
                    r.getPC(), r.getSP(), (r.getA() << 8) | r.getF(), r.getBC(), r.getDE(), r.getHL(), r.getIX(), r.getIY());
            System.err.printf("[TIMEOUT] Bytes at PC: %02X %02X %02X %02X %02X%n",
                    memory.readByte(r.getPC()), memory.readByte(r.getPC() + 1), memory.readByte(r.getPC() + 2),
                    memory.readByte(r.getPC() + 3), memory.readByte(r.getPC() + 4));
        }

        long durationMs = System.currentTimeMillis() - startTime;
        String fullOutput = output.toString();

        List<String> passed = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String line : fullOutput.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.contains("OK")) {
                passed.add(trimmed);
            } else if (trimmed.contains("ERROR")) {
                failed.add(trimmed);
            }
        }

        return new ExecutionResult(fullOutput, passed, failed, completed, cycles, durationMs);
    }

    private static byte[] loadBinary(String resourcePath) throws IOException {
        try (InputStream is = CpmTestHarness.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
            }
            return is.readAllBytes();
        }
    }

    /**
     * Minimal 64KB flat RAM bus for CP/M testing.
     */
    private static final class SimpleMemory implements MemoryBus {
        private final byte[] ram = new byte[65536];

        @Override
        public int readByte(int address) {
            return ram[address & 0xFFFF] & 0xFF;
        }

        @Override
        public void writeByte(int address, int value) {
            ram[address & 0xFFFF] = (byte) value;
        }
    }

    /**
     * Minimal empty I/O bus returning floating bus value 0xFF.
     */
    private static final class SimpleIo implements IoBus {
        @Override
        public int in(int port) {
            return 0xFF;
        }

        @Override
        public void out(int port, int value) {
            // No I/O peripherals connected
        }
    }
}
