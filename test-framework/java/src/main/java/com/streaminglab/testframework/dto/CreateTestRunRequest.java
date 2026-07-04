package com.streaminglab.testframework.dto;

/** Request DTO for creating a test run through streaming-lab-orchestration-api. */
public record CreateTestRunRequest(
        String streamName,
        Integer displayCount
) {}