package localnews_backend.service;

import localnews_backend.exception.InvalidYoutubeUrlException;
import localnews_backend.exception.TranscriptExtractionException;
import localnews_backend.util.VttParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the English subtitle transcript from a YouTube video using
 * <a href="https://github.com/yt-dlp/yt-dlp">yt-dlp</a>.
 *
 * <p>The service:
 * <ol>
 *   <li>Validates that the URL is a recognisable YouTube video URL.</li>
 *   <li>Runs {@code yt-dlp} in a private temporary directory to avoid
 *       collisions between concurrent requests.</li>
 *   <li>Locates the generated {@code .vtt} file and parses it with
 *       {@link VttParser}.</li>
 *   <li>Deletes the temporary directory regardless of success or failure.</li>
 * </ol>
 */
@Service
@Slf4j
public class TranscriptExtractionService {

    private static final String YOUTUBE_ID_REGEX = "[A-Za-z0-9_-]{11}";
    private static final Pattern XML_TEXT_TAG = Pattern.compile("<text\\b[^>]*>(.*?)</text>", Pattern.DOTALL);

    @Value("${ytdlp.timeout.seconds:120}")
    private int ytdlpTimeoutSeconds;

    /** Optional path to a Netscape-format cookies file. When set, passed to yt-dlp via {@code --cookies}. */
    @Value("${ytdlp.cookies.file:}")
    private String ytdlpCookiesFile;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Downloads and returns the English transcript for the given YouTube URL.
     *
     * @param youtubeUrl a YouTube watch / short / embed URL
     * @return plain-text transcript
     * @throws InvalidYoutubeUrlException    if the URL is not a YouTube URL
     * @throws TranscriptExtractionException if yt-dlp fails or no subtitles are found
     */
    public String extractTranscript(String youtubeUrl) {
        validateYoutubeUrl(youtubeUrl);

        Path tempDir = createTempDir();
        try {
            try {
                runYtDlp(youtubeUrl, tempDir);
                File vttFile = findVttFile(tempDir);
                String vttContent = readFile(vttFile);
                String transcript = VttParser.parse(vttContent);
                if (transcript.isBlank()) {
                    throw new TranscriptExtractionException(
                            "Transcript is empty – the video may have no readable subtitle content");
                }
                return transcript;
            } catch (TranscriptExtractionException e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("bot-check")) {
                    log.info("yt-dlp hit bot-check, trying timedtext fallback");
                    return extractTranscriptViaTimedText(youtubeUrl);
                }
                throw e;
            }
        } finally {
            deleteTempDir(tempDir);
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateYoutubeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidYoutubeUrlException("YouTube URL must not be blank");
        }
        // Guard against argument injection: a valid URL never starts with a dash
        if (url.trim().startsWith("-")) {
            throw new InvalidYoutubeUrlException("Invalid YouTube URL: " + url);
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host == null
                    || (!host.endsWith("youtube.com") && !host.endsWith("youtu.be"))) {
                throw new InvalidYoutubeUrlException(
                        "Invalid YouTube URL – must be a youtube.com or youtu.be link");
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidYoutubeUrlException("Malformed URL: " + url);
        }
    }

    // ── yt-dlp process ────────────────────────────────────────────────────────

    private void runYtDlp(String youtubeUrl, Path tempDir) {
        String outputTemplate = tempDir.resolve("output.%(ext)s").toString();

        // Use android,web player clients to reduce bot-check rejections
        List<String> command = new ArrayList<>(List.of(
                "yt-dlp",
                "--write-subs",
                "--write-auto-subs",
                "--skip-download",
                "--sub-lang", "en.*,en",
                "--no-playlist",
                "--extractor-args", "youtube:player_client=android,web",
                "-o", outputTemplate
        ));

        if (ytdlpCookiesFile != null && !ytdlpCookiesFile.isBlank()) {
            command.add("--cookies");
            command.add(ytdlpCookiesFile.trim());
        }

        command.add(youtubeUrl);

        log.info("Running yt-dlp for URL: {}", youtubeUrl);
        log.debug("yt-dlp command: {}", command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new TranscriptExtractionException(
                    "yt-dlp is not installed or could not be started: " + e.getMessage(), e);
        }

        String processOutput;
        try {
            processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(ytdlpTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TranscriptExtractionException(
                        "yt-dlp timed out after " + ytdlpTimeoutSeconds + " seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new TranscriptExtractionException("yt-dlp was interrupted", e);
        } catch (IOException e) {
            throw new TranscriptExtractionException(
                    "Failed to read yt-dlp output: " + e.getMessage(), e);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("yt-dlp exited with code {}: {}", exitCode, processOutput);
            throw createYtDlpFailure(exitCode, processOutput);
        }

        log.debug("yt-dlp output: {}", processOutput);
    }

    // ── File helpers ──────────────────────────────────────────────────────────

    private Path createTempDir() {
        try {
            return Files.createTempDirectory("ytdlp_" + UUID.randomUUID());
        } catch (IOException e) {
            throw new TranscriptExtractionException(
                    "Failed to create temporary directory: " + e.getMessage(), e);
        }
    }

    private File findVttFile(Path dir) {
        File[] vttFiles = dir.toFile().listFiles(
                f -> f.isFile() && f.getName().endsWith(".vtt"));
        if (vttFiles == null || vttFiles.length == 0) {
            throw new TranscriptExtractionException(
                    "No subtitles were downloaded – the video may not have English captions");
        }
        // Prefer a file containing "en" in its name (auto-generated captions)
        return Arrays.stream(vttFiles)
                .filter(f -> f.getName().contains(".en"))
                .findFirst()
                .orElse(vttFiles[0]);
    }

    private String readFile(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TranscriptExtractionException(
                    "Failed to read subtitle file: " + e.getMessage(), e);
        }
    }

    private void deleteTempDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Failed to clean up temporary directory {}: {}", dir, e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private TranscriptExtractionException createYtDlpFailure(int exitCode, String processOutput) {
        String lowerOutput = processOutput == null ? "" : processOutput.toLowerCase();

        if (lowerOutput.contains("no automatic captions")
                || lowerOutput.contains("no subtitles")
                || lowerOutput.contains("subtitles not available")) {
            return new TranscriptExtractionException(
                    "No English subtitles are available for this video");
        }

        if (lowerOutput.contains("sign in to confirm you") && lowerOutput.contains("not a bot")) {
            return new TranscriptExtractionException(
                    "YouTube blocked automated access for this video (bot-check). " +
                    "Try another video, or configure yt-dlp authentication cookies.");
        }

        return new TranscriptExtractionException(
                "yt-dlp failed (exit " + exitCode + "). Output: " + truncate(processOutput, 300));
    }

    private String extractTranscriptViaTimedText(String youtubeUrl) {
        String videoId = extractYoutubeId(youtubeUrl)
                .orElseThrow(() -> new TranscriptExtractionException("Could not parse YouTube video id from URL"));
        List<String> urls = List.of(
                "https://www.youtube.com/api/timedtext?lang=en&v=" + videoId,
                "https://www.youtube.com/api/timedtext?lang=en-US&v=" + videoId,
                "https://www.youtube.com/api/timedtext?lang=en&kind=asr&v=" + videoId
        );

        for (String url : urls) {
            String transcript = fetchTimedTextTranscript(url);
            if (!transcript.isBlank()) {
                return transcript;
            }
        }

        throw new TranscriptExtractionException(
                "YouTube blocked automated access for this video and transcript fallback failed. " +
                        "Try another video, or configure yt-dlp authentication cookies.");
    }

    private String fetchTimedTextTranscript(String timedTextUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(timedTextUrl)).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return "";
            }
            return parseTimedTextXml(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (Exception e) {
            log.warn("TimedText fallback request failed: {}", e.getMessage());
            return "";
        }
    }

    private String parseTimedTextXml(String xml) {
        Matcher matcher = XML_TEXT_TAG.matcher(xml);
        List<String> parts = new ArrayList<>();
        while (matcher.find()) {
            String decoded = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
            String normalized = decodeXmlEntities(decoded)
                    .replaceAll("<[^>]+>", "")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!normalized.isBlank()) {
                parts.add(normalized);
            }
        }
        return String.join(" ", parts).trim();
    }

    private String decodeXmlEntities(String text) {
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private Optional<String> extractYoutubeId(String videoUrl) {
        try {
            URI uri = URI.create(videoUrl);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

            if (host.endsWith("youtu.be")) {
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    String id = path.substring(1);
                    return id.matches(YOUTUBE_ID_REGEX) ? Optional.of(id) : Optional.empty();
                }
            }

            if (host.endsWith("youtube.com")) {
                String path = uri.getPath() == null ? "" : uri.getPath();
                String query = uri.getQuery() == null ? "" : uri.getQuery();

                if (path.equals("/watch")) {
                    for (String part : query.split("&")) {
                        String[] kv = part.split("=", 2);
                        if (kv.length == 2 && kv[0].equals("v") && kv[1].matches(YOUTUBE_ID_REGEX)) {
                            return Optional.of(kv[1]);
                        }
                    }
                }

                if (path.startsWith("/embed/") || path.startsWith("/shorts/")) {
                    String[] segments = path.split("/");
                    String id = segments[segments.length - 1];
                    return id.matches(YOUTUBE_ID_REGEX) ? Optional.of(id) : Optional.empty();
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
