package com.streaminglab.testframework.client;

import com.streaminglab.testframework.model.PlaybackObservation;
import java.nio.file.Path;

/** Browser/display-client boundary. Playwright Java will be introduced here later. */
public class DisplayClientDriver {

  public void open(String displayClientUrl) {
    throw new UnsupportedOperationException("Display Client browser automation is not implemented yet.");
  }

  public PlaybackObservation capturePlaybackObservation() {
    throw new UnsupportedOperationException("Playback observation capture is not implemented yet.");
  }

  public Path captureScreenshot(Path screenshotPath) {
    throw new UnsupportedOperationException("Screenshot capture is not implemented yet.");
  }
}
