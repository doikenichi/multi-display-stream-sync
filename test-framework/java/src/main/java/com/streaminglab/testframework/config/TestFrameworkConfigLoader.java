package com.streaminglab.testframework.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class TestFrameworkConfigLoader {

  public TestFrameworkConfig loadActiveProfile() {
    String profile = System.getProperty("test.framework.profile", "local");
    return load(profile);
  }

  public TestFrameworkConfig load(String profile) {
    Path configPath =
            Path.of("..", "contract", "examples", "test-framework-" + profile + ".yaml")
                    .toAbsolutePath()
                    .normalize();

    return loadFromPath(configPath);
  }

  public TestFrameworkConfig loadFromPath(Path configPath) {
    Map<String, Object> root = readYaml(configPath);

    Map<String, Object> orchestrationApi = requiredMap(root, "orchestrationApi");
    Map<String, Object> displayClient = requiredMap(root, "displayClient");
    Map<String, Object> streaming = requiredMap(root, "streaming");
    Map<String, Object> browser = requiredMap(root, "browser");
    Map<String, Object> viewport = requiredMap(browser, "viewport");
    Map<String, Object> playback = requiredMap(root, "playback");
    Map<String, Object> evidence = requiredMap(root, "evidence");
    Map<String, Object> reporting = requiredMap(root, "reporting");

    return new TestFrameworkConfig(
            requiredString(root, "profile"),
            new TestFrameworkConfig.OrchestrationApiConfig(
                    requiredString(orchestrationApi, "baseUrl"),
                    requiredInt(orchestrationApi, "timeoutMs")),
            new TestFrameworkConfig.DisplayClientConfig(
                    requiredString(displayClient, "baseUrl"),
                    requiredInt(displayClient, "timeoutMs"),
                    requiredString(displayClient, "displayId")),
            new TestFrameworkConfig.StreamingConfig(
                    requiredString(streaming, "hlsBaseUrl"),
                    requiredString(streaming, "streamName")),
            new TestFrameworkConfig.BrowserConfig(
                    requiredBoolean(browser, "headless"),
                    new TestFrameworkConfig.ViewportConfig(
                            requiredInt(viewport, "width"),
                            requiredInt(viewport, "height"))),
            new TestFrameworkConfig.PlaybackConfig(
                    requiredDouble(playback, "minimumProgressSeconds"),
                    requiredInt(playback, "timeoutMs"),
                    requiredInt(playback, "pollIntervalMs")),
            new TestFrameworkConfig.EvidenceConfig(
                    requiredString(evidence, "outputDir"),
                    requiredBoolean(evidence, "screenshotsEnabled"),
                    requiredBoolean(evidence, "logsEnabled")),
            new TestFrameworkConfig.ReportingConfig(
                    requiredBoolean(reporting, "reportPortalEnabled"),
                    requiredString(reporting, "launchName")));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readYaml(Path configPath) {
    if (!Files.exists(configPath)) {
      throw new IllegalArgumentException("Config file not found: " + configPath);
    }

    Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    try (InputStream inputStream = Files.newInputStream(configPath)) {
      Object loaded = yaml.load(inputStream);

      if (!(loaded instanceof Map<?, ?> loadedMap)) {
        throw new IllegalArgumentException("Config file must contain a YAML object: " + configPath);
      }

      return (Map<String, Object>) loadedMap;
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read config file: " + configPath, exception);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> requiredMap(Map<String, Object> source, String key) {
    Object value = source.get(key);

    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("Missing or invalid object field: " + key);
    }

    return (Map<String, Object>) map;
  }

  private String requiredString(Map<String, Object> source, String key) {
    Object value = source.get(key);

    if (value instanceof String stringValue && !stringValue.isBlank()) {
      return stringValue;
    }

    throw new IllegalArgumentException("Missing or invalid string field: " + key);
  }

  private UUID requiredUuid(Map<String, Object> source, String key) {
    String value = requiredString(source, key);

    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Missing or invalid UUID field: " + key, exception);
    }
  }

  private int requiredInt(Map<String, Object> source, String key) {
    Object value = source.get(key);

    if (value instanceof Number numberValue) {
      return numberValue.intValue();
    }

    throw new IllegalArgumentException("Missing or invalid integer field: " + key);
  }

  private double requiredDouble(Map<String, Object> source, String key) {
    Object value = source.get(key);

    if (value instanceof Number numberValue) {
      return numberValue.doubleValue();
    }

    throw new IllegalArgumentException("Missing or invalid decimal field: " + key);
  }

  private boolean requiredBoolean(Map<String, Object> source, String key) {
    Object value = source.get(key);

    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }

    throw new IllegalArgumentException("Missing or invalid boolean field: " + key);
  }
}
