package com.streaminglab.orchestration.testrun.application;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.repository.TestRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TestRunService {
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
    return this.repository.save(testRun);
  }

  public Optional<TestRun> findById(UUID testRunId) {
    return this.repository.findById(testRunId);
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
}
