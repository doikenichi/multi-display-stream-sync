# Video Streaming Test Strategy

## Purpose

This document defines the **video streaming test strategy** for the Multi-Display Stream Synchronization Test Lab.

The purpose of this document is to explain **what video-streaming behavior will be tested, how it will be validated, what evidence will be collected, and how pass/fail decisions will be made**.

This document focuses on the behavior of the streaming system:

```text
FFmpeg → MediaMTX → HLS → Browser Display Clients
```

It does not describe how the test automation framework will be engineered, unit tested, or measured for code coverage. Those topics are covered separately in `test-framework-strategy.md`.

---

## Audience

This document is intended for:

* QA engineers;
* automation engineers;
* senior software engineers;
* engineering leaders;
* stakeholders reviewing streaming quality, synchronization, latency, and test evidence.

---

## Scope

This strategy covers validation of browser-based virtual display clients receiving and rendering a controlled video stream.

The test strategy validates whether multiple browser display clients:

* load the expected stream;
* render the expected video source;
* show frames that continue progressing;
* remain synchronized within a configured frame tolerance;
* detect frozen or stale display behavior;
* expose measurable latency evidence when latency mode is enabled;
* generate network traffic evidence for post-run analysis;
* produce useful artifacts and reports.

The initial streaming path is:

```text
FFmpeg → MediaMTX → HLS → Browser Display Clients
```

---

## Out of Scope

This document does not cover:

* Java framework package structure;
* unit testing the test framework;
* JaCoCo coverage;
* EvoSuite usage;
* framework development milestones;
* internal framework class design;
* test framework CI/CD implementation details.

This document also does not validate:

* physical display panels;
* HDMI output;
* capture cards;
* GPU-specific rendering behavior;
* commercial AV-over-IP hardware;
* production-scale public internet streaming;
* production-grade WebRTC behavior.

Physical display validation, capture-card validation, and WebRTC validation may be added later as separate extensions.

---

## System Under Test

The video streaming system under test contains the following components.

| Component                         | Responsibility                                                                     |
| --------------------------------- | ---------------------------------------------------------------------------------- |
| FFmpeg Synthetic Stream Generator | Generates and publishes a controlled video stream.                                 |
| MediaMTX Media Server             | Receives the stream and exposes it through HLS.                                    |
| HLS Endpoint                      | Serves the stream playlist and media segments over HTTP.                           |
| Browser Display Clients           | Render the stream as virtual displays.                                             |
| Docker Network                    | Provides a repeatable network-based environment.                                   |
| Network Capture Agent             | Captures network traffic for post-run analysis when enabled.                       |
| Test Control API                  | Controls stream lifecycle and prepares the test run.                               |
| Test Framework                    | Observes display output, decodes markers, validates behavior, and stores evidence. |

The test framework is a supporting validation tool, but the subject of this document is the **video streaming behavior**, not the framework’s internal engineering strategy.

---

## Streaming Quality Goals

The streaming system should support the following quality goals.

| Goal                          | Description                                                                   |
| ----------------------------- | ----------------------------------------------------------------------------- |
| Source correctness            | Every display should render the expected stream source.                       |
| Test run consistency          | Displays should show content from the active test run, not stale content.     |
| Frame progression             | Each display should continue advancing frames over time.                      |
| Multi-display synchronization | Displays should remain within a configured frame offset tolerance.            |
| Frozen display detection      | A display that stops updating should be identified.                           |
| Latency observability         | The test should measure observed render latency when latency mode is enabled. |
| First-frame evidence          | Latency mode should attempt to capture frame 0 or frame 1 when required.      |
| Network observability         | Packet captures should support post-run traffic analysis.                     |
| Artifact-first diagnosis      | Streaming failures should leave evidence for review.                          |
| Repeatability                 | The same scenario should be reproducible locally and in future CI runs.       |

---

## Test Modes

The streaming test strategy defines three major test modes.

### 1. Synchronization Mode

Synchronization mode validates whether multiple display clients are rendering the same stream and remain close enough to each other.

This mode validates:

* stream availability;
* source consistency;
* test run ID consistency;
* frame progression;
* frozen display detection;
* multi-display frame offset;
* artifact generation;
* latest report generation.

Synchronization mode does not require frame 0 or frame 1. It may begin validation from the first stable decoded marker after playback readiness.

---

### 2. Latency Mode

Latency mode validates how long it takes for generated stream content to become visible in browser display clients.

This mode validates:

* controlled stream start;
* pre-armed display clients;
* first visible decoded frame;
* missed initial frames;
* observed render latency;
* packet-capture availability;
* correlation between frame marker data, browser capture timing, and network traffic.

Latency mode may require frame 0 or frame 1 depending on configuration.

If frame 0 or frame 1 is required and not observed, the latency test should fail.

---

### 3. Network Analysis Mode

Network analysis mode captures traffic for later inspection.

This mode may be used together with synchronization mode or latency mode.

Network analysis mode validates that traffic evidence is collected for:

* HLS playlist requests;
* HLS segment requests;
* segment download timing;
* TCP retransmissions;
* connection resets;
* display-client traffic differences;
* future impairment scenarios such as latency, jitter, packet loss, or disconnects.

Network analysis does not replace deterministic test assertions. It provides supporting evidence for debugging and post-run investigation.

---

## Testable Streaming Behaviors

## 1. Stream Availability

Stream availability verifies that the HLS stream is reachable after FFmpeg publishes to MediaMTX.

The test should verify:

* MediaMTX is running;
* FFmpeg has started publishing the stream;
* the HLS endpoint is reachable;
* the display client can load the stream URL.

Example HLS endpoint:

```text
http://localhost:8888/camera_sync_test/index.m3u8
```

Failure examples:

| Failure                           | Meaning                                                       |
| --------------------------------- | ------------------------------------------------------------- |
| HLS playlist unavailable          | MediaMTX may not be serving the stream.                       |
| Stream generator not publishing   | FFmpeg may not have started or may have failed.               |
| Display client cannot load stream | Browser client, stream URL, or network path may be incorrect. |
| Stream starts but stops early     | Stream generator or media server may be unstable.             |

---

## 2. Source Consistency

Source consistency verifies that every display is showing the expected source.

The generated video should include a marker containing structured metadata.

Example marker payload:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_001",
  "frame": 184,
  "timestamp": "2026-06-04T21:30:12.250Z"
}
```

A display passes source consistency when:

* a marker is detected;
* the decoded `source` matches the configured source;
* the decoded `testRunId` matches the active test run;
* the content is not stale from a previous run.

Failure examples:

| Failure             | Meaning                                    |
| ------------------- | ------------------------------------------ |
| Source mismatch     | The display is rendering the wrong stream. |
| Missing marker      | The display output cannot be validated.    |
| Wrong test run ID   | The display may be showing stale content.  |
| Blank or error page | The stream did not render correctly.       |

Source consistency is required before synchronization or latency results can be trusted.

---

## 3. Frame Progression

Frame progression verifies that each display continues rendering new frames over time.

The test should collect multiple marker samples from each display.

Example:

```text
DISPLAY_01 samples:
  t=0s   frame=120
  t=1s   frame=150
  t=2s   frame=180
```

A display passes frame progression when:

* decoded frame numbers increase over time;
* repeated samples show forward movement;
* the display does not stay on the same frame beyond the configured timeout;
* progression is reasonable for the configured FPS and sampling interval.

The test does not need to observe every generated frame. HLS buffering, browser rendering, and screenshot timing may cause some frames to be skipped.

The important question is whether the display is actively progressing.

---

## 4. Multi-Display Synchronization

Multi-display synchronization verifies that all participating displays are rendering frames close enough to each other.

Example sample:

```text
DISPLAY_01 frame = 1050
DISPLAY_02 frame = 1049
DISPLAY_03 frame = 1051
```

Frame offset is calculated as:

```text
max frame offset = max(frame) - min(frame)
max frame offset = 1051 - 1049 = 2 frames
```

If the configured tolerance is 3 frames:

```text
Expected tolerance: <= 3 frames
Actual offset: 2 frames
Result: PASS
```

A synchronization sample passes when:

```text
maxFrameOffset <= toleranceFrames
```

Recommended synchronization rule:

```text
Ignore startup warm-up period.
After warm-up, all displays must:
  1. show the expected source;
  2. show the current test run ID;
  3. continue frame progression;
  4. remain within frame offset tolerance.
```

---

## 5. Frozen Display Detection

Frozen display detection verifies that a display has not stopped updating.

A display should be considered frozen or stale when the decoded frame number does not advance across a configured time window.

Example configuration:

```yaml
sync:
  frozenDisplayTimeoutSeconds: 5
  minimumFrameAdvance: 1
```

Example failure:

```text
DISPLAY_02:
  t=10s frame=450
  t=11s frame=450
  t=12s frame=450
  t=13s frame=450
  t=14s frame=450
  t=15s frame=450

Result:
  FAIL - DISPLAY_02 frozen for 5 seconds
```

A frozen display should fail the test even if its last observed frame happens to be within synchronization tolerance.

Frozen display artifacts should include:

* display ID;
* last progressing frame;
* repeated stale frame;
* stale duration;
* screenshot or captured frame;
* marker samples;
* failure summary.

---

## 6. Frame Offset Tolerance

Frame offset tolerance defines how far apart displays are allowed to be.

Example configuration:

```yaml
stream:
  fps: 30

testRun:
  toleranceFrames: 3
```

At 30 FPS:

```text
3 frames ≈ 100 milliseconds
```

The report should show both values where possible:

```text
Tolerance:
  3 frames
  approximately 100 ms at 30 FPS
```

Tolerance should be configurable because acceptable synchronization depends on:

* protocol;
* browser behavior;
* network mode;
* HLS buffering;
* future impairment profiles;
* stakeholder expectations.

For HLS, a larger practical tolerance may be needed during early implementation because HLS is segment-based and may introduce client buffering differences.

---

## 7. Latency and First-Frame Capture

Latency testing requires a stricter lifecycle than synchronization testing.

For general synchronization, the test may start from the first stable decoded marker. For latency testing, the system should capture the earliest visible frame possible, ideally frame 0 or frame 1.

Latency mode should use this lifecycle:

1. create a test run ID;
2. prepare a deterministic frame sequence starting at frame 0;
3. start MediaMTX;
4. open browser display clients and place them in an armed/waiting state;
5. start packet capture;
6. start the stream through the Test Control API;
7. record the stream start timestamp;
8. capture rendered output as soon as playback begins;
9. decode the first visible marker from each display;
10. calculate observed render latency;
11. save screenshots, decoded marker data, timing data, packet captures, and reports.

Recommended marker payload for latency mode:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_001",
  "frame": 0,
  "generatedAt": "2026-06-05T14:00:00.000Z",
  "streamStartEpochMs": 1780689600000,
  "fps": 30,
  "sequenceId": "SEQ_001"
}
```

Observed render latency:

```text
observedRenderLatencyMs = browserCaptureTimeMs - frameGeneratedTimeMs
```

For pre-generated frame sequences:

```text
frameGeneratedTimeMs = streamStartEpochMs + (frameNumber * frameDurationMs)
```

For HLS, this metric should be called:

```text
observed render latency
```

or:

```text
glass-to-browser latency
```

It should not be described as pure network latency.

For HLS, observed render latency includes:

* frame generation;
* encoding;
* media-server ingest;
* HLS segment creation;
* playlist request;
* media segment request;
* browser buffering;
* browser decoding;
* browser rendering;
* test capture timing.

If frame 0 or frame 1 is required but not observed, the latency test should fail.

Example:

```text
Expected first visible frame:
  frame <= 1

Observed first visible frame:
  frame = 42

Result:
  FAIL - required first-frame capture was not achieved
```

---

## 8. Network Behavior

The streaming system should be validated over a network path.

The initial environment uses Docker networking to avoid testing only local file playback.

Initial network behavior tests should verify:

* MediaMTX is reachable;
* the HLS endpoint is reachable;
* browser display clients can load the stream;
* multiple clients can subscribe to the same source;
* packet capture can be enabled when required.

Future network degradation tests may include:

| Scenario                 | Purpose                                             |
| ------------------------ | --------------------------------------------------- |
| Added latency            | Measure synchronization and latency impact.         |
| Jitter                   | Validate behavior when delay varies over time.      |
| Packet loss              | Validate playback stability under lossy conditions. |
| Bandwidth limitation     | Validate behavior under constrained throughput.     |
| Display disconnect       | Validate frozen display detection and recovery.     |
| Media server restart     | Validate stream interruption and recovery behavior. |
| Stream generator restart | Validate stale content and test run ID behavior.    |

Network degradation should be implemented as controlled test modes, not manual steps.

Example future configuration:

```yaml
network:
  mode: docker
  dockerNetwork: stream-sync-net
  impairmentEnabled: true
  impairmentProfile: latency_200ms_jitter_50ms
```

---

## Network Traffic Capture

The test strategy should include network traffic capture for post-run analysis.

Automated tests should use tools such as:

| Tool      | Use                                                         |
| --------- | ----------------------------------------------------------- |
| `tcpdump` | Capture raw packets into `.pcap` files.                     |
| `tshark`  | Generate text, JSON, or CSV summaries from packet captures. |
| Wireshark | Manually inspect saved `.pcap` files after the run.         |

Wireshark should be treated as a manual viewer, not the automated capture mechanism.

Packet capture should help analyze:

* HLS playlist request timing;
* HLS media segment request timing;
* segment download duration;
* TCP retransmissions;
* connection resets;
* differences between display clients;
* effects of future network impairment;
* relationship between network traffic and observed render latency.

Recommended artifact structure:

```text
artifacts/
  runs/
    {testRunId}/
      network/
        full-run.pcap
        mediamtx-interface.pcap
        display-client-traffic.pcap
        tshark-summary.txt
        tshark-http-requests.json
        capture-metadata.json
```

Example capture metadata:

```json
{
  "testRunId": "SYNC_RUN_001",
  "captureEnabled": true,
  "captureTool": "tcpdump",
  "captureStartedAt": "2026-06-05T14:00:00.000Z",
  "captureStoppedAt": "2026-06-05T14:01:00.000Z",
  "interfaces": ["eth0"],
  "filters": [
    "port 8888",
    "port 8554"
  ],
  "files": [
    "network/full-run.pcap",
    "network/tshark-summary.txt"
  ]
}
```

Packet capture is supporting evidence. It should not replace deterministic assertions based on decoded markers, frame progression, frame offset, latency calculation, and artifact validation.

---

## Deterministic Validation

Pass/fail decisions must be deterministic.

The streaming validation should use structured evidence such as:

* expected source ID;
* decoded source ID;
* expected test run ID;
* decoded test run ID;
* decoded frame number;
* generated frame timestamp;
* browser capture timestamp;
* display ID;
* stream protocol;
* FPS;
* tolerance;
* first-frame requirement;
* network mode;
* packet-capture status.

Deterministic assertions include:

| Assertion                | Pass/Fail Basis                                                 |
| ------------------------ | --------------------------------------------------------------- |
| Source consistency       | Decoded source equals expected source.                          |
| Test run consistency     | Decoded test run ID equals active test run.                     |
| Frame progression        | Frame numbers increase over time.                               |
| Frozen display detection | Frame does not remain unchanged beyond timeout.                 |
| Synchronization          | Max frame offset is within configured tolerance.                |
| First-frame requirement  | First observed frame satisfies latency-mode policy.             |
| Observed render latency  | Latency can be calculated and optionally compared to threshold. |
| Network capture          | Required packet-capture files exist when enabled.               |
| Artifact generation      | Required evidence files exist after execution.                  |

Gen AI must not decide whether a streaming test passed or failed.

---

## Streaming Test Scenarios

## Scenario 1: HLS Stream Availability

Purpose:

Verify that the HLS stream endpoint is available after FFmpeg publishes to MediaMTX.

Expected result:

* MediaMTX is reachable;
* HLS playlist is reachable;
* stream can be loaded by a browser display client.

---

## Scenario 2: Single Display Source Validation

Purpose:

Verify that one display renders the expected source.

Expected result:

* display loads successfully;
* marker is detected;
* decoded source matches expected source;
* decoded test run ID matches active test run.

---

## Scenario 3: Single Display Frame Progression

Purpose:

Verify that one display continues rendering frames.

Expected result:

* multiple samples are collected;
* decoded frame number increases over time;
* display is not frozen.

---

## Scenario 4: Multi-Display Source Consistency

Purpose:

Verify that all displays render the same expected stream.

Expected result:

* all displays decode the expected source;
* all displays decode the active test run ID;
* no display shows stale or wrong content.

---

## Scenario 5: Multi-Display Synchronization

Purpose:

Verify that multiple displays remain within configured frame offset tolerance.

Expected result:

* all displays progress frames;
* max frame offset remains within tolerance after warm-up;
* offset samples are written to `offsets.csv`;
* report includes max and average offset.

---

## Scenario 6: Frozen Display Detection

Purpose:

Verify that a frozen or stale display is detected.

Expected result:

* repeated frame values beyond timeout are detected;
* affected display is identified;
* test fails with a clear reason;
* screenshots and marker samples are saved.

---

## Scenario 7: Latency First-Frame Capture

Purpose:

Verify that latency mode can capture the earliest visible rendered frame and calculate observed render latency.

Expected result:

* display clients are armed before stream start;
* stream starts through the Test Control API;
* first visible marker is captured;
* first observed frame satisfies configured first-frame requirement;
* observed render latency is calculated;
* latency results are written to `latency.csv`;
* latest report includes latency summary.

---

## Scenario 8: Network Traffic Capture

Purpose:

Verify that packet-capture artifacts are generated for post-run analysis.

Expected result:

* network capture starts before stream start;
* capture stops after the test run;
* `.pcap` file is saved;
* capture metadata is saved;
* optional `tshark` summary is saved;
* latest report links to network artifacts.

---

## Scenario 9: Future Network Degradation

Purpose:

Validate stream behavior under controlled network impairment.

Status:

Future.

Expected result:

* selected impairment profile is applied;
* active profile is recorded in artifacts;
* source consistency and progression are evaluated;
* synchronization impact is measured;
* latency impact is measured when latency mode is enabled;
* recovery behavior is reported.

---

## Streaming Artifacts

Each streaming test run should generate persistent artifacts.

Recommended structure:

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
        display_01_frame.jpg
        display_02_frame.jpg
        display_03_frame.jpg
      logs/
        ffmpeg.log
        mediamtx.log
        control-api.log
        test-runner.log
      raw/
        marker-samples.jsonl
      network/
        full-run.pcap
        mediamtx-interface.pcap
        display-client-traffic.pcap
        tshark-summary.txt
        tshark-http-requests.json
        capture-metadata.json

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

The latest report should be available at:

```text
artifacts/latest/index.html
```

The report should include:

* test run ID;
* source ID;
* protocol;
* stream URL or source name;
* network mode;
* impairment profile when applicable;
* display IDs;
* pass/fail result;
* source consistency result;
* frame progression result;
* frozen display status;
* max frame offset;
* average frame offset;
* configured tolerance;
* first observed frame per display;
* observed render latency per display;
* missed initial frame count;
* packet-capture file links;
* screenshots;
* decoded marker samples;
* logs;
* failure reason;
* stack trace when applicable.

---

## Streaming Pass/Fail Criteria

Synchronization mode should pass only when:

| Check                             | Required Result |
| --------------------------------- | --------------- |
| Stream endpoint reachable         | Pass            |
| Display clients loaded            | Pass            |
| Marker detected on each display   | Pass            |
| Source matches expected source    | Pass            |
| Test run ID matches active run    | Pass            |
| Frames progress on each display   | Pass            |
| No display frozen beyond timeout  | Pass            |
| Max frame offset within tolerance | Pass            |
| Required artifacts generated      | Pass            |
| Latest report generated           | Pass            |

Latency mode should pass only when:

| Check                                                  | Required Result |
| ------------------------------------------------------ | --------------- |
| Test run created                                       | Pass            |
| Display clients armed before stream start              | Pass            |
| Network capture starts when enabled                    | Pass            |
| Stream starts through controlled endpoint              | Pass            |
| First visible marker captured                          | Pass            |
| First observed frame satisfies first-frame requirement | Pass            |
| Observed render latency calculated                     | Pass            |
| Latency artifacts generated                            | Pass            |
| Packet-capture artifacts generated when enabled        | Pass            |

Failure conditions include:

| Failure                                            | Result |
| -------------------------------------------------- | ------ |
| Wrong source                                       | Fail   |
| Wrong test run ID                                  | Fail   |
| Missing marker after timeout                       | Fail   |
| Frozen display beyond threshold                    | Fail   |
| Frame offset exceeds tolerance                     | Fail   |
| Latency mode misses required frame 0 or frame 1    | Fail   |
| Observed render latency cannot be calculated       | Fail   |
| Required packet-capture files missing when enabled | Fail   |
| Required artifacts missing                         | Fail   |

---

## Risks and Mitigations

| Risk                             | Impact                                                                      | Mitigation                                                                         |
| -------------------------------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| HLS buffering causes offset      | Displays may appear less synchronized.                                      | Use warm-up period, configurable tolerance, and clear reporting.                   |
| HLS may not render frame 0       | Latency test may fail first-frame requirement.                              | Use armed displays, deterministic frame sequence, and explicit first-frame policy. |
| Latency is misunderstood         | Stakeholders may interpret observed render latency as pure network latency. | Label metric clearly as observed render latency or glass-to-browser latency.       |
| OCR is unreliable                | Frame validation may be inconsistent.                                       | Use QR or machine-readable marker as primary oracle.                               |
| Browser timing varies            | Early samples may be misleading.                                            | Use controlled lifecycle and warm-up rules where appropriate.                      |
| Packet captures become large     | Artifacts may be difficult to manage.                                       | Use filters, short test durations, and summaries.                                  |
| Packet capture needs permissions | Local setup may be harder.                                                  | Run capture in a dedicated container with required capabilities.                   |
| Network degradation is flaky     | Results may vary.                                                           | Add degradation only after baseline tests are stable.                              |

---

## Summary

This video streaming test strategy validates the observable behavior of the streaming pipeline:

```text
FFmpeg → MediaMTX → HLS → Browser Display Clients
```

The strategy focuses on source consistency, frame progression, multi-display synchronization, frozen display detection, latency measurement, first-frame capture, network traffic capture, and evidence generation.

Pass/fail decisions must remain deterministic and based on decoded marker data, frame numbers, timestamps, tolerances, first-frame policies, and required artifacts.

Gen AI may later help explain failures, but it must not decide whether the video streaming test passed or failed.
