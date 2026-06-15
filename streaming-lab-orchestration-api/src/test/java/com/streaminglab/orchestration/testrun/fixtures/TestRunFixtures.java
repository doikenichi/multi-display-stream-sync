package com.streaminglab.orchestration.testrun.fixtures;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.time.Instant;
import java.util.UUID;

public final class TestRunFixtures {

  public static final Instant FIXED_NOW = Instant.parse("2026-06-08T00:00:00Z");

  private TestRunFixtures() {}

  public static TestRun createdTestRun() {
    return testRunWithStatus(TestRunStatus.CREATED);
  }

  public static TestRun preparingTestRun() {
    return testRunWithStatus(TestRunStatus.PREPARING);
  }

  public static TestRun streamingTestRun() {
    return testRunWithStatus(TestRunStatus.STREAMING);
  }

  public static TestRun stoppingTestRun() {
    return testRunWithStatus(TestRunStatus.STOPPING);
  }

  public static TestRun stoppedTestRun() {
    return testRunWithStatus(TestRunStatus.STOPPED);
  }

  public static TestRun failedTestRun() {
    return testRunWithStatus(TestRunStatus.FAILED);
  }

  public static TestRun testRunWithStatus(TestRunStatus status) {
    return new TestRun(
        UUID.randomUUID(),
        "camera_sync_test",
        2,
        status,
        "http://mediamtx:8888/camera_sync_test/",
        "http://localhost:8888/camera_sync_test/",
        "rtsp://mediamtx:8554/camera_sync_test",
        "artifacts/test-runs/test-run-id",
        FIXED_NOW,
        startedAtFor(status),
        finishedAtFor(status),
        errorMessageFor(status));
  }

  private static Instant startedAtFor(TestRunStatus status) {
    return switch (status) {
      case STREAMING, STOPPING, STOPPED, FAILED -> FIXED_NOW;
      case CREATED, PREPARING -> null;
    };
  }

  private static Instant finishedAtFor(TestRunStatus status) {
    return switch (status) {
      case STOPPED, FAILED -> FIXED_NOW;
      case CREATED, PREPARING, STREAMING, STOPPING -> null;
    };
  }

  private static String errorMessageFor(TestRunStatus status) {
    return status == TestRunStatus.FAILED ? "Test run failed" : null;
  }
}
