package localnews_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BlogRequest {
    @NotBlank(message = "Video URL is required")
    private String videoUrl;

    @NotBlank(message = "Location is required")
    private String location;

    private List<String> tags;
}