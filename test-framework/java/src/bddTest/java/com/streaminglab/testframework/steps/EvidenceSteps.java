package com.streaminglab.testframework.steps;

import com.streaminglab.testframework.context.ScenarioContext;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Then;

public class EvidenceSteps {

  // PicoContainer will provide shared scenario state as these pending steps are implemented.
  @SuppressWarnings("PMD.UnusedPrivateField")
  private final ScenarioContext context;

  public EvidenceSteps(ScenarioContext context) {
    this.context = context;
  }

  @Then("playback evidence should be written")
  public void playbackEvidenceShouldBeWritten() {
    throw new PendingException("Write playback-status.json using the shared evidence schema.");
  }

  @Then("a playback screenshot should be captured")
  public void aPlaybackScreenshotShouldBeCaptured() {
    throw new PendingException("Capture and store a playback screenshot under the artifact directory.");
  }

  @Then("all required artifacts should contain the test run id")
  public void allRequiredArtifactsShouldContainTheTestRunId() {
    throw new PendingException("Verify testRunId appears consistently in required artifacts.");
  }
}
