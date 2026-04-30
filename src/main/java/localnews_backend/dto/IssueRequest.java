package localnews_backend.dto;

import lombok.Data;

@Data
public class IssueRequest {
    private String title;
    private String description;
    private String location;
    private String imageUrl;
}