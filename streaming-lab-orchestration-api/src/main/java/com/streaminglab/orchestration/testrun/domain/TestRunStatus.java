package com.streaminglab.orchestration.testrun.domain;

public enum TestRunStatus {
  CREATED,
  PREPARING,
  STREAMING,
  STOPPING,
  STOPPED,
  FAILED
}
