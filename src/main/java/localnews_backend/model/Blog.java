package localnews_backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "blogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    private String id;

    private String title;
    private String content;
    private String videoUrl;

    private List<String> tags;
    private String location;

    private String authorId;

    private LocalDateTime createdAt;
}