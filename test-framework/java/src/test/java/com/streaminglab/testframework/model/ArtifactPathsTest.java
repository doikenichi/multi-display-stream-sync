package com.streaminglab.testframework.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ArtifactPathsTest {
  @Test
  void shouldBeInstantiable() {
    var artifactPaths =
        new ArtifactPaths(
            Path.of("/root"),
            Path.of("/screenshot"),
            Path.of("/raw"),
            Path.of("/browser"),
            Path.of("/playback"),
            Path.of("/summary"),
            Path.of("/log"));
    assertThat(artifactPaths).isNotNull();
  }
}
