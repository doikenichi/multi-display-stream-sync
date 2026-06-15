package com.streaminglab.orchestration.testrun.api;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.streaminglab.orchestration.testrun.application.TestRunService;
import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(CommonApiExceptionHandler.class)
@WebMvcTest(TestRunQueryController.class)
public class TestRunControllerTestQuery {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private TestRunService testRunService;

  private static final UUID TEST_RUN_ID = UUID.fromString("848791c4-5cc1-41cf-b98a-9cbcae4d7eab");

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

    when(testRunService.findById(TEST_RUN_ID)).thenReturn(testRun);

    mockMvc
        .perform(get("/api/test-runs/" + TEST_RUN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.testRunId", is(TEST_RUN_ID.toString())))
        .andExpect(jsonPath("$.streamName", is("camera-sync-test")))
        .andExpect(jsonPath("$.status", is("CREATED")));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidIDFormat() throws Exception {
    mockMvc.perform(get("/api/test-runs/invalid-uuid")).andExpect(status().isBadRequest());
  }
}
