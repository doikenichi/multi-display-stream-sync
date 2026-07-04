package com.streaminglab.testframework.dto;

import java.util.UUID;

/** Response DTO returned by streaming-lab-orchestration-api. */
public record TestRunResponse(
    UUID testRunId, String streamName, String status, String hlsExternalUrl, String artifactPath) {}
