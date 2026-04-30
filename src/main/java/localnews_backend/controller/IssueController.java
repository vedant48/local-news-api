package localnews_backend.controller;

import jakarta.validation.Valid;
import localnews_backend.dto.IssueRequest;
import localnews_backend.model.Issue;
import localnews_backend.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Issue createIssue(@Valid @RequestBody IssueRequest request) {
        return issueService.createIssue(request);
    }

    @GetMapping
    public List<Issue> getAllIssues() {
        return issueService.getAllIssues();
    }
}