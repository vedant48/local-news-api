package localnews_backend.service;

import localnews_backend.dto.IssueRequest;
import localnews_backend.model.Issue;
import localnews_backend.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;

    public Issue createIssue(IssueRequest request) {
        Issue issue = Issue.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .imageUrl(request.getImageUrl())
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .build();

        return issueRepository.save(issue);
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAll();
    }
}