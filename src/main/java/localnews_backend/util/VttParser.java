package localnews_backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converts raw WebVTT subtitle content into clean, readable plain text.
 *
 * <p>The parser:
 * <ol>
 *   <li>Drops the WEBVTT file header and metadata lines.</li>
 *   <li>Strips timestamp cue lines ({@code 00:00:00.000 --> 00:00:04.000 …}).</li>
 *   <li>Removes inline HTML tags and WebVTT timestamp tags
 *       ({@code <c>}, {@code <00:00:00.599>}, {@code <b>}, etc.).</li>
 *   <li>Discards blank lines and consecutively duplicated lines (a common
 *       artefact of auto-generated captions).</li>
 *   <li>Joins the remaining lines into coherent paragraphs.</li>
 * </ol>
 */
public final class VttParser {

    /** Matches VTT cue timing lines: {@code 00:00:00.000 --> 00:00:04.000 …} */
    private static final Pattern TIMESTAMP_LINE =
            Pattern.compile("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+-->.*$");

    /** Matches any HTML / WebVTT inline tag, e.g. {@code <c>}, {@code <00:00:01.000>}, {@code </b>}. */
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    /** Matches VTT metadata / header key-value lines, e.g. {@code Kind: captions}. */
    private static final Pattern METADATA_LINE =
            Pattern.compile("^[A-Za-z]+:\\s*.+$");

    private VttParser() {}

    /**
     * Parses {@code vttContent} and returns a single plain-text string with
     * transcript sentences separated by spaces.
     *
     * @param vttContent raw contents of a {@code .vtt} file
     * @return clean transcript text, or an empty string if no cue text was found
     */
    public static String parse(String vttContent) {
        if (vttContent == null || vttContent.isBlank()) {
            return "";
        }

        String[] lines = vttContent.split("\\r?\\n");
        List<String> cleanLines = new ArrayList<>();
        String previous = null;

        for (String raw : lines) {
            String line = raw.trim();

            // Skip empty lines
            if (line.isEmpty()) continue;

            // Skip WEBVTT file identifier
            if (line.startsWith("WEBVTT")) continue;

            // Skip cue timing lines
            if (TIMESTAMP_LINE.matcher(line).matches()) continue;

            // Skip metadata header lines (Kind:, Language:, etc.)
            if (METADATA_LINE.matcher(line).matches()) continue;

            // Skip pure numeric cue identifiers
            if (line.matches("^\\d+$")) continue;

            // Strip inline tags
            String cleaned = HTML_TAG.matcher(line).replaceAll("").trim();

            if (cleaned.isEmpty()) continue;

            // Drop consecutive duplicates produced by caption roll-on behaviour
            if (cleaned.equals(previous)) continue;

            cleanLines.add(cleaned);
            previous = cleaned;
        }

        return String.join(" ", cleanLines);
    }
}
