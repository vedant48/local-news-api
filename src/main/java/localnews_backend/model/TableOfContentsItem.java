package localnews_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableOfContentsItem {

    /** Matches the BlogSection id for anchor navigation */
    private String id;
    private String heading;
    /** "h2" or "h3" */
    private String level;
}
