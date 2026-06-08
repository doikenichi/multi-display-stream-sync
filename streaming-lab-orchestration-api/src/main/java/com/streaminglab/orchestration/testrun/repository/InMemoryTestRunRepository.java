package com.streaminglab.orchestration.testrun.repository;

import com.streaminglab.orchestration.testrun.domain.TestRun;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTestRunRepository implements TestRunRepository {

  private final ConcurrentMap<String, TestRun> testRuns = new ConcurrentHashMap<>();

  @Override
  public TestRun save(TestRun testRun) {
    testRuns.put(testRun.testRunId(), testRun);
    return testRun;
  }

  @Override
  public Optional<TestRun> findById(String testRunId) {
    return Optional.ofNullable(testRuns.get(testRunId));
  }
}
