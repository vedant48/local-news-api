package localnews_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import localnews_backend.model.User;
import localnews_backend.service.ReporterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reporters")
@RequiredArgsConstructor
public class ReporterController {

    private final ReporterService reporterService;

    @GetMapping
    public List<User> getAllReporters() {
        return reporterService.getAllReporters();
    }

    @GetMapping("/{id}")
    public User getReporterById(@PathVariable String id) {
        return reporterService.getReporterById(id);
    }

    @GetMapping("/me")
    public User getMyProfile(HttpServletRequest request) {
        String email = (String) request.getAttribute("email");
        if (email == null) {
            throw new RuntimeException("Unauthorized");
        }
        return reporterService.getReporterByEmail(email);
    }
}
