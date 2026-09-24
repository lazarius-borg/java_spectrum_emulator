package nl.invokedynamic.spectrum.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Unified tape file loader supporting both .TAP and .TZX container formats.
 */
public final class TapeFormat {

    private TapeFormat() {}

    public static List<TapFileFormat.TapBlock> load(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String nameHint = Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("");
        return parse(bytes, nameHint);
    }

    public static List<TapFileFormat.TapBlock> load(InputStream is, String nameHint) throws IOException {
        byte[] bytes = is.readAllBytes();
        return parse(bytes, nameHint);
    }

    public static List<TapFileFormat.TapBlock> parse(byte[] bytes, String nameHint) throws IOException {
        if (TzxFileFormat.isTzx(bytes) || (nameHint != null && nameHint.toLowerCase().endsWith(".tzx"))) {
            return TzxFileFormat.parse(bytes);
        } else {
            return TapFileFormat.parse(bytes);
        }
    }
}
