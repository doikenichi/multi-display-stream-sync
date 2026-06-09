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
                                  displayCount: 1
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
}
