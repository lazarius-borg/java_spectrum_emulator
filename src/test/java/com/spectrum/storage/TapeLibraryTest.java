package com.spectrum.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TapeLibraryTest {

    @TempDir
    Path tempDir;

    @Test
    void testAddAndPersistTapeEntries() throws IOException {
        Path jsonFile = tempDir.resolve("tape_library.json");
        Path dummyTape = tempDir.resolve("GAME1.TAP");
        Files.write(dummyTape, new byte[1024]);

        TapeLibrary library = new TapeLibrary(jsonFile);
        assertThat(library.getEntries()).isEmpty();

        // Add dummy tape
        TapeLibraryEntry entry1 = library.addOrUpdate(dummyTape, 5);
        assertThat(entry1.name()).isEqualTo("GAME1.TAP");
        assertThat(entry1.blockCount()).isEqualTo(5);
        assertThat(entry1.fileSizeBytes()).isEqualTo(1024);
        assertThat(entry1.exists()).isTrue();
        assertThat(entry1.formattedSize()).isEqualTo("1.0 KB");
        assertThat(library.getEntries()).hasSize(1);

        // Add second tape
        Path dummyTape2 = tempDir.resolve("GAME2.TAP");
        Files.write(dummyTape2, new byte[512]);
        library.addOrUpdate(dummyTape2, 3);
        assertThat(library.getEntries()).hasSize(2);
        assertThat(library.getEntries().get(0).name()).isEqualTo("GAME2.TAP");

        // Reload library from disk in a new instance
        TapeLibrary reloaded = new TapeLibrary(jsonFile);
        assertThat(reloaded.getEntries()).hasSize(2);
        assertThat(reloaded.getEntries().get(0).name()).isEqualTo("GAME2.TAP");
        assertThat(reloaded.getEntries().get(1).name()).isEqualTo("GAME1.TAP");

        // Remove an entry
        reloaded.remove(reloaded.getEntries().get(0));
        assertThat(reloaded.getEntries()).hasSize(1);

        // Reload to verify persistence of removal
        TapeLibrary afterRemove = new TapeLibrary(jsonFile);
        assertThat(afterRemove.getEntries()).hasSize(1);
        assertThat(afterRemove.getEntries().get(0).name()).isEqualTo("GAME1.TAP");
    }
}
