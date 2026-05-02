package localnews_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import localnews_backend.dto.AIGeneratedBlog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calls the Google Gemini 1.5 Pro REST API to convert a video URL into a
 * production-grade, structured blog post.
 *
 * <p>Configure {@code gemini.api.key} in {@code application.properties} (or the
 * {@code GEMINI_API_KEY} environment variable) to enable live generation.
 * When no key is present the service returns a safe placeholder so the rest of
 * the application still functions during local development.
 */
@Service
@Slf4j
public class AIService {

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";

    private static final String YOUTUBE_ID_REGEX = "[A-Za-z0-9_-]{11}";

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AIService(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a fully structured blog from the supplied video URL.
     * Falls back to a safe placeholder when the Gemini API key is missing or
     * the upstream call fails.
     */
    public AIGeneratedBlog generateBlogFromVideo(String videoUrl) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("gemini.api.key is not set – returning placeholder blog for: {}", videoUrl);
            return buildPlaceholderBlog(videoUrl);
        }

        try {
            Map<String, Object> requestBody = buildGeminiRequest(videoUrl);

            String rawResponse = restClient.post()
                    .uri(GEMINI_ENDPOINT)
                    .header("x-goog-api-key", geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseGeminiResponse(rawResponse, videoUrl);
        } catch (Exception e) {
            log.error("Gemini blog generation failed for URL '{}': {}", videoUrl, e.getMessage(), e);
            return buildPlaceholderBlog(videoUrl);
        }
    }

    // ── Gemini request building ───────────────────────────────────────────────

    private Map<String, Object> buildGeminiRequest(String videoUrl) {
        List<Map<String, Object>> parts = new ArrayList<>();

        // For YouTube URLs Gemini can stream the video directly via fileData.
        Optional<String> youtubeId = extractYoutubeId(videoUrl);
        if (youtubeId.isPresent()) {
            Map<String, Object> fileData = new LinkedHashMap<>();
            fileData.put("fileUri", videoUrl);
            fileData.put("mimeType", "video/*");

            Map<String, Object> filePart = new LinkedHashMap<>();
            filePart.put("fileData", fileData);
            parts.add(filePart);
        }

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", buildPrompt(videoUrl, youtubeId));
        parts.add(textPart);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", parts);

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 8192);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("contents", List.of(content));
        request.put("generationConfig", generationConfig);

        return request;
    }

    private String buildPrompt(String videoUrl, Optional<String> youtubeId) {
        String videoInstruction = youtubeId.isPresent()
                ? "Thoroughly analyze the provided YouTube video."
                : "Create a blog post about the video available at: " + videoUrl;

        String coverImageEntry = youtubeId
                .map(id -> "\"coverImageUrl\": \"https://img.youtube.com/vi/" + id + "/maxresdefault.jpg\"")
                .orElse("\"coverImageUrl\": null");

        return """
                You are an expert journalist and content strategist. %s

                Return ONLY a single valid JSON object – no markdown fences, no extra text – matching this exact schema:
                {
                  "title": "Engaging SEO-optimized headline (max 70 characters)",
                  "seoTitle": "SEO title for <title> tag (max 60 characters)",
                  "seoDescription": "Meta description for search engines (max 160 characters)",
                  "slug": "url-friendly-hyphenated-slug-from-title",
                  "excerpt": "2-3 sentence compelling teaser that hooks the reader",
                  "category": "one of: News | Technology | Business | Sports | Entertainment | Politics | Health | Science | Education | Lifestyle",
                  %s,
                  "readingTimeMinutes": <integer>,
                  "tableOfContents": [
                    { "id": "section-slug", "heading": "Section Heading", "level": "h2" }
                  ],
                  "sections": [
                    {
                      "id": "section-slug",
                      "heading": "Section Heading",
                      "level": "h2",
                      "content": "Detailed **markdown** content with proper formatting, statistics, analysis and examples (minimum 150 words per section)."
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
                  "content": "Complete polished markdown blog post (minimum 800 words, include all sections with headings, lists, bold text, and a strong conclusion)"
                }

                Requirements:
                - Professional, engaging journalistic tone
                - At least 5 detailed sections (each ≥ 150 words)
                - Markdown formatting throughout (headers, bold, italic, bullet lists)
                - Include real context, statistics, and analysis where available
                - 4–6 FAQ items covering likely reader questions
                - All JSON strings must be properly escaped (no raw newlines inside JSON values)
                """.formatted(videoInstruction, coverImageEntry);
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private AIGeneratedBlog parseGeminiResponse(String rawResponse, String videoUrl) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);

        String jsonText = root
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();

        AIGeneratedBlog blog = objectMapper.readValue(jsonText, AIGeneratedBlog.class);

        // Backfill YouTube thumbnail when Gemini omits it
        if (blog.getCoverImageUrl() == null || blog.getCoverImageUrl().isBlank()) {
            extractYoutubeId(videoUrl).ifPresent(id ->
                    blog.setCoverImageUrl("https://img.youtube.com/vi/" + id + "/maxresdefault.jpg")
            );
        }

        return blog;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

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
            if (candidate.matches(YOUTUBE_ID_REGEX)) return Optional.of(candidate);
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
                if (candidate.matches(YOUTUBE_ID_REGEX)) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    // ── Placeholder fallback ──────────────────────────────────────────────────

    private AIGeneratedBlog buildPlaceholderBlog(String videoUrl) {
        AIGeneratedBlog blog = new AIGeneratedBlog();
        blog.setTitle("Video Blog Post");
        blog.setSeoTitle("Video Blog Post");
        blog.setSeoDescription("A blog post generated from a video.");
        blog.setSlug("video-blog-post");
        blog.setExcerpt("This is a placeholder. Set the `gemini.api.key` property to enable AI-powered blog generation.");
        blog.setCategory("News");
        blog.setReadingTimeMinutes(3);
        blog.setContent("""
                # Video Blog Post

                This is a placeholder blog post. Configure the `gemini.api.key` application property \
                (or the `GEMINI_API_KEY` environment variable) to enable AI-powered generation from \
                video URLs.
                """);
        blog.setKeyTakeaways(List.of("Configure `gemini.api.key` for full AI functionality"));
        blog.setTags(List.of("video", "blog"));
        blog.setTableOfContents(List.of());
        blog.setSections(List.of());
        blog.setFaqs(List.of());
        extractYoutubeId(videoUrl).ifPresent(id ->
                blog.setCoverImageUrl("https://img.youtube.com/vi/" + id + "/maxresdefault.jpg")
        );
        return blog;
    }
}

