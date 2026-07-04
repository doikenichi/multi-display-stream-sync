package com.streaminglab.testframework.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.streaminglab.testframework.client.DisplayClientDriver;
import com.streaminglab.testframework.config.TestFrameworkConfig;
import com.streaminglab.testframework.config.TestFrameworkConfigLoader;
import com.streaminglab.testframework.context.ScenarioContext;
import io.cucumber.java.After;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PlaybackSteps {

  private final ScenarioContext context;
  private final TestFrameworkConfig config;
  private final DisplayClientDriver displayClientDriver;

  public PlaybackSteps(ScenarioContext context) {
    this.context = context;
    this.config = new TestFrameworkConfigLoader().loadActiveProfile();
    this.displayClientDriver =
            new DisplayClientDriver(config.browser(), config.displayClient());
  }

  @When("the display client is opened for the stream")
  public void theDisplayClientIsOpenedForTheStream() {
    assertThat(context.getTestRunId())
            .as("testRunId must exist before opening the Display Client")
            .isNotNull();

    String hlsStreamUrl = resolveHlsStreamUrl();
    String displayClientUrl = buildDisplayClientUrl(hlsStreamUrl);

    displayClientDriver.open(displayClientUrl);

    context.setDisplayClientUrl(displayClientUrl);
    context.setHlsStreamUrl(hlsStreamUrl);
  }

  @Then("the display client should load the video")
  public void theDisplayClientShouldLoadTheVideo() {
    throw new PendingException("Verify that the video element is loaded.");
  }

  @Then("playback time should progress")
  public void playbackTimeShouldProgress() {
    throw new PendingException("Verify that video currentTime progresses from initial to final value.");
  }

  @After
  public void closeBrowser() {
    displayClientDriver.close();
  }

  private String resolveHlsStreamUrl() {
    if (context.getHlsStreamUrl() != null && !context.getHlsStreamUrl().isBlank()) {
      return context.getHlsStreamUrl();
    }

    return stripTrailingSlash(config.streaming().hlsBaseUrl())
            + "/"
            + config.streaming().streamName()
            + "/index.m3u8";
  }

  private String buildDisplayClientUrl(String hlsStreamUrl) {
    String streamName =
            context.getStreamName() == null || context.getStreamName().isBlank()
                    ? config.streaming().streamName()
                    : context.getStreamName();

    String query =
            "displayId=" + encode(config.displayClient().displayId())
                    + "&streamUrl=" + encode(hlsStreamUrl)
                    + "&sourceId=" + encode(streamName)
                    + "&testRunId=" + encode(context.getTestRunId().toString())
                    + "&autoplay=true"
                    + "&muted=true"
                    + "&debug=true";

    String separator = config.displayClient().baseUrl().contains("?") ? "&" : "?";

    return config.displayClient().baseUrl() + separator + query;
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String stripTrailingSlash(String value) {
    if (value.endsWith("/")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
  }
}