package com.streaminglab.orchestration.testrun.application;

import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.util.List;
import java.util.UUID;

public class InvalidTestRunStateTransitionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidTestRunStateTransitionException(
      UUID testRunId,
      TestRunStatus currentStatus,
      TestRunStatus targetStatus,
      List<TestRunStatus> targetStatuses) {
    super(
        "cannot transition test run "
            + testRunId.toString()
            + " with status "
            + currentStatus
            + " to status "
            + targetStatus
            + ". allowed statuses are "
            + targetStatuses);
  }
}
