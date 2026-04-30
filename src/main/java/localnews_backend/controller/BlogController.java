package localnews_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import localnews_backend.dto.BlogRequest;
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
@CrossOrigin
public class BlogController {

    private final BlogService blogService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    public Blog generateBlog(@RequestBody BlogRequest request,
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

    @GetMapping
    public List<Blog> getAllBlogs() {
        return blogService.getAllBlogs();
    }
}