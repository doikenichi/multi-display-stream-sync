@contract @evidence
Feature: Playback evidence contract
  Every test-framework implementation must produce the same required evidence
  shape so CI, dashboards, and future automated analysis can consume results consistently.

  Rule: Required evidence files must be written using the testRunId

    Scenario: Validate required playback artifacts
      Given playback evidence has been produced for a test run
      Then playback-status.json should match the playback status schema
      And run-summary.json should match the run summary schema
      And playback.log should contain the test run id
      And the artifact directory should contain the test run id
      And at least one playback screenshot should exist
