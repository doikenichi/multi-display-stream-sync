package com.streaminglab.orchestration.testrun.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryTestRunRepositoryTest {

  @Test
  void shouldSaveAndFindTestRunById() {
    InMemoryTestRunRepository repository = new InMemoryTestRunRepository();

    TestRun testRun =
        new TestRun(
            "run-001",
            "camera-sync-test",
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

    Optional<TestRun> result = repository.findById("run-001");

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(testRun);
  }

  @Test
  void shouldReturnEmptyWhenTestRunDoesNotExist() {
    InMemoryTestRunRepository repository = new InMemoryTestRunRepository();

    Optional<TestRun> result = repository.findById("missing-run");

    assertThat(result).isEmpty();
  }
}
