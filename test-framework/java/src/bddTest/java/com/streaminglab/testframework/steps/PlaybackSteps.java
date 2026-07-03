package com.streaminglab.testframework.steps;

import com.streaminglab.testframework.context.ScenarioContext;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class PlaybackSteps {

  // PicoContainer will provide shared scenario state as these pending steps are implemented.
  @SuppressWarnings("PMD.UnusedPrivateField")
  private final ScenarioContext context;

  public PlaybackSteps(ScenarioContext context) {
    this.context = context;
  }

  @When("the display client is opened for the stream")
  public void theDisplayClientIsOpenedForTheStream() {
    throw new PendingException("Open the Display Client with the resolved HLS stream URL.");
  }

  @Then("the display client should load the video")
  public void theDisplayClientShouldLoadTheVideo() {
    throw new PendingException("Verify that the video element is loaded.");
  }

  @Then("playback time should progress")
  public void playbackTimeShouldProgress() {
    throw new PendingException("Verify that video currentTime progresses from initial to final value.");
  }
}
