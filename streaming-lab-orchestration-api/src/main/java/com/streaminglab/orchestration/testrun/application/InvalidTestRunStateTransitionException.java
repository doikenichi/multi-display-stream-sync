package com.streaminglab.orchestration.testrun.application;

import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.util.UUID;

public class InvalidTestRunStateTransitionException extends RuntimeException {
  public InvalidTestRunStateTransitionException(
      UUID testRunId,
      TestRunStatus currentStatus,
      TestRunStatus targetStatus) {
    super(
        "cannot transition test run "
            + testRunId.toString()
            + " to status "
            + targetStatus
            + " when test run is "
            + currentStatus);
  }
}
