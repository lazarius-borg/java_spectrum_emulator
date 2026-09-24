package nl.invokedynamic.spectrum.storage;

import java.io.File;
import java.nio.file.Path;

/**
 * Metadata record for a tape stored in the emulator's Tape Library.
 */
public record TapeLibraryEntry(
    String name,
    String filePath,
    int blockCount,
    long fileSizeBytes,
    long lastLoadedTimestamp
) {
    public boolean exists() {
        return filePath != null && new File(filePath).isFile();
    }

    public Path toPath() {
        return Path.of(filePath);
    }

    public String formattedSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format(java.util.Locale.ROOT, "%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }
}
