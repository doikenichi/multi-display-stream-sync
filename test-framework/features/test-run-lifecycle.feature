@contract @orchestration-api @lifecycle
Feature: Test run lifecycle
  The Streaming Lab test framework must use streaming-lab-orchestration-api
  as the source of truth for test-run lifecycle state.

  Rule: The orchestrator owns lifecycle transitions

    Scenario: Complete the happy-path test run lifecycle
      Given a new test run exists
      Then the test run status should be CREATED
      When the test run is prepared
      Then the test run status should be PREPARING
      When the stream is started
      Then the test run status should be STREAMING
      When the test run is stopped
      Then the test run status should be STOPPED

    Scenario: A missing test run cannot be retrieved
      When a missing test run is requested
      Then the orchestrator should reject the request as not found
