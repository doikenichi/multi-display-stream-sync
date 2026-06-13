package com.streaminglab.orchestration.testrun.api;

import static com.streaminglab.orchestration.testrun.application.TestRunService.PREPARED_VALID_STATUS_TRANSITIONS;
import static com.streaminglab.orchestration.testrun.application.TestRunService.STREAM_VALID_STATUS_TRANSITIONS;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.streaminglab.orchestration.testrun.application.InvalidTestRunStateTransitionException;
import com.streaminglab.orchestration.testrun.application.TestRunNotFoundException;
import com.streaminglab.orchestration.testrun.application.TestRunService;
import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TestRunController.class)
class TestRunControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TestRunService testRunService;

  private final UUID TEST_RUN_ID = UUID.fromString("848791c4-5cc1-41cf-b98a-9cbcae4d7eab");

  @Test
  void shouldCreateTestRun() throws Exception {
    TestRun testRun =
        new TestRun(
            TEST_RUN_ID,
            "camera-sync-test",
            1,
            TestRunStatus.CREATED,
            "",
            "",
            "",
            "",
            Instant.parse("2026-06-07T00:00:00Z"),
            null,
            null,
            null);

    when(testRunService.createTestRun(eq("camera-sync-test"), eq(1))).thenReturn(testRun);

    mockMvc
        .perform(
            post("/api/test-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "streamName": "camera-sync-test",
                                  "displayCount": 1
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.testRunId", is(TEST_RUN_ID.toString())))
        .andExpect(jsonPath("$.streamName", is("camera-sync-test")))
        .andExpect(jsonPath("$.status", is("CREATED")));
  }

  @Test
  void shouldReturnTestRunById() throws Exception {
    TestRun testRun =
        new TestRun(
            TEST_RUN_ID,
            "camera-sync-test",
            1,
            TestRunStatus.CREATED,
            "",
            "",
            "",
            "",
            Instant.parse("2026-06-07T00:00:00Z"),
            null,
            null,
            null);

    when(testRunService.findById(TEST_RUN_ID)).thenReturn(Optional.of(testRun));

    mockMvc
        .perform(get("/api/test-runs/" + TEST_RUN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.testRunId", is(TEST_RUN_ID.toString())))
        .andExpect(jsonPath("$.streamName", is("camera-sync-test")))
        .andExpect(jsonPath("$.status", is("CREATED")));
  }

  @Test
  void shouldReturnNotFoundWhenTestRunDoesNotExist() throws Exception {
    UUID missingID = UUID.fromString("c66c2527-57b1-41a8-b502-a8594776c041");
    when(testRunService.findById(missingID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/test-runs/" + missingID)).andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnBadRequestWhenInvalidIDFormat() throws Exception {
    mockMvc.perform(get("/api/test-runs/invalid-uuid")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenStreamNameIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/test-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "streamName": "",
                                  "displayCount": 1
                                }
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenDisplayCountIsZero() throws Exception {
    mockMvc
        .perform(
            post("/api/test-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                                {
                                                  "streamName": "camera-sync-test",
                                                  "displayCount": 0
                                                }
                                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldPrepareTestRun() throws Exception {
    TestRun preparingTestRun =
        new TestRun(
            TEST_RUN_ID,
            "camera_sync_test",
            2,
            TestRunStatus.PREPARING,
            "http://mediamtx:8888/camera_sync_test/",
            "http://localhost:8888/camera_sync_test/",
            "rtsp://mediamtx:8554/camera_sync_test",
            "artifacts/test-runs/" + TEST_RUN_ID,
            Instant.parse("2026-06-07T00:00:00Z"),
            null,
            null,
            null);

    when(testRunService.prepareRun(TEST_RUN_ID)).thenReturn(preparingTestRun);
    mockMvc
        .perform(post("/api/test-runs/{testRunId}/prepare", TEST_RUN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
        .andExpect(jsonPath("$.status").value("PREPARING"));

    verify(testRunService).prepareRun(TEST_RUN_ID);
  }

  @Test
  void shouldReturnConflictWhenPreparingStreamingNonValidTestRun() throws Exception {
    InvalidTestRunStateTransitionException exception =
        new InvalidTestRunStateTransitionException(
            TEST_RUN_ID, TestRunStatus.STREAMING, PREPARED_VALID_STATUS_TRANSITIONS);
    when(testRunService.prepareRun(TEST_RUN_ID)).thenThrow(exception);

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/prepare", TEST_RUN_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorMessage").value(exception.getMessage()));

    verify(testRunService).prepareRun(TEST_RUN_ID);
  }

  @Test
  void shouldReturnNotFoundWhenPreparingMissingTestRun() throws Exception {
    when(testRunService.prepareRun(TEST_RUN_ID))
        .thenThrow(new TestRunNotFoundException(TEST_RUN_ID));

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/prepare", TEST_RUN_ID))
        .andExpect(status().isNotFound());

    verify(testRunService).prepareRun(TEST_RUN_ID);
  }

  @Test
  void shouldStartStreamingTestRun() throws Exception {
    TestRun streamingTestRun =
        new TestRun(
            TEST_RUN_ID,
            "camera_sync_test",
            2,
            TestRunStatus.STREAMING,
            "http://mediamtx:8888/camera_sync_test/",
            "http://localhost:8888/camera_sync_test/",
            "rtsp://mediamtx:8554/camera_sync_test",
            "artifacts/test-runs/" + TEST_RUN_ID,
            Instant.parse("2026-06-07T00:00:00Z"),
            null,
            null,
            null);

    when(testRunService.startStreaming(TEST_RUN_ID)).thenReturn(streamingTestRun);

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/stream", TEST_RUN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.testRunId").value(TEST_RUN_ID.toString()))
        .andExpect(jsonPath("$.status").value("STREAMING"));

    verify(testRunService).startStreaming(TEST_RUN_ID);
  }

  @Test
  void shouldReturnNotFoundWhenStreamingMissingTestRun() throws Exception {
    when(testRunService.startStreaming(TEST_RUN_ID))
        .thenThrow(new TestRunNotFoundException(TEST_RUN_ID));

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/stream", TEST_RUN_ID))
        .andExpect(status().isNotFound());

    verify(testRunService).startStreaming(TEST_RUN_ID);
  }

  @Test
  void shouldReturnConflictWhenStartingStreamingNonPreparingTestRun() throws Exception {
    InvalidTestRunStateTransitionException exception =
        new InvalidTestRunStateTransitionException(
            TEST_RUN_ID, TestRunStatus.CREATED, STREAM_VALID_STATUS_TRANSITIONS);
    when(testRunService.startStreaming(TEST_RUN_ID)).thenThrow(exception);

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/stream", TEST_RUN_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorMessage").value(exception.getMessage()));

    verify(testRunService).startStreaming(TEST_RUN_ID);
  }

  @Test
  void shouldReturnBadRequestWhenStreamingTestRunIdIsInvalid() throws Exception {
    mockMvc.perform(post("/api/test-runs/invalid-uuid/stream")).andExpect(status().isBadRequest());
  }
}
