package com.streaminglab.testframework.config;

public record TestFrameworkConfig(
        String profile,
        OrchestrationApiConfig orchestrationApi,
        DisplayClientConfig displayClient,
        StreamingConfig streaming,
        BrowserConfig browser,
        PlaybackConfig playback,
        EvidenceConfig evidence,
        ReportingConfig reporting) {

  public record OrchestrationApiConfig(String baseUrl, int timeoutMs) {}

  public record DisplayClientConfig(String baseUrl, int timeoutMs) {}

  public record StreamingConfig(String hlsBaseUrl, String streamId) {}

  public record BrowserConfig(boolean headless, ViewportConfig viewport) {}

  public record ViewportConfig(int width, int height) {}

  public record PlaybackConfig(
          double minimumProgressSeconds,
          int timeoutMs,
          int pollIntervalMs) {}

  public record EvidenceConfig(
          String outputDir,
          boolean screenshotsEnabled,
          boolean logsEnabled) {}

  public record ReportingConfig(
          boolean reportPortalEnabled,
          String launchName) {}
}
