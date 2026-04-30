package localnews_backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {

    @Id
    private String id;

    private String title;
    private String description;
    private String location;
    private String imageUrl;

    private String status; // OPEN / COVERED

    private LocalDateTime createdAt;
}