package com.spectrum.storage;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the collection of loaded tapes, providing persistence and quick access.
 */
public final class TapeLibrary {
    private static final String DEFAULT_CONFIG_DIR = ".zxspectrum";
    private static final String DEFAULT_FILE_NAME = "tape_library.json";

    private final Path storageFile;
    private final ObservableList<TapeLibraryEntry> entries = FXCollections.observableArrayList();

    public TapeLibrary() {
        this(Path.of(System.getProperty("user.home"), DEFAULT_CONFIG_DIR, DEFAULT_FILE_NAME), true);
    }

    public TapeLibrary(Path storageFile) {
        this(storageFile, false);
    }

    public TapeLibrary(Path storageFile, boolean autoDiscover) {
        this.storageFile = storageFile;
        load();
        if (entries.isEmpty() && autoDiscover) {
            autoDiscoverLocalTapes();
        }
    }

    public ObservableList<TapeLibraryEntry> getEntries() {
        return entries;
    }

    /**
     * Adds or updates a tape entry in the library. If already present, updates
     * its timestamp and block count, and moves it to the top.
     */
    public synchronized TapeLibraryEntry addOrUpdate(Path tapePath, int blockCount) {
        if (tapePath == null) return null;
        String absPath = tapePath.toAbsolutePath().normalize().toString();
        String name = tapePath.getFileName() != null ? tapePath.getFileName().toString() : "Unknown.tap";

        long size = 0;
        try {
            if (Files.exists(tapePath)) {
                size = Files.size(tapePath);
            }
        } catch (IOException ignored) {}

        TapeLibraryEntry newEntry = new TapeLibraryEntry(
            name,
            absPath,
            blockCount,
            size,
            System.currentTimeMillis()
        );

        // Remove existing entry with matching path
        entries.removeIf(e -> e.filePath().equalsIgnoreCase(absPath));
        entries.addFirst(newEntry);

        save();
        return newEntry;
    }

    /**
     * Removes an entry from the library.
     */
    public synchronized void remove(TapeLibraryEntry entry) {
        if (entry != null && entries.remove(entry)) {
            save();
        }
    }

    /**
     * Clears all entries from the library.
     */
    public synchronized void clear() {
        entries.clear();
        save();
    }

    /**
     * Auto-discovers any TAP files in common project locations (e.g. ./taps).
     */
    private void autoDiscoverLocalTapes() {
        Path localTaps = Path.of("taps");
        if (Files.isDirectory(localTaps)) {
            try (var stream = Files.list(localTaps)) {
                stream.filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".tap") || name.endsWith(".tzx");
                })
                .sorted()
                .forEach(p -> {
                    try {
                        List<TapFileFormat.TapBlock> blocks = TapeFormat.load(p);
                        addOrUpdate(p, blocks.size());
                    } catch (Exception ignored) {}
                });
            } catch (IOException ignored) {}
        }
    }

    /**
     * Saves library entries to the JSON file.
     */
    public synchronized void save() {
        try {
            if (storageFile.getParent() != null) {
                Files.createDirectories(storageFile.getParent());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < entries.size(); i++) {
                TapeLibraryEntry e = entries.get(i);
                sb.append("  {\n");
                sb.append("    \"name\": \"").append(escapeJson(e.name())).append("\",\n");
                sb.append("    \"filePath\": \"").append(escapeJson(e.filePath())).append("\",\n");
                sb.append("    \"blockCount\": ").append(e.blockCount()).append(",\n");
                sb.append("    \"fileSizeBytes\": ").append(e.fileSizeBytes()).append(",\n");
                sb.append("    \"lastLoadedTimestamp\": ").append(e.lastLoadedTimestamp()).append("\n");
                sb.append("  }").append(i < entries.size() - 1 ? "," : "").append("\n");
            }
            sb.append("]\n");
            Files.writeString(storageFile, sb.toString());
        } catch (IOException e) {
            System.err.println("Warning: could not save tape library to " + storageFile + ": " + e.getMessage());
        }
    }

    /**
     * Loads library entries from the JSON file.
     */
    public synchronized void load() {
        entries.clear();
        if (!Files.exists(storageFile)) return;

        try {
            String json = Files.readString(storageFile);
            List<TapeLibraryEntry> loaded = parseJsonEntries(json);
            entries.addAll(loaded);
        } catch (Exception e) {
            System.err.println("Warning: could not load tape library from " + storageFile + ": " + e.getMessage());
        }
    }

    private static List<TapeLibraryEntry> parseJsonEntries(String json) {
        List<TapeLibraryEntry> result = new ArrayList<>();
        Pattern objectPattern = Pattern.compile("\\{([^{}]+)\\}");
        Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"");
        Pattern pathPattern = Pattern.compile("\"filePath\"\\s*:\\s*\"([^\"]*)\"");
        Pattern countPattern = Pattern.compile("\"blockCount\"\\s*:\\s*(\\d+)");
        Pattern sizePattern = Pattern.compile("\"fileSizeBytes\"\\s*:\\s*(\\d+)");
        Pattern timePattern = Pattern.compile("\"lastLoadedTimestamp\"\\s*:\\s*(\\d+)");

        Matcher objMatcher = objectPattern.matcher(json);
        while (objMatcher.find()) {
            String block = objMatcher.group(1);
            String name = extractString(namePattern, block, "Unknown");
            String path = extractString(pathPattern, block, "");
            int count = extractInt(countPattern, block, 0);
            long size = extractLong(sizePattern, block, 0L);
            long time = extractLong(timePattern, block, 0L);

            if (!path.isEmpty()) {
                result.add(new TapeLibraryEntry(unescapeJson(name), unescapeJson(path), count, size, time));
            }
        }
        return result;
    }

    private static String extractString(Pattern pattern, String content, String defaultVal) {
        Matcher m = pattern.matcher(content);
        return m.find() ? m.group(1) : defaultVal;
    }

    private static int extractInt(Pattern pattern, String content, int defaultVal) {
        Matcher m = pattern.matcher(content);
        return m.find() ? Integer.parseInt(m.group(1)) : defaultVal;
    }

    private static long extractLong(Pattern pattern, String content, long defaultVal) {
        Matcher m = pattern.matcher(content);
        return m.find() ? Long.parseLong(m.group(1)) : defaultVal;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
