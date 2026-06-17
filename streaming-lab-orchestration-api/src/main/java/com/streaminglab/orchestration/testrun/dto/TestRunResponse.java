package com.streaminglab.orchestration.testrun.dto;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Current state and client-facing locations for a streaming test run.")
public record TestRunResponse(
    @Schema(
            description = "Test run identifier.",
            example = "018fd86c-9a7d-7f46-a35b-8d2f7d4c7b21",
            requiredMode = Schema.RequiredMode.REQUIRED)
        UUID testRunId,
    @Schema(
            description = "Stream name used to build playback and publishing URLs.",
            example = "display-wall-smoke-test",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String streamName,
    @Schema(
            description = "Current lifecycle status of the test run.",
            example = "CREATED",
            allowableValues = {
              "CREATED",
              "PREPARING",
              "STREAMING",
              "STOPPING",
              "STOPPED",
              "FAILED"
            },
            requiredMode = Schema.RequiredMode.REQUIRED)
        TestRunStatus status,
    @Schema(
            description = "External HLS playback URL clients can use to view the stream.",
            example = "http://localhost:8888/display-wall-smoke-test/",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String hlsExternalUrl,
    @Schema(
            description = "Relative path where artifacts for this test run are stored.",
            example = "artifacts/test-runs/018fd86c-9a7d-7f46-a35b-8d2f7d4c7b21",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String artifactPath,
    @Schema(
            description = "Timestamp when the test run was created.",
            example = "2026-06-14T18:25:43.511Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,
    @Schema(
            description = "Timestamp when streaming started, when available.",
            example = "2026-06-14T18:27:11.042Z",
            nullable = true)
        Instant startedAt,
    @Schema(
            description = "Timestamp when streaming stopped, when available.",
            example = "2026-06-14T18:42:05.880Z",
            nullable = true)
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
        testRun.finishedAt());
  }
}
