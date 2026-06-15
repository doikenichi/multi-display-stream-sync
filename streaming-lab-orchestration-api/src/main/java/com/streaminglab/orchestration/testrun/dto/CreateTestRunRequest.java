package com.streaminglab.orchestration.testrun.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request payload for creating a streaming test run.")
public record CreateTestRunRequest(
    @Schema(
            description = "Unique stream name used to build HLS and RTSP stream URLs.",
            example = "display-wall-smoke-test",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "streamName is required")
        String streamName,
    @Schema(
            description = "Number of displays participating in the test run.",
            example = "4",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "displayCount is required")
        @Positive(message = "displayCount must be greater than 0")
        Integer displayCount) {}
