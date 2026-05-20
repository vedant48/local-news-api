package localnews_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import localnews_backend.dto.BlogRequest;
import localnews_backend.dto.ManualBlogRequest;
import localnews_backend.model.Blog;
import localnews_backend.model.User;
import localnews_backend.repository.UserRepository;
import localnews_backend.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final UserRepository userRepository;

    /** Generate a blog post from a video URL (REPORTER role required). */
    @PostMapping("/generate")
    public Blog generateBlog(@Valid @RequestBody BlogRequest request,
                             HttpServletRequest httpRequest) {

        String email = (String) httpRequest.getAttribute("email");

        if (email == null) {
            throw new RuntimeException("Unauthorized");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getRole().equals("REPORTER")) {
            throw new RuntimeException("Access denied");
        }

        return blogService.generateBlog(request, email);
    }

    /** Manually create a blog post (REPORTER role required). */
    @PostMapping
    public Blog createManualBlog(@Valid @RequestBody ManualBlogRequest request,
                                 HttpServletRequest httpRequest) {
        String email = (String) httpRequest.getAttribute("email");

        if (email == null) {
            throw new RuntimeException("Unauthorized");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getRole().equals("REPORTER")) {
            throw new RuntimeException("Access denied");
        }

        return blogService.createManualBlog(request, email);
    }

    /** Return all published blogs (public). */
    @GetMapping
    public List<Blog> getAllBlogs() {
        return blogService.getAllBlogs();
    }

    /** Return a single blog by its id (public). */
    @GetMapping("/{id}")
    public Blog getBlogById(@PathVariable String id) {
        return blogService.getBlogById(id);
    }
}
