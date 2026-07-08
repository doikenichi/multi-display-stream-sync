package com.streaminglab.testframework.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Immutable scenario execution summary before writing run-summary.json. */
public record ScenarioResult(
    String implementation,
    String framework,
    String profile,
    UUID testRunId,
    String feature,
    String scenarioName,
    List<String> tags,
    String status,
    Instant startedAt,
    Instant finishedAt,
    long durationMs,
    String artifactDirectory) {

  public ScenarioResult {
    tags = List.copyOf(tags);
  }
}
