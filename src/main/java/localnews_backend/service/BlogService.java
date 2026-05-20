package localnews_backend.service;

import localnews_backend.dto.AIGeneratedBlog;
import localnews_backend.dto.BlogRequest;
import localnews_backend.dto.ManualBlogRequest;
import localnews_backend.model.*;
import localnews_backend.repository.BlogRepository;
import localnews_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private final AIService aiService;
    private final UserRepository userRepository;

    public Blog generateBlog(BlogRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AIGeneratedBlog ai = aiService.generateBlogFromVideo(request.getVideoUrl());

        List<String> tags = (request.getTags() != null && !request.getTags().isEmpty())
                ? request.getTags()
                : ai.getTags();

        LocalDateTime now = LocalDateTime.now();

        Blog blog = Blog.builder()
                .title(ai.getTitle())
                .seoTitle(ai.getSeoTitle())
                .seoDescription(ai.getSeoDescription())
                .slug(ai.getSlug())
                .excerpt(ai.getExcerpt())
                .category(ai.getCategory())
                .coverImageUrl(ai.getCoverImageUrl())
                .readingTimeMinutes(ai.getReadingTimeMinutes())
                .content(ai.getContent())
                .tableOfContents(mapTableOfContents(ai.getTableOfContents()))
                .sections(mapSections(ai.getSections()))
                .keyTakeaways(ai.getKeyTakeaways())
                .faqs(mapFaqs(ai.getFaqs()))
                .videoUrl(request.getVideoUrl())
                .location(request.getLocation())
                .tags(tags)
                .authorId(user.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return blogRepository.save(blog);
    }

    public Blog createManualBlog(ManualBlogRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime now = LocalDateTime.now();

        String slug = request.getTitle() != null ? request.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-") : "";

        int readingTime = 1;
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            int wordCount = request.getContent().split("\\s+").length;
            readingTime = Math.max(1, (int) Math.ceil(wordCount / 200.0));
        }

        Blog blog = Blog.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .category(request.getCategory())
                .coverImageUrl(request.getCoverImageUrl())
                .location(request.getLocation())
                .tags(request.getTags())
                .slug(slug)
                .readingTimeMinutes(readingTime)
                .authorId(user.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return blogRepository.save(blog);
    }

    public List<Blog> getAllBlogs() {
        return blogRepository.findAll();
    }

    public Blog getBlogById(String id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog not found: " + id));
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private List<TableOfContentsItem> mapTableOfContents(List<AIGeneratedBlog.TableOfContentsItemDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(d -> TableOfContentsItem.builder()
                        .id(d.getId())
                        .heading(d.getHeading())
                        .level(d.getLevel())
                        .build())
                .collect(Collectors.toList());
    }

    private List<BlogSection> mapSections(List<AIGeneratedBlog.BlogSectionDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(d -> BlogSection.builder()
                        .id(d.getId())
                        .heading(d.getHeading())
                        .level(d.getLevel())
                        .content(d.getContent())
                        .build())
                .collect(Collectors.toList());
    }

    private List<FaqItem> mapFaqs(List<AIGeneratedBlog.FaqItemDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(d -> FaqItem.builder()
                        .question(d.getQuestion())
                        .answer(d.getAnswer())
                        .build())
                .collect(Collectors.toList());
    }
}
