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
    Instant stoppedAt,
    String errorMessage) {}
