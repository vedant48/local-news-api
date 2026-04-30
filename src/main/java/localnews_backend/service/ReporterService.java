package localnews_backend.service;

import localnews_backend.model.User;
import localnews_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporterService {

    private final UserRepository userRepository;

    public List<User> getAllReporters() {
        return userRepository.findByRole("REPORTER");
    }

    public User getReporterById(String id) {
        return userRepository.findById(id)
                .filter(u -> "REPORTER".equals(u.getRole()))
                .orElseThrow(() -> new RuntimeException("Reporter not found"));
    }

    public User getReporterByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> "REPORTER".equals(u.getRole()))
                .orElseThrow(() -> new RuntimeException("Reporter not found"));
    }
}
