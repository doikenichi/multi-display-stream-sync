package com.streaminglab.orchestration.testrun.application;

import java.util.UUID;

public class TestRunNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public TestRunNotFoundException(UUID testRunId) {
    super("Test run not found: " + testRunId);
  }
}
