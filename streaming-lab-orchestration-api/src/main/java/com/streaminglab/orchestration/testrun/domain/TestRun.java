package com.streaminglab.orchestration.testrun.domain;

import java.time.Instant;

public record TestRun(
    String testRunId,
    String streamName,
    TestRunStatus status,
    String hlsInternalUrl,
    String hlsExternalUrl,
    String rtspPublishUrl,
    String artifactPath,
    Instant createdAt,
    Instant startedAt,
    Instant stoppedAt,
    String errorMessage) {}
