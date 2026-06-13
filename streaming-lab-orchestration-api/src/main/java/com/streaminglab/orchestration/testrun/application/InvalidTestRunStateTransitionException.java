package com.streaminglab.orchestration.testrun.application;

import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.util.List;
import java.util.UUID;

public class InvalidTestRunStateTransitionException extends RuntimeException {
  public InvalidTestRunStateTransitionException(
      UUID testRunId, TestRunStatus currentStatus, List<TestRunStatus> targetStatuses) {
    super(
        "cannot transition test run "
            + testRunId.toString()
            + " to status "
            + targetStatuses
            + " when test run is "
            + currentStatus);
  }
}
