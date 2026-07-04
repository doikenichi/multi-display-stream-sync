package com.streaminglab.testframework.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestFrameworkConfigLoaderTest {

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
    assertThat(config.streaming().streamName()).isEqualTo(UUID.fromString("camera_sync_test"));

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
}
