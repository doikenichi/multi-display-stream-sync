package com.streaminglab.testframework.context;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/** Scenario-scoped mutable state shared across Cucumber step definition classes. */
@SuppressWarnings("PMD.DataClass")
public class ScenarioContext {

  private UUID testRunId;
  private String currentTestRunStatus;
  private String streamName;
  private String hlsStreamUrl;
  private String displayClientUrl;
  private Path artifactDirectory;
  private Instant startedAt;

  public UUID getTestRunId() {
    return testRunId;
  }

  public void setTestRunId(UUID testRunId) {
    this.testRunId = testRunId;
  }

  public String getCurrentTestRunStatus() {
    return currentTestRunStatus;
  }

  public void setCurrentTestRunStatus(String currentTestRunStatus) {
    this.currentTestRunStatus = currentTestRunStatus;
  }

  public String getStreamName() {
    return streamName;
  }

  public void setStreamName(String streamName) {
    this.streamName = streamName;
  }

  public String getHlsStreamUrl() {
    return hlsStreamUrl;
  }

  public void setHlsStreamUrl(String hlsStreamUrl) {
    this.hlsStreamUrl = hlsStreamUrl;
  }

  public String getDisplayClientUrl() {
    return displayClientUrl;
  }

  public void setDisplayClientUrl(String displayClientUrl) {
    this.displayClientUrl = displayClientUrl;
  }

  public Path getArtifactDirectory() {
    return artifactDirectory;
  }

  public void setArtifactDirectory(Path artifactDirectory) {
    this.artifactDirectory = artifactDirectory;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }
}
