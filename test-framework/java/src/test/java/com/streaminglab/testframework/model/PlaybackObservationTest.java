package com.streaminglab.testframework.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PlaybackObservationTest {
  @Test
  void shouldBeInstantiable() {
    PlaybackObservation playbackObservation = new PlaybackObservation(true, 1, 1, 1.0, 1.0, true);
    assertThat(playbackObservation).isNotNull();
  }
}
