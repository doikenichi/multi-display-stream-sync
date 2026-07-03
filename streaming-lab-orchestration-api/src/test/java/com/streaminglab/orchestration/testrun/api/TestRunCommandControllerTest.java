package com.streaminglab.orchestration.testrun.api;

import static com.streaminglab.orchestration.testrun.application.TestRunService.PREPARED_VALID_STATUS_TRANSITIONS;
import static com.streaminglab.orchestration.testrun.application.TestRunService.STREAM_VALID_STATUS_TRANSITIONS;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.streaminglab.orchestration.testrun.application.InvalidTestRunStateTransitionException;
import com.streaminglab.orchestration.testrun.application.TestRunNotFoundException;
import com.streaminglab.orchestration.testrun.application.TestRunService;
import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.dto.CreateTestRunRequest;
import com.streaminglab.orchestration.testrun.dto.FailTestRunRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ThrowingConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@Import({CommandApiExceptionHandler.class, CommonApiExceptionHandler.class})
@WebMvcTest(TestRunCommandController.class)
class TestRunCommandControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TestRunService testRunService;

  private static final UUID TEST_RUN_ID = UUID.fromString("848791c4-5cc1-41cf-b98a-9cbcae4d7eab");

  /**
   * Creates a {@link TestRun} instance with the specified {@link TestRunStatus}.
   *
   * @param status the status to assign to the test run; must be a non-null {@link TestRunStatus}.
   * @return a new {@link TestRun} instance with the given status and predefined test run details.
   */
  private static TestRun testRunWithStatus(TestRunStatus status) {
    return new TestRun(
        TEST_RUN_ID,
        "camera_sync_test",
        2,
        status,
        "http://mediamtx:8888/camera_sync_test/",
        "http://localhost:8888/camera_sync_test/",
        "rtsp://mediamtx:8554/camera_sync_test",
        "artifacts/test-runs/" + TEST_RUN_ID,
        Instant.parse("2026-06-07T00:00:00Z"),
        null,
        null,
        null);
  }

  /**
   * Provides test arguments for scenarios where actions on a test run command result in a {@code
   * TestRunNotFoundException}. These arguments include the API endpoint path, a mock service setup
   * for simulating the exception, and verification to ensure the corresponding service method was
   * invoked.
   *
   * <p>source reference: <a
   * href="https://stackoverflow.com/questions/70127774/functiont-r-as-additional-argument-in-junit-5">Function&lt;T,
   * R&gt; as additional argument in Junit 5</a> <a
   * href="https://www.baeldung.com/parameterized-tests-junit-5">Parameterized Tests in JUnit 5</a>
   *
   * @return a {@code Stream} of {@code Arguments}, where each set of arguments contains: - The API
   *     endpoint path as a {@code String}. - A {@code ThrowingConsumer<TestRunService>} responsible
   *     for mocking the service behavior to throw {@code TestRunNotFoundException}. - A {@code
   *     ThrowingConsumer<TestRunService>} responsible for verifying that the expected service
   *     method was called during the test.
   */
  private static Stream<Arguments> missingRunCommandActions() {
    return Stream.of(
        Arguments.of(
            // endpoint path
            "/api/test-runs/{testRunId}/prepare",
            // mockService mocks prepareRun and throws
            // TestRunNotFoundException
            (ThrowingConsumer<TestRunService>)
                service ->
                    when(service.prepareRun(TEST_RUN_ID))
                        .thenThrow(new TestRunNotFoundException(TEST_RUN_ID)),
            // verifyService checks that prepareRun was called
            (ThrowingConsumer<TestRunService>) service -> verify(service).prepareRun(TEST_RUN_ID)),
        Arguments.of(
            // endpoint path
            "/api/test-runs/{testRunId}/stream",
            // mockService mocks startStreaming and throws TestRunNotFoundException
            (ThrowingConsumer<TestRunService>)
                service ->
                    when(service.startStreaming(TEST_RUN_ID))
                        .thenThrow(new TestRunNotFoundException(TEST_RUN_ID)),
            // verifyService checks that startStreaming was called
            (ThrowingConsumer<TestRunService>)
                service -> verify(service).startStreaming(TEST_RUN_ID)));
  }

  @ParameterizedTest(name = "{0} should return 404 when test run is missing")
  @MethodSource("missingRunCommandActions")
  void shouldReturnNotFoundWhenCommandTargetsMissingTestRun(
      String endpoint,
      ThrowingConsumer<TestRunService> mockService,
      ThrowingConsumer<TestRunService> verifyService)
      throws Exception {

    mockService.accept(testRunService);

    mockMvc.perform(post(endpoint, TEST_RUN_ID)).andExpect(status().isNotFound());

    verifyService.accept(testRunService);
  }

  /**
   * Provides a collection of test arguments for scenarios where a command action violates a test
   * run's valid state transitions. These arguments include the API endpoint path, the current and
   * target test run statuses, valid transition statuses, a mock service setup for simulating
   * invalid transitions, and verification to ensure the corresponding service method was called.
   *
   * @return a {@code Stream} of {@code Arguments}, where each argument set contains: - The API
   *     endpoint path as a {@code String}. - The current {@code TestRunStatus} of the test run. -
   *     The target {@code TestRunStatus} to which an invalid transition is attempted. - A {@code
   *     List<TestRunStatus>} defining the valid statuses for a state transition. - A {@code
   *     ThrowingConsumer<TestRunService>} responsible for mocking the service behavior to simulate
   *     the invalid state transition. - A {@code ThrowingConsumer<TestRunService>} responsible for
   *     verifying that the appropriate service method was invoked during the test.
   */
  private static Stream<Arguments> invalidStateTransitionCommandActions() {
    return Stream.of(
        Arguments.of(
            // endpoint path
            "/api/test-runs/{testRunId}/prepare",
            // current status
            TestRunStatus.STREAMING,
            // target status
            TestRunStatus.PREPARING,
            // valid transition statuses
            PREPARED_VALID_STATUS_TRANSITIONS,
            // mockService mocks prepareRun and throws InvalidTestRunStateTransitionException
            (ThrowingConsumer<TestRunService>)
                service -> {
                  InvalidTestRunStateTransitionException exception =
                      new InvalidTestRunStateTransitionException(
                          TEST_RUN_ID,
                          TestRunStatus.STREAMING,
                          TestRunStatus.PREPARING,
                          PREPARED_VALID_STATUS_TRANSITIONS);

                  when(service.prepareRun(TEST_RUN_ID)).thenThrow(exception);
                },
            // verifyService checks that prepareRun was called
            (ThrowingConsumer<TestRunService>) service -> verify(service).prepareRun(TEST_RUN_ID)),
        Arguments.of(
            // endpoint path
            "/api/test-runs/{testRunId}/stream",
            // current status
            TestRunStatus.CREATED,
            // target status
            TestRunStatus.STREAMING,
            // valid transition statuses
            STREAM_VALID_STATUS_TRANSITIONS,
            // mockService mocks startStreaming and throws InvalidTestRunStateTransitionException
            (ThrowingConsumer<TestRunService>)
                service -> {
                  InvalidTestRunStateTransitionException exception =
                      new InvalidTestRunStateTransitionException(
                          TEST_RUN_ID,
                          TestRunStatus.CREATED,
                          TestRunStatus.STREAMING,
                          STREAM_VALID_STATUS_TRANSITIONS);

                  when(service.startStreaming(TEST_RUN_ID)).thenThrow(exception);
                },
            // verifyService checks that startStreaming was called
            (ThrowingConsumer<TestRunService>)
                service -> verify(service).startStreaming(TEST_RUN_ID)));
  }

  @ParameterizedTest
  @MethodSource("invalidStateTransitionCommandActions")
  void shouldReturnConflictWhenCommandViolatesStateTransition(
      String path,
      TestRunStatus currentStatus,
      TestRunStatus targetStatus,
      List<TestRunStatus> validStatuses,
      ThrowingConsumer<TestRunService> mockService,
      ThrowingConsumer<TestRunService> verifyService)
      throws Exception {

    InvalidTestRunStateTransitionException exception =
        new InvalidTestRunStateTransitionException(
            TEST_RUN_ID, currentStatus, targetStatus, validStatuses);

    mockService.accept(testRunService);

    mockMvc
        .perform(post(path, TEST_RUN_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorMessage").value(exception.getMessage()));

    verifyService.accept(testRunService);
  }

  /**
   * Provides a collection of test arguments for parameterized tests that validate API endpoints
   * requiring a test run ID as a path variable. The provided arguments include: - The command
   * action as a {@code String}. - The API endpoint path template as a {@code String}, which
   * contains the placeholder for the test run ID.
   *
   * @return a {@code Stream} of {@code Arguments}, where each argument pair consists of: - The
   *     command action name as a {@code String}. - The corresponding API endpoint path template as
   *     a {@code String}.
   */
  private static Stream<Arguments> commandEndpointsWithTestRunId() {
    return Stream.of(
        Arguments.of("prepare", "/api/test-runs/{testRunId}/prepare"),
        Arguments.of("stream", "/api/test-runs/{testRunId}/stream"),
        Arguments.of("stream", "/api/test-runs/{testRunId}/stop"),
        Arguments.of("stream", "/api/test-runs/{testRunId}/stopped"),
        Arguments.of("stream", "/api/test-runs/{testRunId}/fail"));
  }

  @ParameterizedTest(name = "should return 400 when {0} testRunId is invalid")
  @MethodSource("commandEndpointsWithTestRunId")
  void shouldReturnBadRequestWhenCommandTestRunIdIsInvalid(String action, String path)
      throws Exception {

    mockMvc.perform(post(path, "invalid-uuid")).andExpect(status().isBadRequest());
  }

  /*****************************************
   * CreateTestRun
   *****************************************/
  @Nested
  class CreateTestRun {
    @Test
    void shouldCreateTestRun() throws Exception {
      TestRun testRun = testRunWithStatus(TestRunStatus.CREATED);

      when(testRunService.createTestRun(eq("camera_sync_test"), eq(1))).thenReturn(testRun);

      mockMvc
          .perform(
              post("/api/test-runs")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          new CreateTestRunRequest("camera_sync_test", 1))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.testRunId", is(TEST_RUN_ID.toString())))
          .andExpect(jsonPath("$.streamName", is("camera_sync_test")))
          .andExpect(jsonPath("$.status", is("CREATED")));
    }

    /**
     * Provides a stream of invalid arguments to be used in parameterized tests that validate create
     * test run requests. Each argument consists of a description of the invalid scenario and the
     * corresponding JSON request payload.
     *
     * @return a stream of {@link Arguments} containing invalid test cases, where each case includes
     *     a scenario description and the associated invalid request body.
     */
    private static Stream<Arguments> invalidCreateTestRunRequests() {
      return Stream.of(
          Arguments.of("blank streamName", new CreateTestRunRequest("", 1)),
          Arguments.of("zero displayCount", new CreateTestRunRequest("camera-sync-test", 0)));
    }

    @ParameterizedTest(name = "should return 400 when create request has {0}")
    @MethodSource("invalidCreateTestRunRequests")
    void shouldReturnBadRequestWhenCreateRequestIsInvalid(
        String scenario, CreateTestRunRequest request) throws Exception {

      mockMvc
          .perform(
              post("/api/test-runs")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  /*****************************************
   * PrepareTestRun
   *****************************************/
  @Nested
  class PrepareTestRun {
    @Test
    void shouldPrepareTestRun() throws Exception {
      TestRun preparingTestRun = testRunWithStatus(TestRunStatus.PREPARING);

      when(testRunService.prepareRun(TEST_RUN_ID)).thenReturn(preparingTestRun);
      mockMvc
          .perform(post("/api/test-runs/{testRunId}/prepare", TEST_RUN_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
          .andExpect(jsonPath("$.status").value("PREPARING"));

      verify(testRunService).prepareRun(TEST_RUN_ID);
    }
  }

  /*****************************************
   * StartStreaming
   *****************************************/
  @Nested
  class StartStreaming {
    @Test
    void shouldStartStreamingTestRun() throws Exception {
      TestRun streamingTestRun = testRunWithStatus(TestRunStatus.STREAMING);

      when(testRunService.startStreaming(TEST_RUN_ID)).thenReturn(streamingTestRun);

      mockMvc
          .perform(post("/api/test-runs/{testRunId}/stream", TEST_RUN_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
          .andExpect(jsonPath("$.status").value("STREAMING"));

      verify(testRunService).startStreaming(TEST_RUN_ID);
    }
  }

  /*****************************************
   * StopStreaming
   *****************************************/
  @Nested
  class StopStreaming {
    @Test
    void shouldStopStreamingTestRun() throws Exception {
      TestRun stoppingTestRun = testRunWithStatus(TestRunStatus.STOPPING);

      when(testRunService.stopTestRun(TEST_RUN_ID)).thenReturn(stoppingTestRun);

      mockMvc
          .perform(post("/api/test-runs/{testRunId}/stop", TEST_RUN_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
          .andExpect(jsonPath("$.status").value("STOPPING"));

      verify(testRunService).stopTestRun(TEST_RUN_ID);
    }
  }

  /*****************************************
   * StopStreaming
   *****************************************/
  @Nested
  class StoppedStreaming {
    @Test
    void shouldStopStreamingTestRun() throws Exception {
      TestRun stoppedTestRun = testRunWithStatus(TestRunStatus.STOPPED);

      when(testRunService.markTestRunStopped(TEST_RUN_ID)).thenReturn(stoppedTestRun);

      mockMvc
          .perform(post("/api/test-runs/{testRunId}/stopped", TEST_RUN_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
          .andExpect(jsonPath("$.status").value("STOPPED"));

      verify(testRunService).markTestRunStopped(TEST_RUN_ID);
    }
  }

  /*****************************************
   * StopStreaming
   *****************************************/
  @Nested
  class failStreaming {
    @Test
    void shouldFailStreamingTestRun() throws Exception {
      TestRun failedTestRun = testRunWithStatus(TestRunStatus.FAILED);
      String errorMessage = "Stream generator failed";
      FailTestRunRequest request = new FailTestRunRequest(errorMessage);
      when(testRunService.failTestRun(TEST_RUN_ID, errorMessage)).thenReturn(failedTestRun);

      mockMvc
          .perform(
              post("/api/test-runs/{testRunId}/fail", TEST_RUN_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
          .andExpect(jsonPath("$.status").value("FAILED"));

      verify(testRunService).failTestRun(TEST_RUN_ID, errorMessage);
    }

    private static Stream<Arguments> invalidFailTestRunRequests() {
      return Stream.of(
          Arguments.of("blank errorMessage", new FailTestRunRequest("")),
          Arguments.of("null errorMessage", new FailTestRunRequest(null)));
    }

    @ParameterizedTest(name = "should return 400 when fail request has {0}")
    @MethodSource("invalidFailTestRunRequests")
    void shouldReturnBadRequestWhenFailRequestIsInvalid(String scenario, FailTestRunRequest request)
        throws Exception {

      mockMvc
          .perform(
              post("/api/test-runs/{testRunId}/fail", TEST_RUN_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }
}
