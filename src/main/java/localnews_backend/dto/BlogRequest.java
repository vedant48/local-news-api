package localnews_backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class BlogRequest {
    private String videoUrl;
    private String location;
    private List<String> tags;
}