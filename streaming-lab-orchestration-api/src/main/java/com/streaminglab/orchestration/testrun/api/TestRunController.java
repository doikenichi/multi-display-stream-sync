package com.streaminglab.orchestration.testrun.api;

import com.streaminglab.orchestration.testrun.application.InvalidTestRunStateTransitionException;
import com.streaminglab.orchestration.testrun.application.TestRunNotFoundException;
import com.streaminglab.orchestration.testrun.application.TestRunService;
import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.dto.CreateTestRunRequest;
import com.streaminglab.orchestration.testrun.dto.TestRunResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-runs")
public class TestRunController {

  private final TestRunService testRunService;

  public TestRunController(TestRunService testRunService) {
    this.testRunService = testRunService;
  }

  @PostMapping
  public ResponseEntity<TestRunResponse> createTestRun(
      @Valid @RequestBody CreateTestRunRequest request) {
    TestRun testRun = testRunService.createTestRun(request.streamName(), request.displayCount());

    return ResponseEntity.status(201).body(TestRunResponse.from(testRun));
  }

  @GetMapping("/{testRunId}")
  public ResponseEntity<TestRunResponse> getTestRun(@PathVariable UUID testRunId) {
    return testRunService
        .findById(testRunId)
        .map(TestRunResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/{testRunId}/prepare")
  public ResponseEntity<TestRunResponse> prepareTestRun(@PathVariable UUID testRunId) {
    TestRun testRun = testRunService.prepareRun(testRunId);

    return ResponseEntity.ok(TestRunResponse.from(testRun));
  }

  @PostMapping("/{testRunId}/stream")
  public ResponseEntity<TestRunResponse> streamTestRun(@PathVariable UUID testRunId) {
    try {
      TestRun testRun = testRunService.startStreaming(testRunId);
      return ResponseEntity.ok(TestRunResponse.from(testRun));
    } catch (InvalidTestRunStateTransitionException e) {
      TestRunResponse body = new TestRunResponse(testRunId, null, null, null, null, null, null, null, e.getMessage());
      return ResponseEntity.status(409).body(body);
    } catch (TestRunNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
