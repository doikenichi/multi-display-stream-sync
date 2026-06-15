package com.streaminglab.orchestration.testrun.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response returned when a test-run operation cannot be completed.")
public record ApiErrorResponse(
    @Schema(
            description = "Human-readable error message.",
            example = "Test run 018fd86c-9a7d-7f46-a35b-8d2f7d4c7b21 was not found.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String errorMessage) {}
