package com.streaminglab.orchestration.testrun.application;

import java.util.UUID;

public class TestRunNotFoundException extends RuntimeException {

  public TestRunNotFoundException(UUID testRunId) {
    super("Test run not found: " + testRunId);
  }
}
