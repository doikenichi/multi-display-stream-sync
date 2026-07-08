package com.streaminglab.testframework.dto;

/** Request DTO for creating a test run through streaming-lab-orchestration-api. */

/**
 * Data Transfer Object (DTO) representing a request to create a new test run in the
 * streaming-lab-orchestration-api.
 *
 * <p>This class encapsulates the parameters required to initiate a test run. It is used by clients
 * to specify the stream details and the number of display clients for the test setup.
 *
 * <p>
 *
 * @param streamName: The unique identifier of the stream for which the test run is being created.
 * @param displayCount: The number of display clients participating in the test run.
 */
public record CreateTestRunRequest(String streamName, Integer displayCount) {}
