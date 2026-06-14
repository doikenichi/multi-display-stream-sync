package com.streaminglab.orchestration.testrun.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.repository.InMemoryTestRunRepository;
import com.streaminglab.orchestration.testrun.repository.TestRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TestRunServiceTest {
  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneOffset.UTC);
  private final TestRunRepository repository = new InMemoryTestRunRepository();
  private final TestRunService testRunService = new TestRunService(repository, fixedClock);

  @Test
  void shouldCreateTestRunWithCreatedStatus() {
    TestRun testRun = testRunService.createTestRun("camera_sync_test", 2);

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
    TestRun created = testRunService.createTestRun("camera-sync-test", 1);

    Optional<TestRun> result = testRunService.findById(created.testRunId());

    assertThat(result).contains(created);
  }

  @Test
  void shouldUseSameTestRunIdInArtifactPath() {
    TestRun testRun = testRunService.createTestRun("camera_sync_test", 2);

    assertThat(testRun.artifactPath()).isEqualTo("artifacts/test-runs/" + testRun.testRunId());
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"CREATED", "FAILED", "STOPPED"})
  void shouldPrepareRunFromAllowedStatus(TestRunStatus allowedStatus) {
    TestRun testRun = testRunService.createTestRun("camera_sync_test", 2);

    TestRun runWithAllowedStatus =
        new TestRun(
            testRun.testRunId(),
            testRun.streamName(),
            testRun.displayCount(),
            allowedStatus,
            testRun.hlsInternalUrl(),
            testRun.hlsExternalUrl(),
            testRun.rtspPublishUrl(),
            testRun.artifactPath(),
            testRun.createdAt(),
            testRun.startedAt(),
            testRun.stoppedAt(),
            testRun.errorMessage());

    repository.save(runWithAllowedStatus);

    TestRun updatedTestRun = testRunService.prepareRun(testRun.testRunId());

    assertThat(updatedTestRun.status()).isEqualTo(TestRunStatus.PREPARING);
    assertThat(updatedTestRun.testRunId()).isEqualTo(testRun.testRunId());
    assertThat(updatedTestRun.streamName()).isEqualTo(testRun.streamName());
    assertThat(updatedTestRun.artifactPath()).isEqualTo(testRun.artifactPath());
  }

  @Test
  void shouldRejectPreparingWhenTestRunIsNotValid() {
    TestRun testRun = testRunService.createTestRun("camera_sync_test", 2);
    TestRun failedRun =
        new TestRun(
            testRun.testRunId(),
            testRun.streamName(),
            testRun.displayCount(),
            TestRunStatus.STREAMING,
            testRun.hlsInternalUrl(),
            testRun.hlsExternalUrl(),
            testRun.rtspPublishUrl(),
            testRun.artifactPath(),
            testRun.createdAt(),
            testRun.startedAt(),
            testRun.stoppedAt(),
            testRun.errorMessage());
    repository.save(failedRun);

    assertThatThrownBy(() -> testRunService.prepareRun(testRun.testRunId()))
        .isInstanceOf(InvalidTestRunStateTransitionException.class)
        .hasMessageContaining(TestRunStatus.CREATED.toString())
        .hasMessageContaining(TestRunStatus.FAILED.toString())
        .hasMessageContaining(TestRunStatus.STOPPED.toString())
        .hasMessageContaining(testRun.testRunId().toString());
  }

  @Test
  void shouldThrowExceptionWhenMarkingMissingTestRunAsPreparing() {
    UUID missingTestRunId = UUID.randomUUID();

    assertThatThrownBy(() -> testRunService.prepareRun(missingTestRunId))
        .isInstanceOf(TestRunNotFoundException.class)
        .hasMessage("Test run not found: " + missingTestRunId);
  }

  @Test
  void shouldStartStreamingPreparedTestRun() {
    TestRun testRun = testRunService.createTestRun("camera_sync_test", 2);
    TestRun preparingTestRun = testRunService.prepareRun(testRun.testRunId());

    TestRun streamingTestRun = testRunService.startStreaming(preparingTestRun.testRunId());

    assertThat(streamingTestRun.status()).isEqualTo(TestRunStatus.STREAMING);
    assertThat(streamingTestRun.testRunId()).isEqualTo(testRun.testRunId());
    assertThat(streamingTestRun.streamName()).isEqualTo(testRun.streamName());
    assertThat(streamingTestRun.artifactPath()).isEqualTo(testRun.artifactPath());
  }

  @Test
  void shouldRejectStartingStreamingWhenTestRunIsNotPreparing() {
    TestRun testRun = testRunService.createTestRun("camera_sync_test", 2);

    assertThatThrownBy(() -> testRunService.startStreaming(testRun.testRunId()))
        .isInstanceOf(InvalidTestRunStateTransitionException.class)
        .hasMessageContaining(TestRunStatus.CREATED.toString())
        .hasMessageContaining(testRun.testRunId().toString());
  }

  @Test
  void shouldThrowNotFoundWhenStartingStreamingMissingTestRun() {
    UUID missingTestRunId = UUID.randomUUID();

    assertThatThrownBy(() -> testRunService.startStreaming(missingTestRunId))
        .isInstanceOf(TestRunNotFoundException.class)
        .hasMessage("Test run not found: " + missingTestRunId);
  }
}
