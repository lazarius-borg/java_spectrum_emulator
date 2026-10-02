package nl.invokedynamic.spectrum.cpu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Executes Frank Cringle's ZEXDOC instruction exerciser to validate
 * all documented Z80 instructions and documented flag calculations.
 */
@Tag("exerciser")
public class ZexdocTest {

    private static final String ZEXDOC_RESOURCE = "/cpm/zexdoc.com";
    private static final long MAX_CYCLES = 50_000_000_000L;

    @Test
    @DisplayName("Run ZEXDOC instruction set exerciser (documented flags)")
    void testZexdocInstructionSetExerciser() throws IOException {
        System.out.println("=== Starting ZEXDOC Instruction Exerciser ===");
        var result = CpmTestHarness.run(ZEXDOC_RESOURCE, MAX_CYCLES, true);

        System.out.printf("%n=== ZEXDOC Execution Summary ===%n");
        System.out.printf("Total T-States: %,d%n", result.totalCycles());
        System.out.printf("Duration: %,d ms%n", result.executionTimeMs());
        System.out.printf("Passed Tests: %d%n", result.passedTests().size());
        System.out.printf("Failed Tests: %d%n", result.failedTests().size());

        if (!result.failedTests().isEmpty()) {
            System.err.println("FAILED TESTS:");
            result.failedTests().forEach(System.err::println);
        }

        assertThat(result.completed())
                .as("ZEXDOC must run to completion (warm boot at 0x0000)")
                .isTrue();

        assertThat(result.fullOutput())
                .as("ZEXDOC output must contain 'Tests complete'")
                .contains("Tests complete");

        assertThat(result.failedTests())
                .as("All documented flag tests must pass without ERROR")
                .isEmpty();
    }
}
