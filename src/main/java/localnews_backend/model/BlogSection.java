package localnews_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogSection {

    private String id;
    private String heading;
    /** "h2" or "h3" */
    private String level;
    /** Markdown-formatted body content for this section */
    private String content;
}
