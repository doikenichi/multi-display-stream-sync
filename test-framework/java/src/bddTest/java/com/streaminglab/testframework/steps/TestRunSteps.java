package com.streaminglab.testframework.steps;

import com.streaminglab.testframework.context.ScenarioContext;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TestRunSteps {

  // PicoContainer will provide shared scenario state as these pending steps are implemented.
  @SuppressWarnings("PMD.UnusedPrivateField")
  private final ScenarioContext context;

  public TestRunSteps(ScenarioContext context) {
    this.context = context;
  }

  @Given("a new test run exists")
  public void aNewTestRunExists() {
    throw new PendingException("Create test run through streaming-lab-orchestration-api.");
  }

  @Given("the test run is prepared")
  public void theTestRunIsPrepared() {
    throw new PendingException("Prepare test run through streaming-lab-orchestration-api.");
  }

  @When("the stream is started")
  public void theStreamIsStarted() {
    throw new PendingException("Start stream through streaming-lab-orchestration-api.");
  }

  @When("the test run is stopped")
  public void theTestRunIsStopped() {
    throw new PendingException("Stop test run through streaming-lab-orchestration-api.");
  }

  @Then("the test run should be stopped")
  public void theTestRunShouldBeStopped() {
    throw new PendingException("Verify STOPPED status through the orchestrator response or GET endpoint.");
  }
}
