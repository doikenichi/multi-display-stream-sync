package com.streaminglab.orchestration.testrun.dto;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.time.Instant;
import java.util.UUID;

public record TestRunResponse(
    UUID testRunId,
    String streamName,
    TestRunStatus status,
    String hlsExternalUrl,
    String artifactPath,
    Instant createdAt,
    Instant startedAt,
    Instant stoppedAt) {

  public static TestRunResponse from(TestRun testRun) {
    return new TestRunResponse(
        testRun.testRunId(),
        testRun.streamName(),
        testRun.status(),
        testRun.hlsExternalUrl(),
        testRun.artifactPath(),
        testRun.createdAt(),
        testRun.startedAt(),
        testRun.stoppedAt());
  }
}
