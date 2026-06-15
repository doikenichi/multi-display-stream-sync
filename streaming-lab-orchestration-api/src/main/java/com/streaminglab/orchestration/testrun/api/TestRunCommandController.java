package com.streaminglab.orchestration.testrun.api;

import com.streaminglab.orchestration.testrun.application.TestRunService;
import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.dto.CreateTestRunRequest;
import com.streaminglab.orchestration.testrun.dto.TestRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-runs")
@Tag(name = "Test Runs", description = "Create, inspect, and transition streaming test runs.")
public class TestRunCommandController {

  private final TestRunService testRunService;

  public TestRunCommandController(TestRunService testRunService) {
    this.testRunService = testRunService;
  }

  @Operation(
      summary = "Create a test run",
      description =
          "Creates a test run in CREATED status and returns generated stream playback and artifact"
              + " locations.")
  @PostMapping
  public ResponseEntity<TestRunResponse> createTestRun(
      @Valid @RequestBody CreateTestRunRequest request) {
    TestRun testRun = testRunService.createTestRun(request.streamName(), request.displayCount());

    return ResponseEntity.status(201).body(TestRunResponse.from(testRun));
  }

  @Operation(
      summary = "Prepare a test run",
      description =
          "Transitions a test run to PREPARING. Allowed source statuses are CREATED, FAILED, and"
              + " STOPPED.")
  @PostMapping("/{testRunId}/prepare")
  public ResponseEntity<TestRunResponse> prepareTestRun(
      @Parameter(description = "Test run identifier.", required = true) @PathVariable
          UUID testRunId) {
    TestRun testRun = testRunService.prepareRun(testRunId);

    return ResponseEntity.ok(TestRunResponse.from(testRun));
  }

  @Operation(
      summary = "Start streaming a test run",
      description = "Transitions a PREPARING test run to STREAMING.")
  @PostMapping("/{testRunId}/stream")
  public ResponseEntity<TestRunResponse> streamTestRun(
      @Parameter(description = "Test run identifier.", required = true) @PathVariable
          UUID testRunId) {
    TestRun testRun = testRunService.startStreaming(testRunId);
    return ResponseEntity.ok(TestRunResponse.from(testRun));
  }
}
