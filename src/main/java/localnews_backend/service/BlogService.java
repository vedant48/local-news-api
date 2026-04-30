package localnews_backend.service;

import localnews_backend.dto.BlogRequest;
import localnews_backend.model.Blog;
import localnews_backend.model.User;
import localnews_backend.repository.BlogRepository;
import localnews_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private final AIService aiService;
    private final UserRepository userRepository;


    public Blog generateBlog(BlogRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Blog blog = Blog.builder()
                .title(aiService.generateTitle(request.getVideoUrl()))
                .content(aiService.generateContent(request.getVideoUrl()))
                .videoUrl(request.getVideoUrl())
                .location(request.getLocation())
                .tags(request.getTags())
                .authorId(user.getId()) // ✅ correct mapping
                .createdAt(LocalDateTime.now())
                .build();

        return blogRepository.save(blog);
    }

    public List<Blog> getAllBlogs() {
        return blogRepository.findAll();
    }
}