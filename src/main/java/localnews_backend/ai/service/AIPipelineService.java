package localnews_backend.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AIPipelineService {

    private final String ML_URL = "http://localhost:8000/ai/generate-report";

    public String generateReport(String url) {

        RestTemplate restTemplate = new RestTemplate();

        // request body
        Map<String, String> request = new HashMap<>();
        request.put("url", url);   

        // call FastAPI
        Map<String, Object> response =
                restTemplate.postForObject(ML_URL, request, Map.class);

        
        if (response == null || response.get("report") == null) {
            throw new RuntimeException("Invalid response from ML service");
        }

        return response.get("report").toString();
    }
}