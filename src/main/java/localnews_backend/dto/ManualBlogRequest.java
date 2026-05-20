package localnews_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ManualBlogRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private String excerpt;
    private String category;
    private String coverImageUrl;

    @NotBlank(message = "Location is required")
    private String location;

    private List<String> tags;
}
