@contract @smoke @playback
Feature: HLS playback validation
  The Streaming Lab test framework must prove that the Display Client can load
  and play the configured HLS stream during an orchestrated test run.

  Rule: Playback evidence must be correlated by the orchestrator-generated testRunId

    Scenario: Validate playback progression for a display client
      Given a new test run exists
      And the test run is prepared
      When the stream is started
      And the display client is opened for the stream
      Then the display client should load the video
      And playback time should progress
      And playback evidence should be written
      And a playback screenshot should be captured
      When the test run is stopped
      Then the test run should be stopped
      And all required artifacts should contain the test run id
