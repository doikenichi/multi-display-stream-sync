package com.streaminglab.orchestration.testrun.api;

import com.streaminglab.orchestration.testrun.application.TestRunService;
import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.dto.TestRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-runs")
@Tag(name = "Test Runs", description = "Create, inspect, and transition streaming test runs.")
public class TestRunQueryController {
  private final TestRunService testRunService;

  public TestRunQueryController(TestRunService testRunService) {
    this.testRunService = testRunService;
  }

  @Operation(
      summary = "Get a test run",
      description = "Returns the current persisted state for a test run.")
  @GetMapping("/{testRunId}")
  public ResponseEntity<TestRunResponse> getTestRun(
      @Parameter(description = "Test run identifier.", required = true) @PathVariable
          UUID testRunId) {
    TestRun testRun = this.testRunService.findById(testRunId);
    return ResponseEntity.ok(TestRunResponse.from(testRun));
  }
}
