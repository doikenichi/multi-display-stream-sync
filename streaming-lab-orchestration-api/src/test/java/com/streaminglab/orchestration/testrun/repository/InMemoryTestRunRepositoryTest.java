package com.streaminglab.orchestration.testrun.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryTestRunRepositoryTest {

  private final UUID TEST_RUN_ID = UUID.fromString("848791c4-5cc1-41cf-b98a-9cbcae4d7eab");
  private final UUID MISSING_RUN_ID = UUID.fromString("c66c2527-57b1-41a8-b502-a8594776c041");

  @Test
  void shouldSaveAndFindTestRunById() {
    InMemoryTestRunRepository repository = new InMemoryTestRunRepository();

    TestRun testRun =
        new TestRun(
            TEST_RUN_ID,
            "camera-sync-test",
            1,
            TestRunStatus.CREATED,
            "http://mediamtx:8888/camera-sync-test/",
            "http://localhost:8888/camera-sync-test/",
            "rtsp://mediamtx:8554/camera-sync-test",
            "artifacts/runs/run-001",
            Instant.now(),
            null,
            null,
            null);

    repository.save(testRun);

    Optional<TestRun> result = repository.findById(TEST_RUN_ID);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testRun);
  }

  @Test
  void shouldReturnEmptyWhenTestRunDoesNotExist() {
    InMemoryTestRunRepository repository = new InMemoryTestRunRepository();

    Optional<TestRun> result = repository.findById(MISSING_RUN_ID);

    assertThat(result).isEmpty();
  }
}
