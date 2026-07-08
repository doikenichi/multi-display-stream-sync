package com.streaminglab.testframework.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaminglab.testframework.config.TestFrameworkConfig;
import com.streaminglab.testframework.dto.CreateTestRunRequest;
import com.streaminglab.testframework.dto.TestRunResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrchestrationApiClientTest {

  private static final String BASE_URL = "http://orchestration.example";
  private static final int TIMEOUT_MS = 1000;
  private static final UUID TEST_RUN_ID =
      UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final String STREAM_NAME = "camera_sync_test";
  private static final String STATUS_CREATED = "CREATED";
  private static final String STATUS_PREPARING = "PREPARING";
  private static final String STATUS_STREAMING = "STREAMING";
  private static final String HLS_EXTERNAL_URL =
      "http://streaming.example/camera_sync_test/index.m3u8";
  private static final String ARTIFACT_PATH = "build/evidence";
  private static final String RESPONSE_JSON =
      """
      {
        "testRunId": "123e4567-e89b-12d3-a456-426614174000",
        "streamName": "camera_sync_test",
        "status": "%s",
        "hlsExternalUrl": "http://streaming.example/camera_sync_test/index.m3u8",
        "artifactPath": "build/evidence"
      }
      """;

  @Mock private HttpClient httpClient;
  @Mock private HttpClient.Builder httpClientBuilder;
  @Mock private HttpResponse<String> httpResponse;

  private OrchestrationApiClient client;

  @BeforeEach
  void setUp() {
    client = newClientWithMockHttpClient();
  }

  @Test
  void shouldCreateTestRunWhenApiReturnsCreated() throws Exception {
    // Arrange
    CreateTestRunRequest request = new CreateTestRunRequest(STREAM_NAME, 1);
    givenResponse(201, responseJson(STATUS_CREATED));
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    // Act
    TestRunResponse result = client.createTestRun(request);

    // Assert
    then(httpClient)
        .should()
        .send(
            requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    HttpRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.uri()).hasToString(BASE_URL + "/api/test-runs");
    assertThat(capturedRequest.method()).isEqualTo("POST");
    assertThat(capturedRequest.timeout()).contains(Duration.ofMillis(TIMEOUT_MS));
    assertThat(capturedRequest.headers().firstValue("Content-Type")).contains("application/json");
    assertThat(capturedRequest.headers().firstValue("Accept")).contains("application/json");
    assertThat(bodyOf(capturedRequest))
        .isEqualTo("{\"streamName\":\"camera_sync_test\",\"displayCount\":1}");
    assertThat(result).isEqualTo(response(STATUS_CREATED));
  }

  @Test
  void shouldRejectCreateTestRunWhenApiDoesNotReturnCreated() throws Exception {
    // Arrange
    givenResponse(500, "server unavailable");

    // Act
    assertThatThrownBy(() -> client.createTestRun(new CreateTestRunRequest(STREAM_NAME, 1)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to create test run")
        .hasMessageContaining("Status: 500")
        .hasMessageContaining("body: server unavailable");

    // Assert
    then(httpClient).should().send(anyRequest(), anyStringBodyHandler());
  }

  @Test
  void shouldPrepareTestRunWhenApiReturnsOk() throws Exception {
    // Arrange
    givenResponse(200, responseJson(STATUS_PREPARING));
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    // Act
    TestRunResponse result = client.prepareTestRun(TEST_RUN_ID);

    // Assert
    then(httpClient)
        .should()
        .send(
            requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    HttpRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.uri())
        .hasToString(BASE_URL + "/api/test-runs/" + TEST_RUN_ID + "/prepare");
    assertThat(capturedRequest.method()).isEqualTo("POST");
    assertThat(capturedRequest.bodyPublisher()).isPresent();
    assertThat(capturedRequest.bodyPublisher().orElseThrow().contentLength()).isZero();
    assertThat(capturedRequest.headers().firstValue("Accept")).contains("application/json");
    assertThat(result).isEqualTo(response(STATUS_PREPARING));
  }

  @Test
  void shouldStartStreamWhenApiReturnsOk() throws Exception {
    // Arrange
    givenResponse(200, responseJson(STATUS_STREAMING));
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    // Act
    TestRunResponse result = client.startStream(TEST_RUN_ID);

    // Assert
    then(httpClient)
        .should()
        .send(
            requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    HttpRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.uri())
        .hasToString(BASE_URL + "/api/test-runs/" + TEST_RUN_ID + "/stream");
    assertThat(capturedRequest.method()).isEqualTo("POST");
    assertThat(capturedRequest.bodyPublisher()).isPresent();
    assertThat(capturedRequest.bodyPublisher().orElseThrow().contentLength()).isZero();
    assertThat(capturedRequest.headers().firstValue("Accept")).contains("application/json");
    assertThat(result).isEqualTo(response(STATUS_STREAMING));
  }

  @Test
  void shouldGetTestRunWhenApiReturnsOk() throws Exception {
    // Arrange
    givenResponse(200, responseJson(STATUS_STREAMING));
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

    // Act
    TestRunResponse result = client.getTestRun(TEST_RUN_ID);

    // Assert
    then(httpClient)
        .should()
        .send(
            requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    HttpRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.uri()).hasToString(BASE_URL + "/api/test-runs/" + TEST_RUN_ID);
    assertThat(capturedRequest.method()).isEqualTo("GET");
    assertThat(capturedRequest.bodyPublisher()).isEmpty();
    assertThat(capturedRequest.headers().firstValue("Accept")).contains("application/json");
    assertThat(result).isEqualTo(response(STATUS_STREAMING));
  }

  @Test
  void shouldRejectLifecycleRequestWhenApiDoesNotReturnOk() throws Exception {
    // Arrange
    givenResponse(404, "missing");

    // Act
    assertThatThrownBy(() -> client.getTestRun(TEST_RUN_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to create test run")
        .hasMessageContaining("Status: 404")
        .hasMessageContaining("body: missing");

    // Assert
    then(httpClient).should().send(anyRequest(), anyStringBodyHandler());
  }

  @Test
  void shouldRejectPrepareTestRunWhenApiDoesNotReturnOk() throws Exception {
    // Arrange
    givenResponse(409, "already preparing");

    // Act
    assertThatThrownBy(() -> client.prepareTestRun(TEST_RUN_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to create test run")
        .hasMessageContaining("Status: 409")
        .hasMessageContaining("body: already preparing");

    // Assert
    then(httpClient).should().send(anyRequest(), anyStringBodyHandler());
  }

  @Test
  void shouldRejectStartStreamWhenApiDoesNotReturnOk() throws Exception {
    // Arrange
    givenResponse(503, "stream unavailable");

    // Act
    assertThatThrownBy(() -> client.startStream(TEST_RUN_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to create test run")
        .hasMessageContaining("Status: 503")
        .hasMessageContaining("body: stream unavailable");

    // Assert
    then(httpClient).should().send(anyRequest(), anyStringBodyHandler());
  }

  @Test
  void shouldRejectCreateTestRunWhenResponseBodyIsInvalidJson() throws Exception {
    // Arrange
    givenResponse(201, "not-json");

    // Act
    assertThatThrownBy(() -> client.createTestRun(new CreateTestRunRequest(STREAM_NAME, 1)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to parse orchestration API response: not-json")
        .hasCauseInstanceOf(IOException.class);

    // Assert
    then(httpClient).should().send(anyRequest(), anyStringBodyHandler());
  }

  @Test
  void shouldWrapIOExceptionWhenCallingApi() throws Exception {
    // Arrange
    IOException ioException = new IOException("connection reset");
    given(httpClient.send(anyRequest(), anyStringBodyHandler())).willThrow(ioException);

    // Act
    assertThatThrownBy(() -> client.getTestRun(TEST_RUN_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to call orchestration API.")
        .hasCause(ioException);

    // Assert
    then(httpClient).should().send(anyRequest(), anyStringBodyHandler());
  }

  @Test
  void shouldRestoreInterruptFlagWhenCallingApiIsInterrupted() throws Exception {
    // Arrange
    InterruptedException interruptedException = new InterruptedException("interrupted");
    given(httpClient.send(anyRequest(), anyStringBodyHandler())).willThrow(interruptedException);

    try {
      // Act
      assertThatThrownBy(() -> client.getTestRun(TEST_RUN_ID))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Interrupted while calling orchestration API.")
          .hasCause(interruptedException);

      // Assert
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      then(httpClient).should().send(anyRequest(), anyStringBodyHandler());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void shouldWrapSerializationFailureWhenCreatingTestRun() throws JsonProcessingException {
    // Arrange
    JsonProcessingException jsonProcessingException =
        new JsonProcessingException("cannot serialize") {};

    try (MockedConstruction<ObjectMapper> objectMapperConstruction =
        mockConstruction(
            ObjectMapper.class,
            (mock, context) -> {
              given(mock.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
                  .willReturn(mock);
              given(mock.writeValueAsString(any())).willThrow(jsonProcessingException);
            })) {
      OrchestrationApiClient localClient = newClientWithMockHttpClient();

      // Act
      assertThatThrownBy(
              () -> localClient.createTestRun(new CreateTestRunRequest(STREAM_NAME, 1)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Failed to serialize request body.")
          .hasCause(jsonProcessingException);

      // Assert
      ObjectMapper constructedObjectMapper = objectMapperConstruction.constructed().get(0);
      then(constructedObjectMapper).should().writeValueAsString(any());
      then(httpClient).shouldHaveNoInteractions();
    }
  }

  @Test
  void shouldRejectStopTestRunBecauseItIsNotImplemented() {
    // Arrange

    // Act
    assertThatThrownBy(() -> client.stopTestRun(TEST_RUN_ID))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Stop test run API call is not implemented yet.");

    // Assert
  }

  @Test
  void shouldRejectFailTestRunBecauseItIsNotImplemented() {
    // Arrange

    // Act
    assertThatThrownBy(() -> client.failTestRun(TEST_RUN_ID, "timeout"))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Fail test run API call is not implemented yet.");

    // Assert
  }

  @Test
  void shouldReturnConfiguredBaseUrl() {
    // Arrange

    // Act
    String result = client.baseUrl();

    // Assert
    assertThat(result).isEqualTo(BASE_URL);
  }

  private OrchestrationApiClient newClientWithMockHttpClient() {
    try (MockedStatic<HttpClient> httpClientFactory = mockStatic(HttpClient.class)) {
      httpClientFactory.when(HttpClient::newBuilder).thenReturn(httpClientBuilder);
      given(httpClientBuilder.connectTimeout(Duration.ofMillis(TIMEOUT_MS)))
          .willReturn(httpClientBuilder);
      given(httpClientBuilder.build()).willReturn(httpClient);
      return new OrchestrationApiClient(
          new TestFrameworkConfig.OrchestrationApiConfig(BASE_URL, TIMEOUT_MS));
    }
  }

  private void givenResponse(int statusCode, String body) throws IOException, InterruptedException {
    given(httpResponse.statusCode()).willReturn(statusCode);
    given(httpResponse.body()).willReturn(body);
    given(httpClient.send(anyRequest(), anyStringBodyHandler())).willReturn(httpResponse);
  }

  private static TestRunResponse response(String status) {
    return new TestRunResponse(
        TEST_RUN_ID, STREAM_NAME, status, HLS_EXTERNAL_URL, ARTIFACT_PATH);
  }

  private static String responseJson(String status) {
    return RESPONSE_JSON.replace("%s", status);
  }

  private static String bodyOf(HttpRequest request) {
    BodySubscriber subscriber = new BodySubscriber();
    request.bodyPublisher().orElseThrow().subscribe(subscriber);
    return subscriber.body();
  }

  private static HttpRequest anyRequest() {
    return any(HttpRequest.class);
  }

  private static HttpResponse.BodyHandler<String> anyStringBodyHandler() {
    return ArgumentMatchers.<HttpResponse.BodyHandler<String>>any();
  }

  private static final class BodySubscriber implements Flow.Subscriber<ByteBuffer> {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final CompletableFuture<String> bodyFuture = new CompletableFuture<>();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(ByteBuffer item) {
      byte[] bytes = new byte[item.remaining()];
      item.get(bytes);
      output.writeBytes(bytes);
    }

    @Override
    public void onError(Throwable throwable) {
      bodyFuture.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
      bodyFuture.complete(output.toString(StandardCharsets.UTF_8));
    }

    private String body() {
      return bodyFuture.join();
    }
  }
}

