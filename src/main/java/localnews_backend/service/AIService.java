package localnews_backend.service;

import org.springframework.stereotype.Service;

@Service
public class AIService {

    public String generateTitle(String videoUrl) {
        return "Generated Title from Video";
    }

    public String generateContent(String videoUrl) {
        return "Generated blog content based on video transcript...";
    }
}