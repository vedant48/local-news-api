package localnews_backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "blogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    private String id;

    // ── Core content ──────────────────────────────────────────────────────────

    /** Main article headline */
    private String title;

    /** Full markdown blog content */
    private String content;

    /** Source video URL used for generation */
    private String videoUrl;

    // ── Modern blog metadata ──────────────────────────────────────────────────

    /** URL-friendly identifier (e.g. "breaking-news-on-tech") */
    private String slug;

    /** 2-3 sentence teaser shown in cards/previews */
    private String excerpt;

    /** Hero / cover image URL */
    private String coverImageUrl;

    /** Category (e.g. News, Technology, Sports) */
    private String category;

    /** Estimated reading time in minutes */
    private Integer readingTimeMinutes;

    // ── SEO ───────────────────────────────────────────────────────────────────

    /** SEO-optimised title (≤ 60 chars) */
    private String seoTitle;

    /** Meta description for search engines (≤ 160 chars) */
    private String seoDescription;

    // ── Structured content ────────────────────────────────────────────────────

    /** Anchor links for in-page navigation */
    private List<TableOfContentsItem> tableOfContents;

    /** Ordered list of titled content sections */
    private List<BlogSection> sections;

    /** Bullet-point key takeaways for quick readers */
    private List<String> keyTakeaways;

    /** Frequently asked questions about the topic */
    private List<FaqItem> faqs;

    // ── Taxonomy & authorship ─────────────────────────────────────────────────

    private List<String> tags;
    private String location;

    /** References the User document id */
    private String authorId;

    // ── Timestamps ────────────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}