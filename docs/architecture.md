# Architecture

## Purpose

This document describes the architecture for the **Multi-Display Stream Synchronization Test Lab**.

The project is a proof-of-concept test automation lab for validating whether multiple network-connected display clients can receive, render, and remain synchronized on the same streaming video source.

The architecture is intentionally designed to be:

* local and reproducible;
* network-based rather than OS-only;
* focused on streaming video over IP/HTTP-style networks;
* test-framework friendly;
* test-result aware;
* extensible from HLS to WebRTC;
* extensible from Java to Node/TypeScript and Python;
* focused on measurable and evidence-based video-streaming behavior.

---

## What Are We Building?

We are building a test automation lab that simulates a simplified streaming-video distribution system.

The system generates a controlled synthetic video stream, publishes it through a media server, renders it in multiple browser-based display clients, and uses automated tests to validate synchronization behavior.

At a high level, the project answers this question:

> If the same video stream is delivered to multiple display clients over a network, can automation verify that all displays show the correct stream, continue rendering frames, and remain synchronized within an acceptable tolerance?

The first version focuses on:

* synthetic video generation;
* HLS streaming;
* Docker-based network simulation;
* browser-based virtual display clients;
* Java-first test automation;
* persistent artifact storage;
* human-readable demo reports;
* future support for Node/TypeScript, Python, and WebRTC.

---

## Architecture Summary

```mermaid
flowchart TD
    A[FFmpeg Synthetic Stream Generator] -->|Publishes video stream| B[MediaMTX Media Server]
    B -->|Serves HLS over HTTP| C[Display Client 01]
    B -->|Serves HLS over HTTP| D[Display Client 02]
    B -->|Serves HLS over HTTP| E[Display Client 03]

    F[Test Framework] -->|Opens and controls| C
    F -->|Opens and controls| D
    F -->|Opens and controls| E

    C -->|Rendered frame or screenshot| F
    D -->|Rendered frame or screenshot| F
    E -->|Rendered frame or screenshot| F

    F --> G[Frame Analysis]
    F --> H[Artifact Storage]
    F --> I[Demo Report]
```

The system is not intended to be a production streaming platform. It is a controlled environment for testing ideas related to streaming validation, display rendering, frame progression, synchronization, and post-run artifact review.

---

## Foundational Design Goals

| Goal                             | Description                                                                                                                      |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| Network-based validation         | The stream should be delivered over a network path, not only rendered from a local file.                                         |
| Repeatability                    | Test inputs should be deterministic enough to support reliable automation.                                                       |
| Browser-based display simulation | Virtual display clients should simulate multiple display endpoints without requiring physical displays.                          |
| Measurable synchronization       | Synchronization should be evaluated using frame markers and defined tolerances.                                                  |
| Artifact-first failures          | Test failures should produce useful artifacts such as screenshots, logs, decoded markers, stack traces, and offset measurements. |
| Report availability              | After a demo or test run, results should remain available through a stable local report path.                                    |
| Framework portability            | Java, Node/TypeScript, and Python implementations should be able to validate the same scenario.                                  |
| Protocol evolution               | HLS is used first, while WebRTC remains a planned extension.                                                                     |
| Clear scope boundaries           | The project should distinguish virtual display validation from physical display validation.                                      |

---

## What Components Exist?

The project is divided into infrastructure components, display components, and test automation components.

### Component Overview

| Component                  | Responsibility                                                                                                                |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| Synthetic Stream Generator | Creates a controlled test video stream with source label, timestamp, frame counter, test run ID, and machine-readable marker. |
| Media Server               | Receives the generated stream and exposes it to display clients over a streaming protocol.                                    |
| Display Clients            | Browser-based virtual displays that subscribe to the stream and render video.                                                 |
| Test Framework             | Orchestrates the test scenario, opens display clients, captures rendered output, and performs assertions.                     |
| Frame Analysis             | Extracts structured marker information from captured display output.                                                          |
| Artifact Collector         | Saves screenshots, frame snapshots, logs, offsets, summaries, reports, and stack traces.                                      |
| Demo Report Generator      | Creates a human-readable report for the latest run.                                                                           |
| Network Layer              | Provides a Docker-based simulated network environment.                                                                        |
| Configuration              | Defines source ID, display IDs, stream protocol, stream URL, tolerance, duration, and artifact path.                          |

---

## Component Responsibilities

### Synthetic Stream Generator

The synthetic stream generator creates the controlled video input used by the tests.

The initial implementation will use **FFmpeg**.

The generated video should include both machine-readable and human-readable information.

The machine-readable marker is the primary validation source. A QR code should encode structured metadata such as:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_001",
  "frame": 184,
  "timestamp": "2026-06-04T21:30:12.250Z"
}
```

The same information should also be shown as visible text below or near the marker:

```text
SOURCE: CAMERA_SYNC_TEST
TEST_RUN_ID: SYNC_RUN_001
FRAME: 000184
TIMESTAMP: 2026-06-04T21:30:12.250Z
```

The stream generator is responsible for:

* generating a repeatable synthetic video stream;
* embedding source identity;
* embedding test run identity;
* embedding frame progression information;
* embedding timestamp information;
* rendering a QR code or equivalent machine-readable marker;
* rendering human-readable text for debugging;
* publishing the stream to the media server.

---

## Marker Strategy for Synthetic Video Frames

The synthetic video stream should use a **machine-readable marker as the primary automation oracle** and human-readable text as supporting evidence.

The preferred frame design is:

```text
+--------------------------------------------------+
| QR CODE                                          |
| Payload:                                         |
| {                                                |
|   "source": "CAMERA_SYNC_TEST",                  |
|   "testRunId": "SYNC_RUN_001",                   |
|   "frame": 184,                                  |
|   "timestamp": "2026-06-04T21:30:12.250Z"        |
| }                                                |
|                                                  |
| SOURCE: CAMERA_SYNC_TEST                         |
| FRAME: 000184                                    |
| TEST RUN: SYNC_RUN_001                           |
| TIMESTAMP: 2026-06-04T21:30:12.250Z              |
+--------------------------------------------------+
```

The automation should rely primarily on the QR code or similar machine-readable marker because it is more reliable for automated frame validation than reading plain text through OCR.

Human-readable text should still be included because it makes screenshots and failure artifacts easier for developers and stakeholders to understand.

### Why Use a Machine-Readable Marker?

Plain frame numbers require OCR, which can be affected by:

* video compression;
* browser scaling;
* font rendering;
* anti-aliasing;
* screenshot quality;
* motion blur;
* resolution changes;
* contrast issues;
* timing differences between display clients.

A QR code or similar structured marker gives the test framework a more reliable way to extract the data needed for validation.

The marker should contain:

| Field       | Purpose                                                      |
| ----------- | ------------------------------------------------------------ |
| `source`    | Confirms the display is showing the expected stream          |
| `testRunId` | Prevents false positives from stale or previous test content |
| `frame`     | Allows frame progression and synchronization checks          |
| `timestamp` | Supports latency, staleness, and debugging analysis          |

### Marker Usage in Synchronization Testing

For synchronization validation, the test framework should decode the marker from each display and compare the frame numbers.

Example:

```text
DISPLAY_01 frame = 1050
DISPLAY_02 frame = 1049
DISPLAY_03 frame = 1051
```

The test calculates:

```text
max frame offset = max(frame) - min(frame)
max frame offset = 1051 - 1049 = 2 frames
```

If the configured tolerance is 3 frames, this sample passes.

```text
Expected tolerance: <= 3 frames
Actual offset: 2 frames
Result: PASS
```

### Marker Strategy Decision

The initial marker strategy is:

| Element                          | Role                                         |
| -------------------------------- | -------------------------------------------- |
| QR code                          | Primary automation oracle                    |
| Human-readable source label      | Manual debugging and artifact review         |
| Human-readable frame number      | Manual debugging and screenshot review       |
| Human-readable timestamp         | Manual debugging and latency investigation   |
| Background color or visual theme | Quick visual differentiation between sources |

### Future Marker Options

The first implementation should prioritize QR code payloads because they can encode the full test metadata directly.

Future options may include:

* ArUco markers for faster computer-vision detection;
* multiple markers per frame for stronger detection;
* fixed marker placement to simplify frame analysis;
* source-specific background colors;
* marker confidence scoring;
* fallback OCR only for debugging, not primary assertions.

---

### Media Server

The media server receives the video stream and exposes it to display clients.

The initial implementation will use **MediaMTX**.

The media server is responsible for:

* accepting the stream from FFmpeg;
* exposing the stream through HLS;
* allowing multiple display clients to subscribe to the same source;
* providing a networked stream path instead of local file playback.

Example HLS endpoint:

```text
http://localhost:8888/camera_sync_test/index.m3u8
```

---

### Display Clients

Display clients are browser-based virtual displays.

Each display client represents one screen or display endpoint.

Example display clients:

```text
DISPLAY_01
DISPLAY_02
DISPLAY_03
```

Each display client is responsible for:

* loading the configured HLS stream;
* rendering the video in a browser;
* exposing a stable visual surface for screenshot or frame capture;
* representing a network-connected display endpoint.

The first version may use a simple HTML page with `hls.js`.

Example conceptual URL:

```text
http://localhost:3000/display.html?displayId=DISPLAY_01&source=camera_sync_test
```

---

### Test Framework

The test framework controls the validation process.

The first implementation will use **Java**.

The test framework is responsible for:

* loading shared configuration;
* starting or verifying required services;
* opening multiple display clients;
* waiting for stream playback;
* capturing screenshots or rendered frames;
* validating source consistency;
* validating frame progression;
* calculating synchronization offset;
* saving artifacts;
* writing stack traces when failures occur;
* producing pass/fail results;
* making the latest demo report available after execution.

---

### Frame Analysis

Frame analysis determines what each display is showing.

The preferred approach is to decode a machine-readable marker from each captured display frame.

The first target implementation should use a QR code or similar marker that contains:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_001",
  "frame": 184,
  "timestamp": "2026-06-04T21:30:12.250Z"
}
```

The automation should use the decoded marker to validate:

* the display is showing the expected source;
* the frame belongs to the current test run;
* the frame number is progressing over time;
* the frame number is close enough to the other displays;
* the timestamp is useful for debugging stale-frame or latency issues.

Human-readable text may still be rendered in the video frame, but it should be treated as supporting evidence rather than the primary automation oracle.

Possible frame analysis approaches:

| Approach              | Usage                                                                  |
| --------------------- | ---------------------------------------------------------------------- |
| QR code decoding      | Preferred first implementation for structured metadata extraction      |
| ArUco marker decoding | Future option for robust computer-vision marker detection              |
| Visible text/OCR      | Useful for manual debugging, but not preferred for primary assertions  |
| Screenshot comparison | Useful for simple visual checks, but can be brittle                    |
| OpenCV frame analysis | Useful for advanced marker detection and future capture-card workflows |

The long-term goal is to make frame validation deterministic, structured, and repeatable across Java, Node/TypeScript, and Python implementations.

---

### Artifact Collector and Demo Report

The artifact collector stores test outputs in a persistent format.

The project should produce two levels of output:

1. **Raw run artifacts** for debugging and historical review.
2. **Latest demo report** for quick local review after execution.

Raw run artifacts should be stored by test run:

```text
artifacts/
  runs/
    SYNC_RUN_20260604_153012/
      summary.json
      offsets.csv
      failure-summary.md
      failure-stacktrace.txt
      screenshots/
        display_01_frame.jpg
        display_02_frame.jpg
        display_03_frame.jpg
      logs/
        test-runner.log
        ffmpeg.log
        mediamtx.log
      raw/
        marker-samples.jsonl
```

The latest report should be copied or generated into a stable location:

```text
artifacts/
  latest/
    index.html
    summary.json
    offsets.csv
    failure-summary.md
    failure-stacktrace.txt
    screenshots/
      display_01_frame.jpg
      display_02_frame.jpg
      display_03_frame.jpg
    logs/
      test-runner.log
      ffmpeg.log
      mediamtx.log
```

The `artifacts/latest/index.html` file should be the main report opened after a demo run.

It should show:

* test run ID;
* source ID;
* stream protocol;
* network mode;
* participating displays;
* pass/fail result;
* max frame offset;
* average frame offset;
* tolerance;
* frozen display status;
* screenshots from each display;
* links to raw artifact files;
* failure summary when applicable;
* failure stack trace when applicable.

The artifact collector is responsible for:

* creating a test-run-specific folder;
* creating or updating `artifacts/latest/`;
* saving screenshots or frame snapshots;
* saving decoded marker data;
* saving synchronization measurements;
* saving logs when available;
* saving a machine-readable `summary.json`;
* saving a human-readable `failure-summary.md`;
* saving a stack trace file when a test failure occurs;
* generating or updating a human-readable `index.html` report.

---

### Network Layer

The project uses Docker networking to simulate an internet-style streaming environment.

The goal is to avoid testing only local OS playback. Components should communicate over a network, even when everything runs on one development machine.

The network layer is responsible for:

* isolating project services on a shared Docker network;
* enabling service-to-service communication;
* supporting future network impairment tests;
* providing a repeatable local network topology.

Future network simulations may include:

* latency;
* jitter;
* packet loss;
* client disconnects;
* media server restart;
* display client restart.

---

## How Do Components Communicate?

The main communication path is:

```mermaid
sequenceDiagram
    participant S as FFmpeg Stream Generator
    participant M as MediaMTX Media Server
    participant D1 as Display Client 01
    participant D2 as Display Client 02
    participant D3 as Display Client 03
    participant T as Test Framework
    participant A as Artifacts

    S->>M: Publish synthetic video stream
    M->>D1: Serve HLS stream
    M->>D2: Serve HLS stream
    M->>D3: Serve HLS stream

    T->>D1: Open browser display client
    T->>D2: Open browser display client
    T->>D3: Open browser display client

    T->>D1: Capture rendered output
    T->>D2: Capture rendered output
    T->>D3: Capture rendered output

    T->>T: Decode markers and compare frame offsets
    T->>A: Save run artifacts
    T->>A: Update latest report
```

### Communication Summary

| From                  | To                 | Communication                                                  |
| --------------------- | ------------------ | -------------------------------------------------------------- |
| FFmpeg                | MediaMTX           | Publishes synthetic video stream                               |
| MediaMTX              | Display clients    | Serves HLS stream over HTTP                                    |
| Test framework        | Display clients    | Opens pages and captures rendered output                       |
| Test framework        | Artifacts folder   | Writes screenshots, JSON, CSV, logs, stack traces, and reports |
| Display clients       | MediaMTX           | Subscribe to the HLS stream                                    |
| Future test framework | Media server       | May query health/status endpoints if available                 |
| Future test framework | Network controller | May apply latency, packet loss, or disconnects                 |

---

## What Runs in Docker?

The project uses Docker to create a repeatable networked environment.

The expected Docker-managed components are:

| Component                | Runs in Docker? | Notes                                                              |
| ------------------------ | --------------: | ------------------------------------------------------------------ |
| MediaMTX                 |             Yes | Runs as the media server.                                          |
| Stream generator         |    Yes, planned | FFmpeg can run in a container or locally during early development. |
| Display client app       |    Yes, planned | Serves the browser-based display page.                             |
| Network simulation tools |    Yes, planned | Toxiproxy or similar tools may run in Docker later.                |
| Java test framework      |        Optional | Can run locally first, and later in Docker or CI.                  |
| Node test framework      |        Optional | Future implementation.                                             |
| Python test framework    |        Optional | Future implementation.                                             |

### Initial Docker Topology

```mermaid
flowchart TD
    subgraph Docker_Network[Docker Network: stream-sync-net]
        A[MediaMTX Container]
        B[Stream Generator Container]
        C[Display Client Web App Container]
    end

    B --> A
    A --> C
    D[Java Test Framework on Host] --> C
    D --> A
    D --> E[Host Artifacts Folder]
```

The first implementation can run the Java test framework on the host machine to simplify browser automation and local development.

Later, the test framework can also run inside Docker or through CI.

If the test framework runs inside Docker, the host `./artifacts` folder should be mounted into the test-runner container:

```yaml
volumes:
  - ./artifacts:/app/artifacts
```

This ensures artifacts remain available after containers stop.

---

## What Runs in the Test Framework?

The test framework should not own the entire streaming system. It should orchestrate and validate it.

The test framework is responsible for:

* reading test configuration;
* checking required services are available;
* starting helper processes if needed;
* opening display clients;
* waiting for playback readiness;
* capturing rendered output;
* decoding visible or machine-readable markers;
* calculating frame offset;
* applying pass/fail rules;
* writing run artifacts to `artifacts/runs/{testRunId}/`;
* writing the latest report to `artifacts/latest/`;
* preserving failure stack traces when tests fail.

The test framework should avoid:

* embedding large media-server configuration directly in test cases;
* hardcoding stream URLs in multiple places;
* mixing protocol logic with assertions;
* depending on manual observation;
* making pass/fail decisions through Gen AI;
* relying only on console output for demo results.

---

## Test-Aware Architecture

The architecture is designed to make test results inspectable after each demo run.

The test framework should not only return pass or fail. It should produce structured artifacts that explain what happened during the streaming scenario.

Each demo run should generate:

* a test execution result;
* screenshots or captured frames from each display;
* decoded marker data;
* frame offset measurements;
* logs from the test runner and streaming components;
* a machine-readable `summary.json`;
* a human-readable `index.html` report;
* a human-readable `failure-summary.md` when applicable;
* a `failure-stacktrace.txt` file when applicable.

The goal is that a reviewer can run the demo, open the generated report, and understand:

* which stream was expected;
* which protocol was used;
* which displays participated;
* whether each display rendered the expected stream;
* whether frames progressed;
* whether synchronization stayed within tolerance;
* why the test failed, if applicable;
* where to inspect raw artifacts.

---

## Artifact Storage and Report Availability

The project must persist test artifacts after execution.

Demo runs should not rely only on console output because console logs are not enough to explain streaming behavior after the run finishes.

Each test run should write artifacts to a persistent host-accessible directory.

Recommended local structure:

```text
artifacts/
  runs/
    {testRunId}/
      summary.json
      offsets.csv
      failure-summary.md
      failure-stacktrace.txt
      screenshots/
      logs/
      raw/
        marker-samples.jsonl

  latest/
    index.html
    summary.json
    offsets.csv
    failure-summary.md
    failure-stacktrace.txt
    screenshots/
    logs/
```

The `runs/{testRunId}` folder preserves historical results.

The `latest` folder provides a stable location for the most recent report.

The demo command should print the report location at the end of execution:

```text
Demo test completed.

Result:
  PASS

Report:
  artifacts/latest/index.html

Raw artifacts:
  artifacts/runs/{testRunId}/
```

For failures, the demo command should also print the failure reason and stack trace location:

```text
Demo test completed.

Result:
  FAIL

Reason:
  Synchronization offset exceeded tolerance.

Report:
  artifacts/latest/index.html

Stack trace:
  artifacts/latest/failure-stacktrace.txt

Raw artifacts:
  artifacts/runs/{testRunId}/
```

In GitHub Actions, the `artifacts/` folder should be uploaded using `actions/upload-artifact` with `if: always()` so results are available even when tests fail.

A later GitHub Pages workflow may publish `artifacts/latest/` as a static public report for portfolio/demo review.

---

## What Is Java Responsible For?

Java is the first implementation language for the test automation framework.

Java is responsible for the initial enterprise-style automation layer.

Java should handle:

* JUnit 5 test execution;
* shared configuration loading;
* orchestration of the test scenario;
* browser automation through Playwright for Java;
* screenshot or frame capture from display clients;
* basic marker extraction or integration with marker-decoding utilities;
* synchronization analysis;
* artifact writing;
* failure stack trace preservation;
* structured test reporting;
* latest demo report generation.

### Java Package Direction

A future Java implementation may use a structure similar to:

```text
test-frameworks/java/
  src/main/java/com/streamsync/
    config/
    display/
    artifacts/
    marker/
    media/
    network/
    stream/
    sync/
    report/
    util/

  src/test/java/com/streamsync/
    FrameworkSmokeTest.java
    MultiDisplaySyncTest.java
```

### Java Abstractions

| Class or Interface  | Responsibility                                                  |
| ------------------- | --------------------------------------------------------------- |
| `ConfigLoader`      | Loads shared YAML configuration.                                |
| `DisplayClient`     | Represents one virtual display endpoint.                        |
| `FrameCapture`      | Captures a screenshot or rendered frame.                        |
| `MarkerDecoder`     | Extracts source ID, test run ID, frame number, and timestamp.   |
| `SyncAnalyzer`      | Calculates frame offset and drift.                              |
| `ArtifactCollector` | Writes screenshots, JSON, CSV, logs, stack traces, and reports. |
| `ReportGenerator`   | Generates `artifacts/latest/index.html`.                        |
| `MediaServerClient` | Checks stream/media server availability.                        |
| `StreamGenerator`   | Starts or verifies FFmpeg stream generation.                    |
| `NetworkController` | Applies or removes network impairment in later phases.          |
| `ProtocolAdapter`   | Supports HLS first and WebRTC later.                            |

---

## Where Will Node/TypeScript Fit Later?

Node/TypeScript is planned as a future test framework implementation.

It is especially useful for browser-heavy automation because Playwright is very mature in the Node ecosystem.

Node/TypeScript may be used for:

* Playwright-first browser display testing;
* fast browser workflow experimentation;
* display-client interaction;
* screenshot capture;
* HLS playback validation;
* future WebRTC browser validation;
* comparison with the Java framework.

A future Node structure may look like:

```text
test-frameworks/node/
  package.json
  playwright.config.ts
  src/
    clients/
    capture/
    config/
    artifacts/
    marker/
    sync/
    report/
    utils/
  tests/
    multi-display-sync.spec.ts
```

Node should consume the same shared configuration and produce the same artifact format as Java.

---

## Where Will Python Fit Later?

Python is planned as a future test framework or analysis implementation.

It is especially useful for image processing, computer vision, and video-frame analysis.

Python may be used for:

* OpenCV-based frame analysis;
* QR or marker decoding;
* offset calculations;
* artifact post-processing;
* CSV/JSON reporting;
* future capture-card integration;
* frame-by-frame video analysis.

A future Python structure may look like:

```text
test-frameworks/python/
  pyproject.toml
  pytest.ini
  src/
    streamsync/
      clients/
      capture/
      config/
      artifacts/
      marker/
      sync/
      report/
      utils/
  tests/
    test_multi_display_sync.py
```

Python should consume the same shared configuration and produce the same artifact format as Java and Node/TypeScript.

---

## Why HLS First?

HLS is selected for the first implementation because it is practical for a browser-based proof of concept.

Reasons to start with HLS:

* It is browser-friendly when used with `hls.js`.
* It is easier to debug than WebRTC.
* It works well with HTTP-based local streaming.
* It fits naturally with MediaMTX.
* It reduces early project complexity.
* It allows the project to validate the core test strategy before adding real-time protocol complexity.

HLS is not the lowest-latency streaming option, but the first milestone is not to prove low-latency production streaming. The first milestone is to prove that automation can validate:

* stream availability;
* source consistency;
* frame progression;
* multi-display frame offset;
* artifact collection;
* report generation.

---

## Why WebRTC Later?

WebRTC is planned as a later extension because it is more realistic for lower-latency streaming, but it introduces additional complexity.

WebRTC may be added later to validate:

* lower-latency playback;
* real-time display synchronization;
* connection state handling;
* reconnect behavior;
* browser peer-connection behavior;
* differences between HLS and WebRTC synchronization behavior.

Reasons to defer WebRTC:

* signaling and session setup add complexity;
* browser behavior can be harder to debug;
* autoplay and connection-state issues may affect test stability;
* network negotiation introduces more failure modes;
* the project first needs a working baseline.

The planned protocol path is:

```mermaid
flowchart LR
    A[HLS Baseline] --> B[HLS Synchronization Validation]
    B --> C[Network Degradation Testing]
    C --> D[WebRTC Extension]
```

---

## What Is Inside the Project Scope?

The project scope includes:

* generating synthetic video streams;
* streaming video through a local media server;
* delivering video over a Docker network;
* playing HLS streams in browser-based display clients;
* validating multiple virtual displays;
* measuring source consistency;
* measuring frame progression;
* measuring frame offset across displays;
* detecting frozen displays;
* collecting persistent artifacts;
* generating a latest demo report;
* preserving stack traces for failed tests;
* creating Java-first automation;
* supporting future Node/TypeScript and Python implementations;
* preparing for future WebRTC support;
* preparing for future network degradation testing;
* preparing for GitHub Actions artifact upload.

---

## What Is Outside the Project Scope?

The project does not currently include:

* validating a physical video wall;
* validating HDMI output;
* validating capture-card input;
* validating GPU-specific rendering performance;
* validating commercial AV-over-IP hardware;
* validating a production-grade streaming platform;
* validating actual public internet behavior across regions;
* building a full media server;
* building a full display-management product;
* using Gen AI for pass/fail decisions;
* measuring Java code coverage.

These may be considered future extensions, but they are not part of the initial scope.

---

## Scope Boundary: Virtual Display vs Physical Display

The initial architecture validates browser-based virtual displays.

This means the project can prove:

* the browser client loaded the stream;
* the expected source was rendered;
* frames progressed;
* frame offsets could be measured;
* display clients behaved consistently under the test conditions.

It does not prove:

* the content appeared on a physical monitor;
* HDMI output was correct;
* a hardware video wall was synchronized;
* external display devices behaved correctly.

Physical output validation would require a different architecture, likely involving:

* capture cards;
* real display clients;
* HDMI outputs;
* external cameras;
* lab hardware;
* capture-agent services.

---

## Scope Boundary: Deterministic Automation vs Gen AI

The core validation should remain deterministic.

The test framework should calculate pass/fail results using structured data:

* expected source;
* decoded source;
* frame number;
* frame progression;
* frame offset;
* tolerance;
* timeout;
* network mode;
* protocol.

Gen AI may be added later as an optional post-test analysis layer.

Gen AI can help with:

* failure summarization;
* likely failure classification;
* stakeholder-friendly reports;
* suggested debugging steps.

Gen AI should not decide whether the test passed or failed.

---

## Shared Configuration Model

All test framework implementations should use the same configuration model.

Example:

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
  futureProtocols:
    - WebRTC
  fps: 30

network:
  mode: docker
  dockerNetwork: stream-sync-net
  impairmentEnabled: false

artifacts:
  outputDir: artifacts
  latestDir: artifacts/latest
```

This keeps Java, Node/TypeScript, and Python aligned.

---

## Expected Repository Layout

```text
multi-display-stream-sync/
  README.md
  docker-compose.yml
  .gitignore

  docs/
    architecture.md
    streaming-plan.md
    test-strategy.md
    evidence-format.md

  config/
    test-config.yaml
    mediamtx.yml

  stream-generator/
    Dockerfile
    scripts/
      generate-stream.ps1
      generate-stream.sh
      publish-hls.ps1
      publish-hls.sh

  media-server/
    mediamtx.yml

  display-client/
    Dockerfile
    package.json
    src/
      index.html
      app.js
      styles.css

  artifacts/
    .gitkeep

  test-frameworks/
    java/
      pom.xml
      src/
        main/java/
        test/java/

    node/
      package.json
      playwright.config.ts
      src/
      tests/

    python/
      pyproject.toml
      pytest.ini
      src/
      tests/
```

---

## Architecture Principles

### 1. Keep streaming infrastructure separate from test logic

The media server, stream generator, and display clients should be configurable infrastructure, not hidden inside test methods.

### 2. Keep tests readable

A test should describe the scenario clearly:

```text
Given the stream is available
When three display clients render it
Then the frame offset should remain within tolerance
```

### 3. Keep artifacts consistent

All frameworks should write comparable artifacts.

### 4. Keep protocols replaceable

HLS should be the first protocol, but the architecture should allow WebRTC later.

### 5. Keep pass/fail deterministic

The automation should use measurable data for assertions.

### 6. Keep scope honest

The initial project validates virtual display clients, not physical display walls.

### 7. Keep the marker machine-readable

The QR code or equivalent marker should be the primary source of truth for automated validation. Human-readable text should support debugging and artifact review.

### 8. Keep reports available after execution

A demo run should leave behind persistent artifacts and a stable latest report. The result should not disappear when containers stop or terminal output is cleared.

---

## Summary

This architecture creates a controlled, network-based lab for validating multi-display video stream synchronization.

The first version will use:

* FFmpeg for synthetic stream generation;
* QR-code-based frame markers as the primary validation oracle;
* MediaMTX as the media server;
* HLS as the first streaming protocol;
* browser-based virtual display clients;
* Docker networking for local network simulation;
* Java as the first test automation framework;
* persistent artifact storage under `artifacts/`;
* a stable latest demo report under `artifacts/latest/`;
* shared artifact and configuration formats.

Future versions may add:

* Node/TypeScript test automation;
* Python frame analysis;
* WebRTC streaming;
* network impairment;
* capture-card validation;
* GitHub Actions artifact publishing;
* GitHub Pages report publishing;
* Gen AI-assisted failure analysis.

The architecture is intentionally scoped to demonstrate practical, measurable, and evidence-based validation of streaming video behavior across multiple display clients.
