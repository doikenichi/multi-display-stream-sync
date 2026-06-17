package com.streaminglab.orchestration.testrun.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.fixtures.TestRunFixtures;
import com.streaminglab.orchestration.testrun.repository.InMemoryTestRunRepository;
import com.streaminglab.orchestration.testrun.repository.TestRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class TestRunServiceTest {
  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneOffset.UTC);
  private static final UUID MISSING_TEST_RUN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  private TestRunRepository repository;
  private TestRunService testRunService;

  // A BiConsumer<A, B> is a function that accepts two inputs and returns nothing
  // A function that receives:
  // 1. a TestRunService
  // 2. a UUID
  private record MissingRunAction(String actionName, BiConsumer<TestRunService, UUID> action) {

    @Override
    public @NonNull String toString() {
      return actionName;
    }
  }

  // A Stream of MissingRunAction objects
  // JUnit requires @MethodSource methods to be static by default.
  private static Stream<MissingRunAction> missingRunActions() {
    return Stream.of(
        new MissingRunAction("prepareRun", TestRunService::prepareRun),
        new MissingRunAction("startStreaming", TestRunService::startStreaming),
        new MissingRunAction("stopTestRun", TestRunService::stopTestRun),
        // Given a service and a testRunId, call failTestRun using that ID and a fixed error
        // message.
        new MissingRunAction(
            "failTestRun",
            (service, testRunId) ->
                service.failTestRun(testRunId, "FFmpeg process exited unexpectedly")),
        new MissingRunAction("markTestRunStopped", TestRunService::markTestRunStopped));
  }

  @BeforeEach
  void setUp() {
    this.repository = new InMemoryTestRunRepository();
    this.testRunService = new TestRunService(this.repository, this.fixedClock);
  }

  @ParameterizedTest(name = "{0} should throw TestRunNotFoundException when test run is missing")
  @MethodSource("missingRunActions") // invokes missingRunActions()
  void shouldThrowNotFoundWhenTestRunIsMissing(MissingRunAction missingRunAction) {
    // .action() returns a BiConsumer<A, B>
    // .accept(testRunService, missingTestRunId) Executes that function with service and testRunId
    // in the end, acts as testRunService.startStreaming(missingTestRunId)
    // or act as testRunService.failTestRun(missingTestRunId,"FFmpeg process exited unexpectedly")
    assertThatThrownBy(() -> missingRunAction.action().accept(testRunService, MISSING_TEST_RUN_ID))
        .isInstanceOf(TestRunNotFoundException.class)
        .hasMessage("Test run not found: " + MISSING_TEST_RUN_ID);
  }

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
    assertThat(testRun.finishedAt()).isNull();
    assertThat(testRun.errorMessage()).isNull();
  }

  @Test
  void shouldFindCreatedTestRunById() {
    TestRun created = testRunService.createTestRun("camera-sync-test", 1);

    TestRun result = testRunService.findById(created.testRunId());

    assertThat(result).isEqualTo(created);
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"CREATED", "FAILED", "STOPPED"})
  void shouldPrepareRunFromAllowedStatus(TestRunStatus allowedStatus) {
    TestRun runWithAllowedStatus = TestRunFixtures.testRunWithStatus(allowedStatus);
    repository.save(runWithAllowedStatus);

    TestRun updatedTestRun = testRunService.prepareRun(runWithAllowedStatus.testRunId());

    assertThat(updatedTestRun.status()).isEqualTo(TestRunStatus.PREPARING);
    assertThat(updatedTestRun.streamName()).isEqualTo("camera_sync_test");
    assertThat(updatedTestRun.displayCount()).isEqualTo(2);
    assertThat(updatedTestRun.hlsInternalUrl()).isEqualTo("http://mediamtx:8888/camera_sync_test/");
    assertThat(updatedTestRun.hlsExternalUrl())
        .isEqualTo("http://localhost:8888/camera_sync_test/");
    assertThat(updatedTestRun.rtspPublishUrl()).isEqualTo("rtsp://mediamtx:8554/camera_sync_test");
    assertThat(updatedTestRun.artifactPath()).isEqualTo(runWithAllowedStatus.artifactPath());
    assertThat(updatedTestRun.createdAt()).isEqualTo(runWithAllowedStatus.createdAt());
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"PREPARING", "STREAMING", "STOPPING"})
  void shouldRejectPrepareRunFromDisallowedStatus(TestRunStatus disallowedStatus) {
    TestRun runWithDisallowedStatus = TestRunFixtures.testRunWithStatus(disallowedStatus);
    repository.save(runWithDisallowedStatus);

    assertThatThrownBy(() -> testRunService.prepareRun(runWithDisallowedStatus.testRunId()))
        .isInstanceOf(InvalidTestRunStateTransitionException.class)
        .hasMessageContaining(TestRunStatus.PREPARING.toString())
        .hasMessageContaining(TestRunStatus.CREATED.toString())
        .hasMessageContaining(TestRunStatus.FAILED.toString())
        .hasMessageContaining(TestRunStatus.STOPPED.toString())
        .hasMessageContaining(disallowedStatus.toString())
        .hasMessageContaining(runWithDisallowedStatus.testRunId().toString());
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"PREPARING"})
  void shouldStartStreamingPreparedTestRun(TestRunStatus allowedStatus) {
    TestRun runWithAllowedStatus = TestRunFixtures.testRunWithStatus(allowedStatus);
    repository.save(runWithAllowedStatus);

    TestRun streamingTestRun = testRunService.startStreaming(runWithAllowedStatus.testRunId());

    assertThat(streamingTestRun.status()).isEqualTo(TestRunStatus.STREAMING);
    assertThat(streamingTestRun.streamName()).isEqualTo("camera_sync_test");
    assertThat(streamingTestRun.displayCount()).isEqualTo(2);
    assertThat(streamingTestRun.hlsInternalUrl())
        .isEqualTo("http://mediamtx:8888/camera_sync_test/");
    assertThat(streamingTestRun.hlsExternalUrl())
        .isEqualTo("http://localhost:8888/camera_sync_test/");
    assertThat(streamingTestRun.rtspPublishUrl())
        .isEqualTo("rtsp://mediamtx:8554/camera_sync_test");
    assertThat(streamingTestRun.artifactPath()).isEqualTo(runWithAllowedStatus.artifactPath());
    assertThat(streamingTestRun.createdAt()).isEqualTo(runWithAllowedStatus.createdAt());
    assertThat(streamingTestRun.startedAt()).isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
    assertThat(streamingTestRun.finishedAt()).isNull();
    assertThat(streamingTestRun.errorMessage()).isNull();
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"CREATED", "STREAMING", "STOPPING", "STOPPED", "FAILED"})
  void shouldRejectStartingStreamingFromDisallowedStatus(TestRunStatus disallowedStatus) {
    TestRun runWithDisallowedStatus = TestRunFixtures.testRunWithStatus(disallowedStatus);
    repository.save(runWithDisallowedStatus);

    assertThatThrownBy(() -> testRunService.startStreaming(runWithDisallowedStatus.testRunId()))
        .isInstanceOf(InvalidTestRunStateTransitionException.class)
        .hasMessageContaining(TestRunStatus.STREAMING.toString())
        .hasMessageContaining(TestRunStatus.PREPARING.toString())
        .hasMessageContaining(disallowedStatus.toString())
        .hasMessageContaining(runWithDisallowedStatus.testRunId().toString());
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"STREAMING"})
  void shouldMoveStreamingTestRunToStopping(TestRunStatus allowedStatus) {
    TestRun runWithAllowedStatus = TestRunFixtures.testRunWithStatus(allowedStatus);
    repository.save(runWithAllowedStatus);

    TestRun stopping = testRunService.stopTestRun(runWithAllowedStatus.testRunId());

    assertThat(stopping.status()).isEqualTo(TestRunStatus.STOPPING);
    assertThat(stopping.finishedAt()).isNull();
    assertThat(stopping.errorMessage()).isNull();
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"CREATED", "FAILED", "PREPARING", "STOPPING", "STOPPED"})
  void shouldRejectStopRunFromDisallowedStatus(TestRunStatus disallowedStatus) {
    TestRun runWithDisallowedStatus = TestRunFixtures.testRunWithStatus(disallowedStatus);
    repository.save(runWithDisallowedStatus);

    assertThatThrownBy(() -> testRunService.stopTestRun(runWithDisallowedStatus.testRunId()))
        .isInstanceOf(InvalidTestRunStateTransitionException.class)
        .hasMessageContaining(TestRunStatus.STOPPING.toString())
        .hasMessageContaining(TestRunStatus.STREAMING.toString())
        .hasMessageContaining(disallowedStatus.toString())
        .hasMessageContaining(runWithDisallowedStatus.testRunId().toString());
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"PREPARING", "STREAMING", "STOPPING"})
  void shouldFailRunFromAllowedStatus(TestRunStatus allowedStatus) {
    TestRun runWithAllowedStatus = TestRunFixtures.testRunWithStatus(allowedStatus);
    this.repository.save(runWithAllowedStatus);

    TestRun failed =
        this.testRunService.failTestRun(
            runWithAllowedStatus.testRunId(), "FFmpeg process exited unexpectedly");

    assertThat(failed.status()).isEqualTo(TestRunStatus.FAILED);
    assertThat(failed.finishedAt()).isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
    assertThat(failed.errorMessage()).isEqualTo("FFmpeg process exited unexpectedly");
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"CREATED", "FAILED", "STOPPED"})
  void shouldRejectFailRunFromDisallowedStatus(TestRunStatus disallowedStatus) {
    TestRun runWithDisallowedStatus = TestRunFixtures.testRunWithStatus(disallowedStatus);
    repository.save(runWithDisallowedStatus);

    assertThatThrownBy(
            () ->
                testRunService.failTestRun(
                    runWithDisallowedStatus.testRunId(), "FFmpeg process exited unexpectedly"))
        .isInstanceOf(InvalidTestRunStateTransitionException.class)
        .hasMessageContaining(TestRunStatus.FAILED.toString())
        .hasMessageContaining(TestRunStatus.PREPARING.toString())
        .hasMessageContaining(TestRunStatus.STREAMING.toString())
        .hasMessageContaining(TestRunStatus.STOPPING.toString())
        .hasMessageContaining(disallowedStatus.toString())
        .hasMessageContaining(runWithDisallowedStatus.testRunId().toString());
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {"STOPPING"})
  void shouldMarkStopRunFromStoppingStatus(TestRunStatus allowedStatus) {
    TestRun runWithAllowedStatus = TestRunFixtures.testRunWithStatus(allowedStatus);
    repository.save(runWithAllowedStatus);

    TestRun stopped = testRunService.markTestRunStopped(runWithAllowedStatus.testRunId());

    assertThat(stopped.status()).isEqualTo(TestRunStatus.STOPPED);
    assertThat(stopped.finishedAt()).isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
    assertThat(stopped.errorMessage()).isNull();
  }

  @ParameterizedTest
  @EnumSource(
      value = TestRunStatus.class,
      names = {
        "CREATED",
        "FAILED",
        "STOPPED",
        "PREPARING",
        "STREAMING",
      })
  void shouldRejectMarkStopRunFromDisallowedStatus(TestRunStatus disallowedStatus) {
    TestRun runWithDisallowedStatus = TestRunFixtures.testRunWithStatus(disallowedStatus);
    repository.save(runWithDisallowedStatus);

    assertThatThrownBy(() -> testRunService.markTestRunStopped(runWithDisallowedStatus.testRunId()))
        .isInstanceOf(InvalidTestRunStateTransitionException.class)
        .hasMessageContaining(TestRunStatus.STOPPED.toString())
        .hasMessageContaining(TestRunStatus.STOPPING.toString())
        .hasMessageContaining(disallowedStatus.toString())
        .hasMessageContaining(runWithDisallowedStatus.testRunId().toString());
  }

  @Test
  void shouldCompleteSuccessfulTestRunLifecycle() {
    TestRun created = testRunService.createTestRun("camera_sync_test", 2);
    TestRun prepared = testRunService.prepareRun(created.testRunId());
    TestRun streaming = testRunService.startStreaming(prepared.testRunId());
    TestRun stopping = testRunService.stopTestRun(streaming.testRunId());
    TestRun stopped = testRunService.markTestRunStopped(stopping.testRunId());

    assertThat(stopped.status()).isEqualTo(TestRunStatus.STOPPED);
    assertThat(stopped.startedAt()).isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
    assertThat(stopped.finishedAt()).isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
    assertThat(stopped.errorMessage()).isNull();
  }
}
