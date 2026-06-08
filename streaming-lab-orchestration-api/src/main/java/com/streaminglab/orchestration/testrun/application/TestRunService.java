package com.streaminglab.orchestration.testrun.application;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import com.streaminglab.orchestration.testrun.domain.TestRunStatus;
import com.streaminglab.orchestration.testrun.repository.TestRunRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TestRunService {
  private final TestRunRepository repository;

  public TestRunService(TestRunRepository repository) {
    this.repository = repository;
  }

  public TestRun createTestRun(String streamName) {
    Instant now = Instant.now();
    TestRun testRun =
        new TestRun(
            UUID.randomUUID().toString(),
            streamName,
            TestRunStatus.CREATED,
            "",
            "",
            "",
            "",
            now,
            null,
            null,
            null);
    return this.repository.save(testRun);
  }

  public Optional<TestRun> findById(String testRunId) {
    return this.repository.findById(testRunId);
  }
}
