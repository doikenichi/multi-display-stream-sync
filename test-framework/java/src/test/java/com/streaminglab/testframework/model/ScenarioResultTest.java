package com.streaminglab.testframework.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScenarioResultTest {

  @Test
  void shouldDefensivelyCopyTags() {
    List<String> tags = new ArrayList<>(List.of("@smoke"));

    ScenarioResult result =
        new ScenarioResult(
            "java",
            "cucumber",
            "local",
            UUID.randomUUID(),
            "Playback",
            "Video progresses",
            tags,
            "PASSED",
            Instant.parse("2026-07-02T00:00:00Z"),
            Instant.parse("2026-07-02T00:00:01Z"),
            1000,
            "build/evidence");

    tags.add("@regression");

    assertThat(result.tags()).containsExactly("@smoke").isUnmodifiable();
  }
}
