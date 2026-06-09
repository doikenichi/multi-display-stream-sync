package com.streaminglab.orchestration.testrun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTestRunRequest(
    @NotBlank(message = "streamName is required") String streamName,
    @NotNull(message = "displayCount is required")
        @Positive(message = "displayCount must be greater than 0")
        Integer displayCount) {}
