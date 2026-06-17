package com.streaminglab.orchestration.testrun.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for failing a streaming test run.")
public record FailTestRunRequest(
    @Schema(
            description = "Error message explaining why the test run failed.",
            example = "The test run failed due to an unexpected error.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "errorMessage is required")
        String errorMessage) {}
