package com.streaminglab.orchestration.smoke;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class OrchestrationApiSmokeTest {
  private final HttpClient client = HttpClient.newHttpClient();
  private final String baseUrl = System.getProperty("smoke.baseUrl", "http://localhost:8080");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldReturnHealthyStatus() throws IOException, InterruptedException {
    HttpResponse<String> response = get("/actuator/health");

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"status\":\"UP\""));
  }

  @Test
  void shouldReachStreamingState() throws IOException, InterruptedException {
    String testRunId = createTestRun();

    prepareTestRun(testRunId);
    streamTestRun(testRunId);

    JsonNode body = getTestRun(testRunId);

    assertEquals("STREAMING", body.get("status").asText());
    assertTrue(body.hasNonNull("startedAt"), "Expected startedAt to be populated after streaming");
    assertTrue(body.get("stoppedAt").isNull(), "Expected stoppedAt to be null");
  }

  private String createTestRun() throws IOException, InterruptedException {
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("streamName", "camera_sync_test");
    requestBody.put("displayCount", 2);

    HttpResponse<String> response = post("/api/test-runs", requestBody.toString());
    assertEquals(201, response.statusCode());

    JsonNode body = objectMapper.readTree(response.body());
    assertAttributesNonNull(
        body, "testRunId", "streamName", "status", "hlsExternalUrl", "artifactPath", "createdAt");
    assertAttributesNull(body, "startedAt", "stoppedAt");

    String testRunId = body.get("testRunId").asText();
    String streamName = body.get("streamName").asText();
    String createdAt = body.get("createdAt").asText();

    assertDoesNotThrow(() -> UUID.fromString(testRunId));
    assertEquals("camera_sync_test", streamName);
    assertEquals("CREATED", body.get("status").asText());
    assertEquals("http://localhost:8888/camera_sync_test/", body.get("hlsExternalUrl").asText());
    assertEquals("artifacts/test-runs/" + testRunId, body.get("artifactPath").asText());
    assertDoesNotThrow(() -> Instant.parse(createdAt));
    return testRunId;
  }

  private void prepareTestRun(String testRunId) throws IOException, InterruptedException {
    HttpResponse<String> response = post("/api/test-runs/" + testRunId + "/prepare", "");

    assertEquals(200, response.statusCode());

    JsonNode body = objectMapper.readTree(response.body());

    assertEquals(testRunId, body.get("testRunId").asText());
    assertEquals("PREPARING", body.get("status").asText());

    assertAttributesNonNull(
        body, "testRunId", "streamName", "status", "hlsExternalUrl", "artifactPath", "createdAt");
    assertAttributesNull(body, "startedAt", "stoppedAt");
    String streamName = body.get("streamName").asText();

    assertEquals("camera_sync_test", streamName);
    assertEquals("http://localhost:8888/" + streamName + "/", body.get("hlsExternalUrl").asText());
    assertEquals("artifacts/test-runs/" + testRunId, body.get("artifactPath").asText());
    assertDoesNotThrow(() -> Instant.parse(body.get("createdAt").asText()));
  }

  private void streamTestRun(String testRunId) throws IOException, InterruptedException {
    HttpResponse<String> response = post("/api/test-runs/" + testRunId + "/stream", "");

    assertEquals(200, response.statusCode());

    JsonNode body = objectMapper.readTree(response.body());

    assertEquals(testRunId, body.get("testRunId").asText());
    assertEquals("STREAMING", body.get("status").asText());
    assertTrue(body.hasNonNull("startedAt"), "Expected startedAt to be populated after streaming");
    assertDoesNotThrow(() -> Instant.parse(body.get("startedAt").asText()));

    assertAttributesNonNull(
        body,
        "testRunId",
        "streamName",
        "status",
        "hlsExternalUrl",
        "artifactPath",
        "createdAt",
        "startedAt");
    assertAttributesNull(body, "stoppedAt");
  }

  private JsonNode getTestRun(String testRunId) throws IOException, InterruptedException {
    HttpResponse<String> response = get("/api/test-runs/" + testRunId);

    assertEquals(200, response.statusCode());

    JsonNode body = objectMapper.readTree(response.body());

    assertEquals(testRunId, body.get("testRunId").asText());

    return body;
  }

  private void assertAttributesNonNull(JsonNode body, String... attributes) {
    // With Jackson JsonNode, assertNotNull(body.get("field")) only proves the field node object
    // exists. It does not prove the JSON value is non-null, because JSON null is represented by
    // NullNode. For required fields, I used hasNonNull.
    for (String attribute : attributes) {
      assertTrue(body.hasNonNull(attribute), "Expected response to include " + attribute);
    }
  }

  private void assertAttributesNull(JsonNode body, String... attributes) {
    //  For fields that are expected to exist but be null, I used has plus isNull.
    for (String attribute : attributes) {
      assertTrue(body.has(attribute), "Expected response to include " + attribute);
      assertTrue(body.get(attribute).isNull(), "Expected " + attribute + " to be null");
    }
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).GET().build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String jsonBody)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
