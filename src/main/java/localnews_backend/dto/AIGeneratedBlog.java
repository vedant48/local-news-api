package localnews_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Structured blog data returned by the Gemini AI model.
 * Matches the JSON schema requested in the generation prompt.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIGeneratedBlog {

    private String title;

    private String seoTitle;

    private String seoDescription;

    /** URL-friendly slug (e.g. "breaking-news-on-tech-layoffs") */
    private String slug;

    /** 2-3 sentence hook / teaser shown in cards and previews */
    private String excerpt;

    private String category;

    /** Thumbnail / hero image URL */
    private String coverImageUrl;

    /** Estimated reading time in minutes */
    private Integer readingTimeMinutes;

    /** Full markdown content of the blog post */
    private String content;

    private List<TableOfContentsItemDto> tableOfContents;

    private List<BlogSectionDto> sections;

    private List<String> keyTakeaways;

    private List<FaqItemDto> faqs;

    private List<String> tags;

    // ── Embedded DTOs ──────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TableOfContentsItemDto {
        private String id;
        private String heading;
        private String level;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlogSectionDto {
        private String id;
        private String heading;
        private String level;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaqItemDto {
        private String question;
        private String answer;
    }
}
