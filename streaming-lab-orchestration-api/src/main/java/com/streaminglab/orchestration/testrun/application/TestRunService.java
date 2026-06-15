package com.streaminglab.orchestration.testrun.application;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.repository.TestRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TestRunService {
  private static final Logger log = LoggerFactory.getLogger(TestRunService.class);
  public static final List<TestRunStatus> PREPARED_VALID_STATUS_TRANSITIONS =
      List.of(TestRunStatus.CREATED, TestRunStatus.FAILED, TestRunStatus.STOPPED);
  public static final List<TestRunStatus> STREAM_VALID_STATUS_TRANSITIONS =
      List.of(TestRunStatus.PREPARING);
  public static final List<TestRunStatus> STOPPING_VALID_STATUS_TRANSITIONS =
      List.of(TestRunStatus.STREAMING);
  public static final List<TestRunStatus> STOPPED_VALID_STATUS_TRANSITIONS =
      List.of(TestRunStatus.STOPPING);
  public static final List<TestRunStatus> FAILED_VALID_STATUS_TRANSITIONS =
      List.of(TestRunStatus.PREPARING, TestRunStatus.STREAMING, TestRunStatus.STOPPING);

  private final TestRunRepository repository;
  private final Clock clock;

  public TestRunService(TestRunRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public TestRun createTestRun(String streamName, int displayCount) {
    UUID testRunId = UUID.randomUUID();
    Instant now = Instant.now(this.clock);
    TestRun testRun =
        new TestRun(
            testRunId,
            streamName,
            displayCount,
            TestRunStatus.CREATED,
            buildHlsInternalUrl(streamName),
            buildHlsExternalUrl(streamName),
            buildRtspPublishUrl(streamName),
            buildArtifactPath(testRunId),
            now,
            null,
            null,
            null);
    TestRun created = this.repository.save(testRun);
    log.info("Test run created: testRunId={}", testRunId);
    return created;
  }

  public TestRun findById(UUID testRunId) {
    return this.repository
        .findById(testRunId)
        .orElseThrow(() -> new TestRunNotFoundException(testRunId));
  }

  private String buildHlsInternalUrl(String streamName) {
    return "http://mediamtx:8888/" + streamName + "/";
  }

  private String buildHlsExternalUrl(String streamName) {
    return "http://localhost:8888/" + streamName + "/";
  }

  private String buildRtspPublishUrl(String streamName) {
    return "rtsp://mediamtx:8554/" + streamName;
  }

  private String buildArtifactPath(UUID testRunId) {
    return "artifacts/test-runs/" + testRunId;
  }

  public TestRun prepareRun(UUID testRunId) {
    TestRun testRun = this.findById(testRunId);

    if (!canPrepare(testRun.status())) {
      log.warn(
          "Invalid test run transition: testRunId={}, currentStatus={}, requestedAction={}",
          testRunId,
          testRun.status(),
          "stream");
      throw new InvalidTestRunStateTransitionException(
          testRun.testRunId(),
          testRun.status(),
          TestRunStatus.PREPARING,
          PREPARED_VALID_STATUS_TRANSITIONS);
    }
    TestRun prepared = this.repository.save(testRun.prepare());
    log.info("Test run prepared: testRunId={}", testRunId);
    return prepared;
  }

  private boolean canPrepare(TestRunStatus status) {
    return status == TestRunStatus.CREATED
        || status == TestRunStatus.FAILED
        || status == TestRunStatus.STOPPED;
  }

  public TestRun startStreaming(UUID testRunId) {
    TestRun testRun = this.findById(testRunId);

    if (testRun.status() != TestRunStatus.PREPARING) {
      log.warn(
          "Invalid test run transition: testRunId={}, currentStatus={}, requestedAction={}",
          testRunId,
          testRun.status(),
          "stream");
      throw new InvalidTestRunStateTransitionException(
          testRun.testRunId(),
          testRun.status(),
          TestRunStatus.STREAMING,
          STREAM_VALID_STATUS_TRANSITIONS);
    }
    TestRun streaming = this.repository.save(testRun.start(Instant.now(clock)));
    log.info("Test run started streaming: testRunId={}", testRunId);
    return streaming;
  }

  public TestRun stopTestRun(UUID testRunId) {
    TestRun testRun = this.findById(testRunId);

    if (testRun.status() != TestRunStatus.STREAMING) {
      log.warn(
          "Invalid test run transition: testRunId={}, currentStatus={}, requestedAction={}",
          testRunId,
          testRun.status(),
          "stream");
      throw new InvalidTestRunStateTransitionException(
          testRun.testRunId(),
          testRun.status(),
          TestRunStatus.STOPPING,
          STOPPING_VALID_STATUS_TRANSITIONS);
    }
    TestRun stopping = this.repository.save(testRun.requestStop());
    log.info("Test run stopping: testRunId={}", testRunId);
    return stopping;
  }

  public TestRun markTestRunStopped(UUID testRunId) {
    TestRun testRun = this.findById(testRunId);

    if (testRun.status() != TestRunStatus.STOPPING) {
      log.warn(
          "Invalid test run transition: testRunId={}, currentStatus={}, requestedAction={}",
          testRunId,
          testRun.status(),
          "stream");
      throw new InvalidTestRunStateTransitionException(
          testRun.testRunId(),
          testRun.status(),
          TestRunStatus.STOPPED,
          STOPPED_VALID_STATUS_TRANSITIONS);
    }

    TestRun stopped = this.repository.save(testRun.markStopped(Instant.now(clock)));
    log.info("Test run stopped: testRunId={}", testRunId);
    return stopped;
  }

  public TestRun failTestRun(UUID testRunId, String errorMessage) {
    TestRun testRun = this.findById(testRunId);

    if (!canFail(testRun.status())) {
      log.warn(
          "Invalid test run transition: testRunId={}, currentStatus={}, requestedAction={}",
          testRunId,
          testRun.status(),
          "stream");
      throw new InvalidTestRunStateTransitionException(
          testRun.testRunId(),
          testRun.status(),
          TestRunStatus.FAILED,
          FAILED_VALID_STATUS_TRANSITIONS);
    }
    TestRun failed = this.repository.save(testRun.fail(Instant.now(clock), errorMessage));
    log.warn(
        "Test run failed: testRunId={}, statusBeforeFailure={}, errorMessage={}",
        testRunId,
        testRun.status(),
        errorMessage);
    return failed;
  }

  private boolean canFail(TestRunStatus status) {
    return status == TestRunStatus.PREPARING
        || status == TestRunStatus.STREAMING
        || status == TestRunStatus.STOPPING;
  }
}
