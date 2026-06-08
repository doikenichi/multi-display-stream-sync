package com.streaminglab.orchestration.testrun.repository;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import java.util.Optional;

public interface TestRunRepository {

  TestRun save(TestRun testRun);

  Optional<TestRun> findById(String testRunId);
}
