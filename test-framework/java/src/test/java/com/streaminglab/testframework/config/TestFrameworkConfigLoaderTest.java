package com.streaminglab.testframework.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestFrameworkConfigLoaderTest {

  private static final String LOCAL_CONFIG =
      """
      profile: local

      orchestrationApi:
        baseUrl: http://localhost:8080
        timeoutMs: 5000

      displayClient:
        baseUrl: http://localhost:3000
        timeoutMs: 15000
        displayId: DISPLAY_01

      streaming:
        hlsBaseUrl: http://localhost:8888
        streamName: camera_sync_test

      browser:
        headless: true
        viewport:
          width: 1280
          height: 720

      playback:
        minimumProgressSeconds: 2
        timeoutMs: 30000
        pollIntervalMs: 500

      evidence:
        outputDir: build/evidence
        screenshotsEnabled: true
        logsEnabled: true

      reporting:
        reportPortalEnabled: false
        launchName: streaming-lab-contract-local
      """;

  @TempDir private Path tempDirectory;

  @Test
  void shouldLoadLocalProfileConfig() {
    TestFrameworkConfig config = new TestFrameworkConfigLoader().load("local");

    assertThat(config.profile()).isEqualTo("local");

    assertThat(config.orchestrationApi().baseUrl()).isEqualTo("http://localhost:8080");
    assertThat(config.orchestrationApi().timeoutMs()).isGreaterThan(0);

    assertThat(config.displayClient().baseUrl()).isEqualTo("http://localhost:3000");
    assertThat(config.displayClient().timeoutMs()).isGreaterThan(0);
    assertThat(config.displayClient().displayId()).isEqualTo("DISPLAY_01");

    assertThat(config.streaming().hlsBaseUrl()).isEqualTo("http://localhost:8888");
    assertThat(config.streaming().streamName()).isEqualTo("camera_sync_test");

    assertThat(config.browser().headless()).isTrue();
    assertThat(config.browser().viewport().width()).isEqualTo(1280);
    assertThat(config.browser().viewport().height()).isEqualTo(720);

    assertThat(config.playback().minimumProgressSeconds()).isGreaterThan(0);
    assertThat(config.playback().timeoutMs()).isGreaterThan(0);
    assertThat(config.playback().pollIntervalMs()).isGreaterThan(0);

    assertThat(config.evidence().outputDir()).isNotBlank();
    assertThat(config.evidence().screenshotsEnabled()).isTrue();
    assertThat(config.evidence().logsEnabled()).isTrue();

    assertThat(config.reporting().reportPortalEnabled()).isFalse();
    assertThat(config.reporting().launchName()).isEqualTo("streaming-lab-contract-local");
  }

  @Test
  void shouldLoadProfileConfigFromParentContractDirectory() throws IOException {
    Path javaDirectory = tempDirectory.resolve("java");
    writeConfig(tempDirectory.resolve("contract/examples/test-framework-local.yaml"));

    TestFrameworkConfig config = loadFromWorkingDirectory(javaDirectory, "local");

    assertThat(config.profile()).isEqualTo("local");
    assertThat(config.displayClient().displayId()).isEqualTo("DISPLAY_01");
  }

  @Test
  void shouldLoadProfileConfigFromRepositoryParentDirectory() throws IOException {
    Path repositoryDirectory = tempDirectory.resolve("repository");
    Path javaDirectory = repositoryDirectory.resolve("test-framework/java");
    writeConfig(
        repositoryDirectory.resolve("test-framework/contract/examples/test-framework-local.yaml"));

    TestFrameworkConfig config = loadFromWorkingDirectory(javaDirectory, "local");

    assertThat(config.profile()).isEqualTo("local");
    assertThat(config.streaming().streamName()).isEqualTo("camera_sync_test");
  }

  @Test
  void shouldRejectMissingProfileConfig() {
    assertThatThrownBy(
            () ->
                new TestFrameworkConfigLoader().loadFromPath(tempDirectory.resolve("missing.yaml")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Config file not found");
  }

  @Test
  void shouldRejectInvalidProfileConfig() throws IOException {
    Path configPath = tempDirectory.resolve("invalid.yaml");
    Files.writeString(configPath, "- not\n- an\n- object\n");

    assertThatThrownBy(() -> new TestFrameworkConfigLoader().loadFromPath(configPath))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Config file must contain a YAML object");
  }

  private void writeConfig(Path configPath) throws IOException {
    Files.createDirectories(configPath.getParent());
    Files.writeString(configPath, LOCAL_CONFIG);
  }

  private TestFrameworkConfig loadFromWorkingDirectory(Path workingDirectory, String profile)
      throws IOException {
    Files.createDirectories(workingDirectory);
    String originalWorkingDirectory = System.getProperty("user.dir");

    try {
      System.setProperty("user.dir", workingDirectory.toString());
      return new TestFrameworkConfigLoader().load(profile);
    } finally {
      System.setProperty("user.dir", originalWorkingDirectory);
    }
  }
}
