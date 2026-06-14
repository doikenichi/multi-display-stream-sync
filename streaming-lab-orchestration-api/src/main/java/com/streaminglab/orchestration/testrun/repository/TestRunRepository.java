package com.streaminglab.orchestration.testrun.repository;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import java.util.Optional;
import java.util.UUID;

public interface TestRunRepository {

  TestRun save(TestRun testRun);

  Optional<TestRun> findById(UUID testRunId);
}
