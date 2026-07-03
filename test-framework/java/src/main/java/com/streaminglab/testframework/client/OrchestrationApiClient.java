package com.streaminglab.testframework.client;

import com.streaminglab.testframework.config.TestFrameworkConfig;
import com.streaminglab.testframework.dto.CreateTestRunRequest;
import com.streaminglab.testframework.dto.TestRunResponse;
import java.util.UUID;

/** Client boundary for streaming-lab-orchestration-api. */
public class OrchestrationApiClient {

  private final TestFrameworkConfig.OrchestrationApiConfig config;

  public OrchestrationApiClient(TestFrameworkConfig.OrchestrationApiConfig config) {
    this.config = config;
  }

  public TestRunResponse createTestRun(CreateTestRunRequest request) {
    throw new UnsupportedOperationException("Create test run API call is not implemented yet.");
  }

  public TestRunResponse prepareTestRun(UUID testRunId) {
    throw new UnsupportedOperationException("Prepare test run API call is not implemented yet.");
  }

  public TestRunResponse startStream(UUID testRunId) {
    throw new UnsupportedOperationException("Start stream API call is not implemented yet.");
  }

  public TestRunResponse stopTestRun(UUID testRunId) {
    throw new UnsupportedOperationException("Stop test run API call is not implemented yet.");
  }

  public TestRunResponse failTestRun(UUID testRunId, String reason) {
    throw new UnsupportedOperationException("Fail test run API call is not implemented yet.");
  }

  public TestRunResponse getTestRun(UUID testRunId) {
    throw new UnsupportedOperationException("Get test run API call is not implemented yet.");
  }

  public String baseUrl() {
    return config.baseUrl();
  }
}
