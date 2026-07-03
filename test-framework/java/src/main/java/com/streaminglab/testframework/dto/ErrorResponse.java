package com.streaminglab.testframework.dto;

/** Error DTO returned by streaming-lab-orchestration-api when a request fails. */
public record ErrorResponse(String errorMessage) {}
