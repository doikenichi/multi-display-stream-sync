package com.streaminglab.testframework.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaminglab.testframework.config.TestFrameworkConfig;
import com.streaminglab.testframework.dto.CreateTestRunRequest;
import com.streaminglab.testframework.dto.TestRunResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/** Client boundary for streaming-lab-orchestration-api. */
public class OrchestrationApiClient {

  private final TestFrameworkConfig.OrchestrationApiConfig config;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public OrchestrationApiClient(TestFrameworkConfig.OrchestrationApiConfig config) {
    this.config = config;
    this.httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.timeoutMs()))
                    .build();
    this.objectMapper =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public TestRunResponse createTestRun(CreateTestRunRequest request) {
    HttpRequest httpRequest =
            HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl()).resolve("/api/test-runs"))
                    .timeout(Duration.ofMillis(config.timeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(toJson(request)))
                    .build();

    HttpResponse<String> response = send(httpRequest);

    if (response.statusCode() != 201) {
      throw new IllegalStateException(
              "Failed to create test run. Status: "
                      + response.statusCode()
                      + ", body: "
                      + response.body());
    }

    return fromJson(response.body(), TestRunResponse.class);
  }

  public TestRunResponse prepareTestRun(UUID testRunId) {
    throw new UnsupportedOperationException("Prepare test run API call is not implemented yet.");
  }

  public TestRunResponse startStream(UUID testRunId) {
    throw new UnsupportedOperationException("Start stream API call is not implemented yet.");
  }

  public TestRunResponse stopTestRun(UUID testRunId) {
    throw new UnsupportedOperationException("Stop test run API call is not implemented yet.");
  }

  public TestRunResponse failTestRun(UUID testRunId, String reason) {
    throw new UnsupportedOperationException("Fail test run API call is not implemented yet.");
  }

  public TestRunResponse getTestRun(UUID testRunId) {
    throw new UnsupportedOperationException("Get test run API call is not implemented yet.");
  }

  public String baseUrl() {
    return config.baseUrl();
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to call orchestration API.", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while calling orchestration API.", exception);
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to serialize request body.", exception);
    }
  }

  private <T> T fromJson(String body, Class<T> responseType) {
    try {
      return objectMapper.readValue(body, responseType);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to parse orchestration API response: " + body, exception);
    }
  }
}