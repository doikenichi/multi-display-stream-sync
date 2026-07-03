package com.streaminglab.testframework.dto;

/** Stream metadata returned by the orchestrator when available. */
public record StreamMetadataResponse(String streamId, String hlsUrl) {}
