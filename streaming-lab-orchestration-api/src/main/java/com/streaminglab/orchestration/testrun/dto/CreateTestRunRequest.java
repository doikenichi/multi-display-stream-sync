package com.streaminglab.orchestration.testrun.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTestRunRequest(
    @NotBlank(message = "streamName is required") String streamName) {}
