# Test Framework Strategy

## Purpose

This document defines the strategy for building, testing, and evolving the **test automation framework** for the Multi-Display Stream Synchronization Test Lab.

The purpose of this document is to explain **how the framework will be designed, developed, tested, measured, and maintained**.

This document focuses on the framework itself:

```text
Java Test Framework → Test Control API → Browser Automation → Marker Decoding → Assertions → Artifacts
```

The video-streaming validation strategy is covered separately in `video-stream-test-strategy.md`.

---

## Audience

This document is intended for:

* automation engineers;
* QA engineers;
* software engineers;
* senior technical leaders;
* engineering managers;
* reviewers evaluating framework quality and maintainability.

---

## Scope

This document covers:

* framework responsibilities;
* framework boundaries;
* Java-first implementation strategy;
* framework architecture;
* Test Control API integration;
* browser automation strategy;
* marker decoding strategy;
* artifact and report generation strategy;
* unit testing the framework;
* code coverage with JaCoCo;
* EvoSuite usage;
* OpenSpec usage for test and functional coverage;
* development milestones;
* CI and artifact publishing direction;
* future Node/TypeScript and Python extensions.

---

## Out of Scope

This document does not define the detailed pass/fail expectations for the streaming behavior itself.

The following topics belong in `video-stream-test-strategy.md`:

* source consistency rules;
* frame progression rules;
* synchronization tolerance rules;
* frozen display detection behavior;
* latency pass/fail behavior;
* packet-capture expectations;
* streaming-specific test scenarios.

This document explains how the framework will implement and support those validations.

---

## Framework Goals

The framework should be:

| Goal                 | Description                                                               |
| -------------------- | ------------------------------------------------------------------------- |
| Deterministic        | Pass/fail should be based on structured data and explicit rules.          |
| Testable             | Framework logic should have unit and integration tests.                   |
| Maintainable         | Framework code should be modular and easy to change.                      |
| Artifact-first       | Every run should generate useful debugging evidence.                      |
| Configuration-driven | Tests should avoid hardcoded stream URLs and tolerances.                  |
| Protocol-ready       | HLS should be first, but WebRTC should be possible later.                 |
| Language-extensible  | Java is first; Node/TypeScript and Python may be added later.             |
| CI-ready             | Tests should support future GitHub Actions execution and artifact upload. |
| Stakeholder-friendly | Reports should be readable by both engineers and leaders.                 |

---

## Framework Responsibilities

The test framework is responsible for:

* loading shared configuration;
* creating or obtaining a test run ID;
* calling the Test Control API;
* opening browser display clients;
* waiting for display readiness;
* capturing screenshots or rendered frames;
* decoding machine-readable markers;
* validating source consistency;
* validating frame progression;
* calculating synchronization offset;
* detecting frozen displays;
* calculating observed render latency when enabled;
* verifying network-capture artifacts when enabled;
* saving structured artifacts;
* preserving failure stack traces;
* generating the latest demo report;
* exposing clear pass/fail results.

---

## Framework Non-Responsibilities

The framework should not:

* implement the media server;
* replace MediaMTX;
* hide all streaming infrastructure inside test cases;
* hardcode stream URLs in multiple places;
* rely on manual observation for pass/fail;
* rely on Gen AI for pass/fail;
* treat packet capture as a replacement for assertions;
* mix protocol-specific setup directly into assertion logic.

---

## High-Level Framework Flow

Expected validation flow:

```text
1. Load configuration.
2. Create test run ID.
3. Call Test Control API to prepare the run.
4. Start network capture if enabled.
5. Start stream through Test Control API.
6. Open or arm browser display clients.
7. Capture displayed frames.
8. Decode markers.
9. Run deterministic assertions.
10. Save screenshots, marker samples, offsets, latency data, logs, and stack traces.
11. Generate latest report.
12. Stop stream and network capture.
```

The framework should keep test logic readable.

Example test intent:

```text
Given the stream is available
When three display clients render the stream
Then all displays should show the expected source
And frame offset should remain within tolerance
And artifacts should be saved
```

---

## Test Control API Integration

The framework should use a lightweight Test Control API to control lifecycle operations.

The Test Control API should wrap infrastructure actions such as:

* creating a test run;
* preparing deterministic frame sequences;
* starting FFmpeg;
* stopping FFmpeg;
* resolving stream URLs;
* arming display clients;
* starting packet capture;
* stopping packet capture;
* exposing artifact locations.

Suggested endpoints:

```text
POST /api/test-runs
POST /api/test-runs/{testRunId}/prepare
POST /api/test-runs/{testRunId}/displays/arm
POST /api/test-runs/{testRunId}/network-capture/start
POST /api/test-runs/{testRunId}/stream/start
GET  /api/test-runs/{testRunId}/stream/status
POST /api/test-runs/{testRunId}/stream/stop
POST /api/test-runs/{testRunId}/network-capture/stop
GET  /api/test-runs/{testRunId}/artifacts
```

Responsibility boundary:

| Area                     | Test Control API | Test Framework |
| ------------------------ | ---------------- | -------------- |
| Create test run          | Yes              | Calls API      |
| Start FFmpeg             | Yes              | Calls API      |
| Stop FFmpeg              | Yes              | Calls API      |
| Start packet capture     | Yes              | Calls API      |
| Stop packet capture      | Yes              | Calls API      |
| Resolve stream URL       | Yes              | Uses result    |
| Open browser clients     | Optional         | Yes            |
| Capture displayed frames | No               | Yes            |
| Decode markers           | No               | Yes            |
| Calculate frame offset   | No               | Yes            |
| Calculate latency        | No               | Yes            |
| Decide pass/fail         | No               | Yes            |
| Generate final report    | Optional support | Yes            |

The API controls lifecycle.

The framework validates behavior.

---

## Java-First Implementation Strategy

Java is the first implementation language for the framework.

Java should handle:

* JUnit 5 test execution;
* shared configuration loading;
* API calls to Test Control API;
* Playwright for Java browser automation;
* screenshot or frame capture;
* marker decoding integration;
* synchronization analysis;
* latency analysis;
* artifact writing;
* failure stack trace preservation;
* HTML report generation.

Recommended Java project location:

```text
test-frameworks/java/
```

Recommended package direction:

```text
test-frameworks/java/
  src/main/java/com/streamsync/
    config/
    control/
    display/
    capture/
    marker/
    sync/
    latency/
    network/
    artifacts/
    report/
    protocol/
    util/

  src/test/java/com/streamsync/
    config/
    control/
    marker/
    sync/
    latency/
    artifacts/
    report/
```

---

## Core Framework Abstractions

| Class or Interface       | Responsibility                                                           |
| ------------------------ | ------------------------------------------------------------------------ |
| `ConfigLoader`           | Loads shared YAML configuration.                                         |
| `TestRunContext`         | Holds test run ID, source ID, protocol, display IDs, and artifact paths. |
| `TestControlClient`      | Calls Test Control API endpoints.                                        |
| `DisplayClient`          | Represents one browser-based virtual display.                            |
| `DisplayManager`         | Opens and manages multiple display clients.                              |
| `FrameCapture`           | Captures screenshot or rendered frame from a display.                    |
| `MarkerDecoder`          | Extracts source ID, test run ID, frame number, and timestamps.           |
| `SyncAnalyzer`           | Calculates frame offsets and synchronization status.                     |
| `FrozenDisplayDetector`  | Detects stale or frozen display behavior.                                |
| `LatencyAnalyzer`        | Calculates observed render latency.                                      |
| `FirstFramePolicy`       | Validates frame 0/frame 1 requirements for latency mode.                 |
| `NetworkCaptureVerifier` | Verifies capture metadata and packet-capture artifact presence.          |
| `ArtifactCollector`      | Writes screenshots, JSON, CSV, logs, stack traces, and raw samples.      |
| `ReportGenerator`        | Generates `artifacts/latest/index.html`.                                 |
| `ProtocolAdapter`        | Abstracts protocol-specific behavior.                                    |
| `HlsProtocolAdapter`     | Supports the first HLS implementation.                                   |
| `WebRtcProtocolAdapter`  | Future protocol extension.                                               |

---

## Configuration Strategy

The framework should be configuration-driven.

Example configuration:

```yaml
testRun:
  sourceId: CAMERA_SYNC_TEST
  displays:
    - DISPLAY_01
    - DISPLAY_02
    - DISPLAY_03
  durationSeconds: 60
  sampleIntervalMs: 500
  toleranceFrames: 3

stream:
  protocol: HLS
  hlsUrl: http://localhost:8888/camera_sync_test/index.m3u8
  fps: 30

latency:
  enabled: true
  requireFirstFrame: true
  maxAllowedFirstFrame: 1

network:
  mode: docker
  dockerNetwork: stream-sync-net
  captureEnabled: true
  impairmentEnabled: false

artifacts:
  outputDir: artifacts
  latestDir: artifacts/latest
```

The framework should avoid hardcoding:

* stream URLs;
* display IDs;
* protocol names;
* tolerances;
* sample intervals;
* artifact paths;
* packet-capture settings.

---

## Browser Automation Strategy

The first implementation should use Playwright for Java.

The framework should use browser automation to:

* open display clients;
* pass display ID and source information;
* wait for page readiness;
* wait for video element readiness;
* capture screenshots or rendered frames;
* collect browser-side diagnostics where useful.

The framework should avoid making pass/fail decisions based only on browser events such as “page loaded” or “video element exists.”

Browser readiness is not the same as stream correctness.

The actual validation should use decoded frame markers from displayed output.

---

## Marker Decoding Strategy

The target marker strategy uses machine-readable markers as the primary oracle.

The framework should decode marker metadata such as:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_001",
  "frame": 184,
  "generatedAt": "2026-06-05T14:00:00.000Z",
  "streamStartEpochMs": 1780689600000,
  "fps": 30,
  "sequenceId": "SEQ_001"
}
```

Marker decoding should support:

* source consistency validation;
* test run ID validation;
* frame progression validation;
* synchronization offset calculation;
* latency calculation;
* stale content detection.

Human-readable text in the video should be treated as debugging support, not the primary automation oracle.

---

## Assertion Strategy

Assertions should be deterministic and clear.

The framework should produce explicit failure reasons such as:

```text
DISPLAY_02 source mismatch:
  expected: CAMERA_SYNC_TEST
  actual: CAMERA_OTHER
```

```text
Synchronization offset exceeded tolerance:
  tolerance: 3 frames
  actual: 7 frames
```

```text
Latency first-frame requirement failed:
  max allowed first frame: 1
  first observed frame: 42
```

```text
Frozen display detected:
  display: DISPLAY_03
  stale duration: 5 seconds
  repeated frame: 450
```

Assertions should be organized around domain-specific analyzers, not scattered across test methods.

---

## Artifact Strategy

The framework should write persistent artifacts for every run.

Recommended output structure:

```text
artifacts/
  runs/
    {testRunId}/
      summary.json
      offsets.csv
      latency.csv
      failure-summary.md
      failure-stacktrace.txt
      screenshots/
      logs/
      raw/
        marker-samples.jsonl
      network/

  latest/
    index.html
    summary.json
    offsets.csv
    latency.csv
    failure-summary.md
    failure-stacktrace.txt
    screenshots/
    logs/
    raw/
    network/
```

The framework should write artifacts even when the test fails.

The framework should preserve:

* screenshots;
* decoded marker samples;
* offset calculations;
* latency calculations;
* packet-capture references;
* framework logs;
* FFmpeg logs where available;
* MediaMTX logs where available;
* Test Control API logs where available;
* stack traces;
* human-readable failure summary;
* latest HTML report.

---

## Report Generation Strategy

The latest report should be generated at:

```text
artifacts/latest/index.html
```

The report should be readable by both technical and non-technical stakeholders.

It should include:

* test run ID;
* source ID;
* protocol;
* network mode;
* display IDs;
* overall result;
* source consistency result;
* frame progression result;
* synchronization result;
* frozen display status;
* max frame offset;
* average frame offset;
* tolerance;
* latency results when enabled;
* first observed frames when enabled;
* packet-capture links when enabled;
* screenshots;
* failure summary;
* stack trace link;
* raw artifact links.

---

## Framework Test Strategy

The framework itself must be tested.

The goal is to avoid building a test framework that is only validated indirectly through end-to-end demos.

Framework test levels include:

* unit tests;
* component tests;
* integration tests;
* end-to-end framework validation.


## Separation Between Framework Tests and Video Validation Tests

The project must clearly separate tests that validate the automation framework from tests that validate video-streaming behavior.

These two test categories are related, but they serve different purposes and should not be treated as one mixed test suite.

```text
Framework tests validate the automation tool.
Video validation tests use the automation tool to validate the streaming system.
```

### Framework Verification Tests

Framework verification tests prove that the test framework code works correctly.

These tests should validate framework logic without requiring the full streaming lab to run.

They should focus on:

* configuration loading and validation;
* marker payload parsing;
* synchronization offset calculation;
* frozen display detection;
* latency calculation;
* first-frame policy validation;
* artifact path creation;
* report model generation;
* Test Control API client behavior using mocks or test doubles;
* network-capture metadata verification.

Framework verification tests should normally run as part of the standard Java build.

Recommended command:

```bash
mvn clean verify
```

These tests should produce JaCoCo coverage because they exercise framework implementation logic.

### Video Streaming Validation Tests

Video streaming validation tests use the framework to validate the actual streaming lab behavior.

These tests require the streaming environment or a close equivalent, including:

* Streaming Lab Orchestration API;
* generated frame sequences;
* FFmpeg;
* MediaMTX;
* HLS endpoint;
* browser display clients;
* optional network-capture container or agent.

These tests validate product-like streaming behavior such as:

* HLS stream availability;
* source consistency;
* active test run ID consistency;
* frame progression;
* multi-display synchronization;
* frozen display detection;
* latency first-frame capture;
* packet-capture artifact generation.

Video validation tests should not be used as the main way to prove that framework helper classes work. They prove that the complete lab scenario works when executed by the framework.

### Recommended Test Source Layout

The Java test project should use separate test source sets or clearly separated packages.

Recommended layout:

```text
test-frameworks/java/
  src/
    main/java/com/streamsync/
      config/
      control/
      display/
      capture/
      marker/
      sync/
      latency/
      network/
      artifacts/
      report/
      protocol/
      util/

    test/java/com/streamsync/
      config/
      marker/
      sync/
      latency/
      artifacts/
      report/
      network/
      control/

    integrationTest/java/com/streamsync/
      control/
      display/
      artifacts/
      report/

    e2eTest/java/com/streamsync/
      video/
        HlsStreamAvailabilityTest.java
        SourceConsistencyTest.java
        FrameProgressionTest.java
        MultiDisplaySynchronizationTest.java
        FrozenDisplayDetectionTest.java
        LatencyFirstFrameCaptureTest.java
        NetworkCaptureArtifactTest.java
```

If the project starts with only Maven default folders, the first implementation can still separate the tests by package and naming convention:

```text
src/test/java/com/streamsync/framework/...
src/test/java/com/streamsync/video/...
```

However, the preferred long-term structure is to use separate source sets so that framework verification and video validation can be run independently in CI.

### Recommended Naming

Use explicit names to avoid confusion:

| Test Category | Purpose |
|---|---|
| Framework Verification Tests | Validate the automation framework itself. |
| Video Streaming Validation Tests | Use the framework to validate streaming behavior. |
| Framework Unit Tests | Validate isolated framework logic. |
| Framework Component Tests | Validate framework modules with mocks or sample files. |
| Framework Integration Tests | Validate framework integration with API/browser/test doubles. |
| Streaming E2E Tests | Validate the full streaming lab scenario. |

### Execution Boundary

Framework verification tests should be fast and stable enough to run on every pull request.

Video streaming validation tests are heavier. They may require Docker, browsers, generated frames, FFmpeg, MediaMTX, network capture permissions, and artifact upload. They should run in a separate workflow or separate CI job from framework verification.

---

## Unit Testing the Framework

Unit tests should validate deterministic framework logic without requiring Docker, FFmpeg, MediaMTX, or browsers.

Unit test targets include:

* configuration loading;
* configuration validation;
* marker payload parsing;
* source comparison;
* frame offset calculation;
* frozen display detection;
* latency calculation;
* first-frame policy;
* artifact path generation;
* report model generation;
* network capture metadata validation;
* protocol adapter selection.

Example unit test cases:

| Area                     | Example                                                                |
| ------------------------ | ---------------------------------------------------------------------- |
| `SyncAnalyzer`           | Calculates max offset from display frame samples.                      |
| `LatencyAnalyzer`        | Calculates observed render latency from marker and capture timestamps. |
| `FirstFramePolicy`       | Fails when required frame 0 or frame 1 is not observed.                |
| `FrozenDisplayDetector`  | Detects repeated frame values across timeout window.                   |
| `MarkerPayloadParser`    | Parses valid marker JSON and rejects malformed data.                   |
| `ConfigLoader`           | Loads default tolerance, stream URL, and latency settings.             |
| `ArtifactCollector`      | Creates expected run, latest, and network directories.                 |
| `NetworkCaptureVerifier` | Validates required `.pcap` and metadata file presence.                 |

Unit tests should be fast, deterministic, and runnable during normal development.

---

## Component Testing the Framework

Component tests should validate framework modules with controlled dependencies.

Examples:

| Component                | Test Focus                                      |
| ------------------------ | ----------------------------------------------- |
| Test Control Client      | Calls mock API endpoints correctly.             |
| Display Manager          | Opens configured display URLs.                  |
| Artifact Collector       | Writes expected file structure.                 |
| Report Generator         | Produces valid HTML from a summary model.       |
| Marker Decoder           | Decodes known sample marker images.             |
| Network Capture Verifier | Confirms capture artifacts exist when expected. |

Component tests may use local test doubles, mock servers, or sample image files.

---

## Integration Testing the Framework

Integration tests should validate framework interaction with real or near-real components.

Initial integration tests should verify:

* framework can call Test Control API;
* framework can open a browser display client;
* framework can capture a screenshot;
* framework can save artifacts;
* framework can detect stream availability;
* framework can verify network-capture files when enabled.

Integration tests may require Docker services.

They should remain smaller and more targeted than full end-to-end streaming tests.

---

## End-to-End Framework Validation

End-to-end framework validation verifies that the framework can execute a full streaming scenario and produce expected artifacts.

The first full validation should cover:

```text
Test Control API
  → FFmpeg
  → MediaMTX
  → HLS
  → Browser Display Clients
  → Java Test Framework
  → Artifacts and Report
```

End-to-end framework validation should prove that the framework can:

* orchestrate the scenario;
* capture display output;
* decode markers;
* run assertions;
* write artifacts;
* generate a report;
* fail clearly when behavior is invalid.

---

## Code Coverage Strategy with JaCoCo

JaCoCo should be used to measure Java framework unit test coverage.

Coverage should focus on meaningful deterministic logic rather than only producing a high percentage.

Recommended coverage expectations:

| Area                                 | Coverage Expectation                                               |
| ------------------------------------ | ------------------------------------------------------------------ |
| Synchronization logic                | High coverage required.                                            |
| Latency calculation logic            | High coverage required.                                            |
| First-frame policy logic             | High coverage required.                                            |
| Frozen display detection             | High coverage required.                                            |
| Marker payload parsing               | High coverage required.                                            |
| Config validation                    | High coverage required.                                            |
| Artifact path and report model logic | Medium to high coverage required.                                  |
| Network capture metadata logic       | Medium to high coverage required.                                  |
| Browser automation wrappers          | Lower unit coverage acceptable; covered more by integration tests. |
| External process orchestration       | Lower unit coverage acceptable; use component/integration tests.   |

Recommended Maven plugin direction:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>verify</phase>
      <goals>
        <goal>report</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Recommended command:

```bash
mvn clean verify
```

Expected report path:

```text
test-frameworks/java/target/site/jacoco/index.html
```

The JaCoCo report should be saved as an artifact in CI.

---

## EvoSuite Strategy

EvoSuite may be used to generate additional unit tests for deterministic Java framework classes.

Recommended EvoSuite use cases:

* synchronization calculation edge cases;
* latency calculation boundaries;
* first-frame policy boundaries;
* malformed marker payload parsing;
* configuration validation boundaries;
* artifact path utilities;
* pure utility classes.

EvoSuite should not be used as the main source of test design for:

* browser automation;
* streaming behavior;
* Test Control API behavior;
* network packet capture;
* stakeholder-facing acceptance criteria.

Generated tests should be reviewed before they are committed.

Policy:

```text
EvoSuite can increase coverage, but committed tests must remain readable, stable, and relevant.
```

---

## OpenSpec Strategy

OpenSpec should be used to document functional expectations and map them to automated test coverage.

The framework should support OpenSpec as a traceability mechanism between:

* functional streaming requirements;
* framework test cases;
* test levels;
* automation status;
* coverage status.

OpenSpec should answer:

* What behavior is expected?
* Which automated test validates it?
* Which framework module supports it?
* What test level covers it?
* Is coverage planned, partial, automated, manual, or future?

Example functional areas:

```text
stream-source-consistency
frame-progression
multi-display-synchronization
frozen-display-detection
latency-measurement
first-frame-capture
network-traffic-capture
artifact-generation
report-generation
framework-unit-coverage
framework-report-generation
```

Example requirement entry:

```yaml
id: STREAM-SYNC-001
name: Source consistency
description: Each display must render the expected source for the active test run.
priority: high
deterministic: true
```

Example coverage mapping:

```yaml
coverage:
  requirementId: STREAM-SYNC-001
  automatedTests:
    - MultiDisplaySyncTest.shouldRenderExpectedSourceOnAllDisplays
    - MarkerPayloadParserTest.shouldRejectWrongSource
  frameworkModules:
    - MarkerDecoder
    - SourceConsistencyValidator
  testLevel:
    - unit
    - end-to-end
  framework:
    - java
  status: planned
```

Coverage status values:

| Status    | Meaning                                             |
| --------- | --------------------------------------------------- |
| planned   | Requirement exists but test is not implemented yet. |
| automated | Requirement has automated test coverage.            |
| partial   | Some behavior is covered, but not all.              |
| manual    | Behavior is currently checked manually.             |
| future    | Behavior is intentionally deferred.                 |

---

## CI/CD Strategy

The project should use separate CI/CD workflows for framework verification and video streaming validation.

This separation is important because framework tests and video validation tests have different goals, dependencies, execution cost, and failure modes.

```text
CI/CD workflow 1: test the framework
CI/CD workflow 2: run streaming end-to-end validation
```

The framework workflow should answer:

```text
Is the automation framework implementation healthy?
```

The streaming E2E workflow should answer:

```text
Can the framework validate the streaming lab behavior in a real environment?
```

---

## CI/CD Workflow 1: Framework Verification

The framework verification workflow should run on pull requests and pushes to the main branch.

Its purpose is to validate the Java framework code without requiring the complete streaming lab.

### Recommended Trigger

```yaml
on:
  pull_request:
  push:
    branches:
      - main
```

### Responsibilities

This workflow should:

* check out the repository;
* set up the required JDK;
* build the Java test framework;
* run framework unit tests;
* run framework component tests where possible;
* generate JaCoCo coverage;
* publish test reports;
* publish JaCoCo coverage reports;
* fail quickly when deterministic framework logic is broken.

### Tests Included

This workflow should include:

| Test Type | Included? | Notes |
|---|---:|---|
| Framework unit tests | Yes | Main purpose of this workflow. |
| Framework component tests | Yes | Use mocks, sample files, or local test doubles. |
| Framework integration tests | Optional | Include only if stable and lightweight. |
| Video streaming E2E tests | No | Run in separate workflow. |
| Network packet-capture tests | No | Run in streaming E2E workflow. |

### Example Maven Command

```bash
mvn clean verify
```

If separate Maven profiles are used:

```bash
mvn clean verify -Pframework-tests
```

### Recommended Artifacts

This workflow should upload:

```text
test-frameworks/java/target/site/jacoco/
test-frameworks/java/target/surefire-reports/
test-frameworks/java/target/failsafe-reports/
```

Artifacts should be uploaded even when tests fail.

Example GitHub Actions behavior:

```yaml
if: always()
```

### Success Criteria

The workflow should pass when:

* Java framework compiles successfully;
* unit tests pass;
* component tests pass;
* JaCoCo report is generated;
* required test report artifacts are produced.

The workflow should fail when:

* framework code does not compile;
* deterministic framework tests fail;
* JaCoCo report cannot be generated;
* required test artifacts are missing.

---

## CI/CD Workflow 2: Video Streaming End-to-End Validation

The video streaming E2E workflow should run the full lab scenario or a reduced CI-safe version of it.

Its purpose is to validate that the framework can execute real streaming tests against the lab infrastructure.

This workflow is heavier than the framework verification workflow because it may require:

* Docker Compose;
* Streaming Lab Orchestration API;
* generated frame sequences;
* FFmpeg;
* MediaMTX;
* display client web app;
* browser automation;
* HLS playback;
* artifact generation;
* optional packet capture.

### Recommended Trigger

The streaming E2E workflow may run on:

* manual dispatch;
* nightly schedule;
* push to main;
* selected pull requests when streaming-related files change.

Recommended initial trigger:

```yaml
on:
  workflow_dispatch:
  push:
    branches:
      - main
```

A scheduled nightly run may be added later:

```yaml
on:
  schedule:
    - cron: "0 8 * * *"
```

### Responsibilities

This workflow should:

* check out the repository;
* set up Java;
* set up Docker Compose;
* build or pull required containers;
* start MediaMTX;
* start the Streaming Lab Orchestration API;
* generate a run-specific frame sequence;
* start FFmpeg through the orchestration API;
* wait for the HLS endpoint to become ready;
* run browser-based video validation tests;
* collect screenshots, decoded markers, offsets, latency data, logs, and network artifacts;
* upload artifacts even when tests fail.

### Tests Included

This workflow should include:

| Test Type | Included? | Notes |
|---|---:|---|
| Framework unit tests | Optional | Already covered by framework workflow. |
| Framework component tests | Optional | Not the main goal. |
| HLS stream availability test | Yes | Validates stream readiness. |
| Source consistency test | Yes | Validates expected source and test run ID. |
| Frame progression test | Yes | Validates frames advance over time. |
| Multi-display synchronization test | Yes | Validates frame offset tolerance. |
| Frozen display detection test | Yes | Validates stale display detection. |
| Latency first-frame test | Yes, once stable | Validates first observed frame policy. |
| Network capture artifact test | Optional initially | Enable after capture container is stable. |

### Example Maven Command

If using a Maven profile:

```bash
mvn verify -Pe2e-tests
```

If using a separate source set:

```bash
mvn verify -Dgroups=e2e
```

The exact command may evolve, but the execution path should remain separate from framework verification tests.

### Recommended Artifacts

This workflow should upload:

```text
artifacts/
test-frameworks/java/target/failsafe-reports/
test-frameworks/java/target/playwright-report/
```

The `artifacts/` folder should include:

```text
artifacts/
  runs/
    {testRunId}/
      summary.json
      offsets.csv
      latency.csv
      failure-summary.md
      failure-stacktrace.txt
      screenshots/
      logs/
      raw/
        marker-samples.jsonl
      network/
      generated-frames/
      source-sequence/
        sequence-manifest.json

  latest/
    index.html
    summary.json
    offsets.csv
    latency.csv
    screenshots/
    logs/
    raw/
    network/
```

Artifacts should be uploaded with `if: always()` so failed runs still preserve evidence.

### Success Criteria

The workflow should pass when:

* Docker services start successfully;
* frame sequence generation succeeds;
* FFmpeg publishes to MediaMTX;
* HLS endpoint becomes ready;
* browser display clients load the stream;
* video validation tests pass;
* latest report is generated;
* required artifacts are uploaded.

The workflow should fail when:

* the orchestration API cannot prepare the run;
* generated frames or manifest are missing;
* FFmpeg fails to start;
* MediaMTX does not expose the HLS endpoint;
* browser display clients cannot render the stream;
* source consistency fails;
* frame progression fails;
* synchronization exceeds tolerance;
* first-frame policy fails in latency mode;
* required evidence artifacts are missing.

---

## CI/CD Workflow Separation Summary

| Workflow | Purpose | Runs On | Requires Docker/Streaming? | Main Artifacts |
|---|---|---|---:|---|
| Framework Verification | Validate framework code quality and deterministic logic. | Pull requests and main branch pushes. | No, or minimal. | Surefire/Failsafe reports, JaCoCo report. |
| Video Streaming E2E Validation | Validate actual streaming lab behavior using the framework. | Manual, main branch, scheduled, or selected PRs. | Yes. | Streaming artifacts, screenshots, logs, marker samples, latest report, optional pcap. |

The framework verification workflow should be fast and reliable.

The video streaming E2E workflow may be slower and more infrastructure-dependent, so it should be isolated from the framework verification workflow.

This separation prevents infrastructure failures from hiding framework unit-test failures and prevents framework changes from requiring full video validation on every small edit.


---

## Development Strategy

The framework should be built incrementally.

Recommended implementation order:

1. Create Java framework project structure.
2. Add configuration model and loader.
3. Add unit tests for configuration.
4. Add artifact path model.
5. Add artifact collector.
6. Add unit tests for artifact paths and file creation.
7. Add marker payload model and parser.
8. Add unit tests for marker parsing.
9. Add synchronization analyzer.
10. Add unit tests for frame offset calculation.
11. Add frozen display detector.
12. Add unit tests for frozen display detection.
13. Add latency analyzer.
14. Add unit tests for latency calculation.
15. Add first-frame policy.
16. Add unit tests for first-frame rules.
17. Add JaCoCo coverage reporting.
18. Add Test Control API client.
19. Add component tests with mock API.
20. Add Playwright browser wrapper.
21. Add screenshot or frame capture.
22. Add report model.
23. Add HTML report generator.
24. Add unit/component tests for report generation.
25. Add network capture artifact verifier.
26. Add OpenSpec coverage mapping.
27. Add HLS end-to-end test.
28. Add latency mode end-to-end test.
29. Add network capture verification.
30. Add CI/CD workflow for framework verification tests.
31. Add CI/CD workflow for video streaming E2E validation.
32. Add CI artifact upload for both workflows.

This order builds the deterministic framework core before relying on full streaming end-to-end tests.

---

## Future Node/TypeScript Extension

Node/TypeScript may be added later because Playwright is especially mature in the Node ecosystem.

A future Node framework should:

* consume the same shared configuration;
* use the same artifact format;
* validate the same scenarios;
* produce comparable reports;
* support browser-heavy experimentation.

Possible structure:

```text
test-frameworks/node/
  package.json
  playwright.config.ts
  src/
    config/
    control/
    display/
    capture/
    marker/
    sync/
    latency/
    artifacts/
    report/
  tests/
    multi-display-sync.spec.ts
```

---

## Future Python Extension

Python may be added later for image processing, computer vision, and artifact analysis.

A future Python framework or utility layer may support:

* OpenCV-based frame analysis;
* QR or marker decoding;
* packet-capture post-processing;
* CSV/JSON artifact analysis;
* latency analysis;
* future capture-card validation.

Possible structure:

```text
test-frameworks/python/
  pyproject.toml
  pytest.ini
  src/
    streamsync/
      config/
      marker/
      sync/
      latency/
      artifacts/
      network/
      report/
  tests/
    test_sync_analyzer.py
    test_latency_analyzer.py
```

Python should consume the same configuration and produce the same artifact schema where possible.

---

## Gen AI Boundary

Gen AI may be added later as an optional post-run analysis layer.

The framework may eventually use Gen AI to:

* summarize failures;
* explain packet-capture findings;
* classify likely root causes;
* generate stakeholder-friendly summaries;
* compare current failures with previous runs.

Gen AI must not decide pass/fail.

Pass/fail must remain deterministic and based on framework assertions.

Boundary:

| Area                     | Deterministic Framework       | Future Gen AI         |
| ------------------------ | ----------------------------- | --------------------- |
| Source consistency       | Yes                           | No                    |
| Frame progression        | Yes                           | No                    |
| Frame offset calculation | Yes                           | No                    |
| Frozen display detection | Yes                           | No                    |
| First-frame policy       | Yes                           | No                    |
| Latency calculation      | Yes                           | No                    |
| Artifact existence       | Yes                           | No                    |
| Failure explanation      | Basic summary                 | Enhanced summary      |
| Root cause hypothesis    | Optional basic classification | Suggested explanation |

---

## Framework Risks and Mitigations

| Risk                                        | Impact                                      | Mitigation                                              |
| ------------------------------------------- | ------------------------------------------- | ------------------------------------------------------- |
| Framework becomes too coupled to HLS        | WebRTC becomes harder to add.               | Use protocol adapters.                                  |
| Tests become infrastructure-heavy           | Test methods become hard to read.           | Use Test Control API and domain abstractions.           |
| Browser automation becomes flaky            | Results may be unstable.                    | Separate readiness checks from marker-based validation. |
| Unit coverage focuses on wrong areas        | High coverage may not mean high confidence. | Prioritize deterministic logic coverage.                |
| Artifact generation is inconsistent         | Failures become hard to debug.              | Standardize artifact schema early.                      |
| Packet capture requires special permissions | Local setup becomes difficult.              | Use dedicated capture container.                        |
| EvoSuite tests become noisy                 | Suite becomes hard to maintain.             | Review generated tests before committing.               |
| Gen AI overstates conclusions               | Stakeholders may misinterpret results.      | Keep Gen AI outside pass/fail decisions.                |

---

## Summary

This document defines the strategy for building and testing the automation framework.

The framework should be Java-first, deterministic, modular, artifact-first, and configuration-driven. It should validate streaming behavior through decoded markers, timestamps, frame numbers, browser captures, and structured artifacts.

The framework itself should be treated as production-quality software. Its core logic should be unit tested, measured with JaCoCo, and supplemented with EvoSuite where appropriate. OpenSpec should be used to map requirements to automated test coverage.

The result should be a maintainable framework that can support the initial HLS proof of concept and later evolve toward WebRTC, network degradation testing, Python analysis, Node/TypeScript browser automation, and Gen AI-assisted post-run analysis.
