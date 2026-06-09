package com.streaminglab.orchestration.testrun.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.repository.InMemoryTestRunRepository;
import com.streaminglab.orchestration.testrun.repository.TestRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestRunServiceTest {
  private final Clock fixedClock =
          Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneOffset.UTC);
  private final TestRunRepository repository = new InMemoryTestRunRepository();
  private final TestRunService service = new TestRunService(repository, fixedClock);

  @Test
  void shouldCreateTestRunWithCreatedStatus() {
    TestRun testRun = service.createTestRun("camera_sync_test", 2);

    assertThat(testRun.testRunId()).isNotNull();
    assertThat(testRun.streamName()).isEqualTo("camera_sync_test");
    assertThat(testRun.displayCount()).isEqualTo(2);
    assertThat(testRun.status()).isEqualTo(TestRunStatus.CREATED);
    assertThat(testRun.hlsInternalUrl()).isEqualTo("http://mediamtx:8888/camera_sync_test/");
    assertThat(testRun.hlsExternalUrl()).isEqualTo("http://localhost:8888/camera_sync_test/");
    assertThat(testRun.rtspPublishUrl()).isEqualTo("rtsp://mediamtx:8554/camera_sync_test");
    assertThat(testRun.artifactPath()).startsWith("artifacts/test-runs/");
    assertThat(testRun.createdAt()).isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
    assertThat(testRun.startedAt()).isNull();
    assertThat(testRun.stoppedAt()).isNull();
    assertThat(testRun.errorMessage()).isNull();
  }

  @Test
  void shouldFindCreatedTestRunById() {
    TestRun created = service.createTestRun("camera-sync-test",1);

    Optional<TestRun> result = service.findById(created.testRunId());

    assertThat(result).contains(created);
  }

  @Test
  void shouldUseSameTestRunIdInArtifactPath() {
    TestRun testRun = service.createTestRun("camera_sync_test", 2);

    assertThat(testRun.artifactPath())
            .isEqualTo("artifacts/test-runs/" + testRun.testRunId());
  }
}
