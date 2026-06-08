package com.streaminglab.orchestration.testrun.api;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.streaminglab.orchestration.testrun.application.TestRunService;
import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.time.Instant;
import java.util.Optional;
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

  @Test
  void shouldCreateTestRun() throws Exception {
    TestRun testRun =
        new TestRun(
            "run-001",
            "camera-sync-test",
            TestRunStatus.CREATED,
            "",
            "",
            "",
            "",
            Instant.parse("2026-06-07T00:00:00Z"),
            null,
            null,
            null);

    when(testRunService.createTestRun(eq("camera-sync-test"))).thenReturn(testRun);

    mockMvc
        .perform(
            post("/api/test-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "streamName": "camera-sync-test"
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.testRunId", is("run-001")))
        .andExpect(jsonPath("$.streamName", is("camera-sync-test")))
        .andExpect(jsonPath("$.status", is("CREATED")));
  }

  @Test
  void shouldReturnTestRunById() throws Exception {
    TestRun testRun =
        new TestRun(
            "run-001",
            "camera-sync-test",
            TestRunStatus.CREATED,
            "",
            "",
            "",
            "",
            Instant.parse("2026-06-07T00:00:00Z"),
            null,
            null,
            null);

    when(testRunService.findById("run-001")).thenReturn(Optional.of(testRun));

    mockMvc
        .perform(get("/api/test-runs/run-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.testRunId", is("run-001")))
        .andExpect(jsonPath("$.streamName", is("camera-sync-test")))
        .andExpect(jsonPath("$.status", is("CREATED")));
  }

  @Test
  void shouldReturnNotFoundWhenTestRunDoesNotExist() throws Exception {
    when(testRunService.findById("missing-run")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/test-runs/missing-run")).andExpect(status().isNotFound());
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
                                  "streamName": ""
                                }
                                """))
        .andExpect(status().isBadRequest());
  }
}
