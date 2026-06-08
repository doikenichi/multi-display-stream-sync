package com.streaminglab.orchestration.testrun.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.repository.InMemoryTestRunRepository;
import com.streaminglab.orchestration.testrun.repository.TestRunRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestRunServiceTest {

  @Test
  void shouldCreateTestRunWithCreatedStatus() {
    TestRunRepository repository = new InMemoryTestRunRepository();
    TestRunService service = new TestRunService(repository);

    TestRun testRun = service.createTestRun("camera-sync-test");

    assertThat(testRun.testRunId()).isNotBlank();
    assertThat(testRun.streamName()).isEqualTo("camera-sync-test");
    assertThat(testRun.status()).isEqualTo(TestRunStatus.CREATED);
    assertThat(testRun.createdAt()).isNotNull();
    assertThat(repository.findById(testRun.testRunId())).contains(testRun);
  }

  @Test
  void shouldFindCreatedTestRunById() {
    TestRunRepository repository = new InMemoryTestRunRepository();
    TestRunService service = new TestRunService(repository);

    TestRun created = service.createTestRun("camera-sync-test");

    Optional<TestRun> result = service.findById(created.testRunId());

    assertThat(result).contains(created);
  }
}
