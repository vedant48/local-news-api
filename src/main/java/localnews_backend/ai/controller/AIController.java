package localnews_backend.ai.controller;

import localnews_backend.ai.service.AIPipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AIPipelineService pipelineService;

    @PostMapping("/generate-report")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {

        // Validation
        if (body.get("url") == null || body.get("url").isEmpty()) {
            throw new RuntimeException("URL is required");
        }

        String report = pipelineService.generateReport(body.get("url"));

        // response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("report", report);

        return response;
    }
}