package com.streaminglab.testframework.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.streaminglab.testframework.client.OrchestrationApiClient;
import com.streaminglab.testframework.config.TestFrameworkConfig;
import com.streaminglab.testframework.config.TestFrameworkConfigLoader;
import com.streaminglab.testframework.context.ScenarioContext;
import com.streaminglab.testframework.dto.CreateTestRunRequest;
import com.streaminglab.testframework.dto.TestRunResponse;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TestRunSteps {

  private static final int SINGLE_DISPLAY_CLIENT = 1;

  private final ScenarioContext context;
  private final TestFrameworkConfig config;
  private final OrchestrationApiClient orchestrationApiClient;

  public TestRunSteps(ScenarioContext context) {
    this.context = context;
    this.config = new TestFrameworkConfigLoader().loadActiveProfile();
    this.orchestrationApiClient = new OrchestrationApiClient(config.orchestrationApi());
  }

  @Given("a new test run exists")
  public void aNewTestRunExists() {
    CreateTestRunRequest request =
            new CreateTestRunRequest(config.streaming().streamName(), SINGLE_DISPLAY_CLIENT);

    TestRunResponse response = orchestrationApiClient.createTestRun(request);

    assertThat(response.testRunId()).isNotNull();
    assertThat(response.status()).isEqualTo("CREATED");

    context.setTestRunId(response.testRunId());
    context.setCurrentTestRunStatus(response.status());
    context.setStreamName(response.streamName());
    context.setHlsStreamUrl(response.hlsExternalUrl());
  }

  @Given("the test run is prepared")
  public void theTestRunIsPrepared() {
    TestRunResponse response = orchestrationApiClient.prepareTestRun(context.getTestRunId());
    assertThat(response.testRunId()).isNotNull();
    assertThat(response.status()).isEqualTo("PREPARING");
    assertThat(response.streamName()).isEqualTo(context.getStreamName());

    context.setCurrentTestRunStatus(response.status());
  }

  @When("the stream is started")
  public void theStreamIsStarted() {
    TestRunResponse response = orchestrationApiClient.startStream(context.getTestRunId());
    assertThat(response.testRunId()).isNotNull();
    assertThat(response.status()).isEqualTo("STREAMING");
    assertThat(response.streamName()).isEqualTo(context.getStreamName());

    context.setCurrentTestRunStatus(response.status());
  }

  @When("the test run is stopped")
  public void theTestRunIsStopped() {
    TestRunResponse response = orchestrationApiClient.stopTestRun(context.getTestRunId());
    assertThat(response.testRunId()).isNotNull();
    assertThat(response.status()).isEqualTo("STOPPING");
    assertThat(response.streamName()).isEqualTo(context.getStreamName());

    context.setCurrentTestRunStatus(response.status());

  }

  @Then("the test run should be stopped")
  public void theTestRunShouldBeStopped() {
    TestRunResponse response = orchestrationApiClient.getTestRun(context.getTestRunId());
    assertThat(response.testRunId()).isNotNull();
    assertThat(response.status()).isEqualTo("STOPPED");
    assertThat(response.streamName()).isEqualTo(context.getStreamName());

    context.setCurrentTestRunStatus(response.status());

  }
}
