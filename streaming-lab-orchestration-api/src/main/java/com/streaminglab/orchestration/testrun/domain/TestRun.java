package com.streaminglab.orchestration.testrun.domain;

import java.time.Instant;
import java.util.UUID;

public record TestRun(
    UUID testRunId,
    String streamName,
    int displayCount,
    TestRunStatus status,
    String hlsInternalUrl,
    String hlsExternalUrl,
    String rtspPublishUrl,
    String artifactPath,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    String errorMessage) {
  public TestRun prepare() {
    return new TestRun(
        testRunId,
        streamName,
        displayCount,
        TestRunStatus.PREPARING,
        hlsInternalUrl,
        hlsExternalUrl,
        rtspPublishUrl,
        artifactPath,
        createdAt,
        startedAt,
        finishedAt,
        errorMessage);
  }

  public TestRun start(Instant now) {
    return new TestRun(
        testRunId,
        streamName,
        displayCount,
        TestRunStatus.STREAMING,
        hlsInternalUrl,
        hlsExternalUrl,
        rtspPublishUrl,
        artifactPath,
        createdAt,
        now,
        finishedAt,
        errorMessage);
  }

  public TestRun requestStop() {
    return new TestRun(
        testRunId,
        streamName,
        displayCount,
        TestRunStatus.STOPPING,
        hlsInternalUrl,
        hlsExternalUrl,
        rtspPublishUrl,
        artifactPath,
        createdAt,
        startedAt,
        finishedAt,
        errorMessage);
  }

  public TestRun markStopped(Instant now) {
    return new TestRun(
        testRunId,
        streamName,
        displayCount,
        TestRunStatus.STOPPED,
        hlsInternalUrl,
        hlsExternalUrl,
        rtspPublishUrl,
        artifactPath,
        createdAt,
        startedAt,
        now,
        errorMessage);
  }

  public TestRun fail(Instant now, String errorMessage) {
    return new TestRun(
        testRunId,
        streamName,
        displayCount,
        TestRunStatus.FAILED,
        hlsInternalUrl,
        hlsExternalUrl,
        rtspPublishUrl,
        artifactPath,
        createdAt,
        startedAt,
        now,
        errorMessage);
  }
}
