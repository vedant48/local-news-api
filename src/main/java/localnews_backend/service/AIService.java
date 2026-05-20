package localnews_backend.service;

import tools.jackson.databind.ObjectMapper;
import localnews_backend.dto.AIGeneratedBlog;
import localnews_backend.exception.OllamaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a YouTube video URL into a structured, SEO-friendly blog post by:
 * <ol>
 *   <li>Extracting the English transcript via {@link TranscriptExtractionService} (yt-dlp).</li>
 *   <li>Sending a structured JSON prompt to a locally running Ollama instance.</li>
 *   <li>Parsing the model output and mapping it to {@link AIGeneratedBlog}.</li>
 * </ol>
 *
 * <p>Configure the following properties in {@code application.properties}:
 * <ul>
 *   <li>{@code ollama.model} – Ollama model name (default: {@code llama3})</li>
 *   <li>{@code ollama.timeout.seconds} – Generation timeout in seconds (default: {@code 300})</li>
 *   <li>{@code transcript.max.chars} – Maximum transcript length fed to the model (default: {@code 4000})</li>
 * </ul>
 */
@Service
@Slf4j
public class AIService {

    private static final String YOUTUBE_ID_REGEX = "[A-Za-z0-9_-]{11}";

    /** Matches a JSON object possibly wrapped in a markdown code fence. */
    private static final Pattern JSON_FENCE =
            Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.DOTALL);

    private static final Pattern JSON_OBJECT =
            Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);

    @Value("${ollama.model:llama3}")
    private String ollamaModel;

    @Value("${ollama.timeout.seconds:300}")
    private int ollamaTimeoutSeconds;

    @Value("${transcript.max.chars:4000}")
    private int transcriptMaxChars;

    private final TranscriptExtractionService transcriptService;
    private final ObjectMapper objectMapper;

    public AIService(TranscriptExtractionService transcriptService,
                     ObjectMapper objectMapper) {
        this.transcriptService = transcriptService;
        this.objectMapper = objectMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a fully structured blog from the supplied YouTube video URL.
     *
     * @param videoUrl a YouTube watch / short / embed URL
     * @return structured blog post
     * @throws localnews_backend.exception.InvalidYoutubeUrlException    if the URL is not a valid YouTube URL
     * @throws localnews_backend.exception.TranscriptExtractionException if yt-dlp fails
     * @throws OllamaException                                           if Ollama is unavailable or times out
     */
    public AIGeneratedBlog generateBlogFromVideo(String videoUrl) {
        log.info("Generating blog for video URL: {}", videoUrl);

        String transcript = transcriptService.extractTranscript(videoUrl);
        String truncatedTranscript = truncateTranscript(transcript);

        log.info("Transcript extracted ({} chars, fed {} chars to model)",
                transcript.length(), truncatedTranscript.length());

        String prompt = buildPrompt(truncatedTranscript);
        String rawOutput = callOllama(prompt);

        AIGeneratedBlog blog = parseOllamaOutput(rawOutput);
        backfillCoverImage(blog, videoUrl);

        log.info("Blog generated: title='{}'", blog.getTitle());
        return blog;
    }

    // ── Ollama process ────────────────────────────────────────────────────────

    private String callOllama(String prompt) {
        List<String> command = List.of("ollama", "run", ollamaModel);
        log.debug("Calling Ollama model '{}' (timeout: {}s)", ollamaModel, ollamaTimeoutSeconds);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new OllamaException(
                    "Ollama is not installed or could not be started. " +
                    "Ensure Ollama is running locally: " + e.getMessage(), e);
        }

        // Write prompt to Ollama's stdin and close the stream
        try (OutputStream stdin = process.getOutputStream();
             PrintWriter writer = new PrintWriter(
                     new BufferedWriter(new OutputStreamWriter(stdin, StandardCharsets.UTF_8)))) {
            writer.print(prompt);
        } catch (IOException e) {
            process.destroyForcibly();
            throw new OllamaException("Failed to write prompt to Ollama: " + e.getMessage(), e);
        }

        // Read stdout in a background thread to prevent buffer deadlock
        StringBuilder outputBuilder = new StringBuilder();
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append('\n');
                }
            } catch (IOException ignored) {
                // process ended
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();

        try {
            boolean finished = process.waitFor(ollamaTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new OllamaException(
                        "Ollama timed out after " + ollamaTimeoutSeconds + " seconds");
            }
            readerThread.join(Math.max(5_000L, ollamaTimeoutSeconds * 1000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new OllamaException("Ollama process was interrupted", e);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new OllamaException(
                    "Ollama exited with non-zero status code: " + exitCode);
        }

        return outputBuilder.toString().trim();
    }

    // ── Prompt building ───────────────────────────────────────────────────────

    private String buildPrompt(String transcript) {
        return """
                You are an expert journalist and content strategist.

                Convert the following YouTube video transcript into a professional news blog article.

                Return ONLY a single valid JSON object – no markdown fences, no extra text – matching this exact schema:
                {
                  "title": "Engaging SEO-optimized headline (max 70 characters)",
                  "seoTitle": "SEO title for <title> tag (max 60 characters)",
                  "seoDescription": "Meta description for search engines (max 160 characters)",
                  "slug": "url-friendly-hyphenated-slug-from-title",
                  "excerpt": "2-3 sentence compelling teaser that hooks the reader",
                  "category": "one of: News | Technology | Business | Sports | Entertainment | Politics | Health | Science | Education | Lifestyle",
                  "readingTimeMinutes": <integer>,
                  "tableOfContents": [
                    { "id": "section-slug", "heading": "Section Heading", "level": "h2" }
                  ],
                  "sections": [
                    {
                      "id": "section-slug",
                      "heading": "Section Heading",
                      "level": "h2",
                      "content": "Detailed markdown content with proper formatting (minimum 150 words per section)."
                    }
                  ],
                  "keyTakeaways": [
                    "Concise key insight 1",
                    "Concise key insight 2",
                    "Concise key insight 3",
                    "Concise key insight 4",
                    "Concise key insight 5"
                  ],
                  "faqs": [
                    { "question": "Reader question?", "answer": "Comprehensive answer." }
                  ],
                  "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
                  "content": "Complete polished markdown blog post (minimum 800 words)"
                }

                Include:
                - Catchy headline
                - Structured subheadings (h2 / h3)
                - Summary / TL;DR section
                - Key highlights
                - SEO keywords in the tags array
                - Conclusion

                Requirements:
                - Professional, engaging journalistic tone
                - At least 4 detailed sections (each ≥ 150 words)
                - Markdown formatting throughout (headers, bold, italic, bullet lists)
                - 3-5 FAQ items covering likely reader questions
                - All JSON strings must be properly escaped (no raw newlines inside JSON string values)

                Transcript:
                %s
                """.formatted(transcript);
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    /**
     * Attempts to deserialise {@code rawOutput} as JSON.
     * Falls back to extracting the first JSON object from the text if the
     * output contains explanatory prose around it, then falls back to a
     * minimal plain-text blog if JSON parsing fails entirely.
     */
    private AIGeneratedBlog parseOllamaOutput(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            log.warn("Ollama returned empty output; building minimal blog");
            return buildMinimalBlog(null);
        }

        // 1. Try direct parse
        try {
            return objectMapper.readValue(rawOutput, AIGeneratedBlog.class);
        } catch (Exception ignored) {
            // fall through
        }

        // 2. Strip markdown code fence and retry
        Matcher fenceMatcher = JSON_FENCE.matcher(rawOutput);
        if (fenceMatcher.find()) {
            try {
                return objectMapper.readValue(fenceMatcher.group(1), AIGeneratedBlog.class);
            } catch (Exception ignored) {
                // fall through
            }
        }

        // 3. Locate first '{' … last '}' in the output and retry
        Matcher objectMatcher = JSON_OBJECT.matcher(rawOutput);
        if (objectMatcher.find()) {
            try {
                return objectMapper.readValue(objectMatcher.group(), AIGeneratedBlog.class);
            } catch (Exception ignored) {
                // fall through
            }
        }

        // 4. Last resort: wrap the raw text in a minimal blog structure
        log.warn("Could not parse Ollama output as JSON; building minimal blog from raw text");
        return buildMinimalBlog(rawOutput);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String truncateTranscript(String transcript) {
        if (transcript == null) return "";
        return transcript.length() <= transcriptMaxChars
                ? transcript
                : transcript.substring(0, transcriptMaxChars) + "…";
    }

    /**
     * Extracts the 11-character YouTube video ID from a URL using standard URI
     * parsing – no regex backtracking, no ReDoS risk.
     *
     * <p>Handles the following URL forms:
     * <ul>
     *   <li>{@code https://www.youtube.com/watch?v=ID}</li>
     *   <li>{@code https://www.youtube.com/watch?list=X&v=ID}</li>
     *   <li>{@code https://www.youtube.com/embed/ID}</li>
     *   <li>{@code https://www.youtube.com/shorts/ID}</li>
     *   <li>{@code https://youtu.be/ID}</li>
     * </ul>
     */
    public Optional<String> extractYoutubeId(String url) {
        if (url == null || url.isBlank()) return Optional.empty();
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return Optional.empty();

            if (host.endsWith("youtu.be")) {
                return idFromPath(uri.getPath(), 1);
            }

            if (host.endsWith("youtube.com")) {
                String path = uri.getPath() != null ? uri.getPath() : "";
                if (path.startsWith("/embed/") || path.startsWith("/shorts/")) {
                    return idFromPath(path, 2);
                }
                if (path.equals("/watch")) {
                    return idFromQueryParam(uri.getQuery(), "v");
                }
            }
        } catch (Exception ignored) {
            // Malformed URL – treat as non-YouTube
        }
        return Optional.empty();
    }

    /** Returns the path segment at {@code segmentIndex} (1-based) if it is a valid video ID. */
    private Optional<String> idFromPath(String path, int segmentIndex) {
        if (path == null) return Optional.empty();
        String[] segments = path.split("/");
        if (segments.length > segmentIndex) {
            String candidate = segments[segmentIndex];
            if (candidate.length() == 11 && candidate.matches(YOUTUBE_ID_REGEX)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Returns the value of a query parameter by name if it is a valid video ID. */
    private Optional<String> idFromQueryParam(String query, String paramName) {
        if (query == null) return Optional.empty();
        String prefix = paramName + "=";
        for (String param : query.split("&")) {
            if (param.startsWith(prefix)) {
                String candidate = param.substring(prefix.length());
                if (candidate.length() == 11 && candidate.matches(YOUTUBE_ID_REGEX)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    /** Sets the YouTube thumbnail URL on the blog if not already populated. */
    private void backfillCoverImage(AIGeneratedBlog blog, String videoUrl) {
        if (blog.getCoverImageUrl() == null || blog.getCoverImageUrl().isBlank()) {
            extractYoutubeId(videoUrl).ifPresent(id ->
                    blog.setCoverImageUrl("https://img.youtube.com/vi/" + id + "/maxresdefault.jpg")
            );
        }
    }

    // ── Minimal fallback ──────────────────────────────────────────────────────

    private AIGeneratedBlog buildMinimalBlog(String rawContent) {
        AIGeneratedBlog blog = new AIGeneratedBlog();
        blog.setTitle("Video Blog Post");
        blog.setSeoTitle("Video Blog Post");
        blog.setSeoDescription("A blog post generated from a video transcript.");
        blog.setSlug("video-blog-post");
        blog.setExcerpt("Blog generated from video transcript.");
        blog.setCategory("News");
        blog.setReadingTimeMinutes(3);
        blog.setContent(rawContent != null ? rawContent : "No content available.");
        blog.setKeyTakeaways(List.of());
        blog.setTags(List.of("video", "blog"));
        blog.setTableOfContents(List.of());
        blog.setSections(List.of());
        blog.setFaqs(List.of());
        return blog;
    }
}

