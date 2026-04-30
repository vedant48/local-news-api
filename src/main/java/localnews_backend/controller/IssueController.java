package localnews_backend.controller;

import localnews_backend.dto.IssueRequest;
import localnews_backend.model.Issue;
import localnews_backend.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
@CrossOrigin
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public Issue createIssue(@RequestBody IssueRequest request) {
        return issueService.createIssue(request);
    }

    @GetMapping
    public List<Issue> getAllIssues() {
        return issueService.getAllIssues();
    }
}