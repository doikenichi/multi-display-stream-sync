# Evidence Format

## Purpose

This document defines the output evidence artifacts for the **Multi-Display Stream Synchronization Test Lab** before implementation begins.

The goal is to make every test run explainable, reproducible, and reviewable by QA engineers, automation engineers, developers, and senior stakeholders.

The evidence format must support:

* deterministic pass/fail decisions;
* quick stakeholder review after a demo run;
* detailed engineering investigation after failures;
* comparable output across Java, Node/TypeScript, and Python implementations;
* future CI/CD artifact upload and report publishing.

This document defines the expected folder structure and the required format for key artifacts such as:

* `summary.json`;
* `offsets.csv`;
* screenshots;
* logs;
* `failure-summary.md`;
* supporting raw evidence.

---

## Audience

This document is intended for:

* QA engineers validating streaming behavior;
* automation engineers implementing the framework;
* senior engineers reviewing architecture and failure evidence;
* engineering managers and senior leaders reviewing demo results;
* future contributors implementing Node/TypeScript or Python versions of the same validation logic.

---

## Evidence Design Principles

| Principle | Meaning |
|---|---|
| Deterministic | Pass/fail must be based on structured evidence, not manual interpretation. |
| Artifact-first | Every run should leave enough evidence to understand what happened after execution. |
| Stakeholder-readable | A senior leader should be able to open the latest report and understand the result. |
| Engineer-debuggable | QA and engineering teams should be able to inspect screenshots, logs, marker samples, offsets, and stack traces. |
| Comparable across languages | Java, Node/TypeScript, and Python should produce the same artifact structure and schemas. |
| Failure-safe | Artifacts should be written even when the test fails or aborts. |
| CI-friendly | The entire `artifacts/` directory should be uploadable by CI, including failed runs. |

---

## Evidence Ownership

Evidence is split between the Streaming Lab Orchestration API and the test framework.

| Artifact Area | Primary Owner | Purpose |
|---|---|---|
| Generated frames | Streaming Lab Orchestration API | Source evidence for the synthetic stream. |
| Source sequence manifest | Streaming Lab Orchestration API | Defines expected frame sequence, FPS, media path, and stream URL. |
| FFmpeg logs | Streaming Lab Orchestration API | Shows stream publishing behavior. |
| MediaMTX logs | Streaming Lab Orchestration API / infrastructure | Shows media server behavior. |
| Packet captures | Streaming Lab Orchestration API / network capture agent | Supports network-level investigation. |
| Screenshots / captured frames | Test framework | Shows what each browser display rendered. |
| Decoded marker samples | Test framework | Primary structured evidence for validation. |
| Offset calculations | Test framework | Synchronization evidence. |
| Latency calculations | Test framework | Observed render latency evidence when latency mode is enabled. |
| Summary result | Test framework | Machine-readable execution result. |
| Failure summary | Test framework | Human-readable explanation of failure. |
| Stack trace | Test framework | Engineering debug evidence. |
| Latest report | Test framework or report module | Main review artifact for demo and stakeholder use. |

The deterministic test result must come from the test framework.

The orchestration API provides environment and source evidence, but it should not decide whether the validation passed or failed.

---

## Required Evidence Folder Structure

Every run must write artifacts under a unique run folder.

```text
artifacts/
  runs/
    {testRunId}/
      summary.json
      offsets.csv
      latency.csv
      failure-summary.md
      failure-stacktrace.txt

      generated-frames/
        frame_000000.png
        frame_000001.png
        frame_000002.png

      source-sequence/
        sequence-manifest.json

      screenshots/
        display_01/
          sample_000001.jpg
          sample_000002.jpg
          final.jpg
        display_02/
          sample_000001.jpg
          sample_000002.jpg
          final.jpg
        display_03/
          sample_000001.jpg
          sample_000002.jpg
          final.jpg

      logs/
        test-runner.log
        ffmpeg.log
        mediamtx.log
        control-api.log
        browser-console-display_01.log
        browser-console-display_02.log
        browser-console-display_03.log

      raw/
        marker-samples.jsonl
        display-events.jsonl
        assertion-events.jsonl

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
    failure-summary.md
    failure-stacktrace.txt
    screenshots/
    logs/
    raw/
    network/
```

### Run Folder

The run folder preserves historical evidence.

```text
artifacts/runs/{testRunId}/
```

Example:

```text
artifacts/runs/SYNC_RUN_20260605_001/
```

This folder should never be overwritten by another run.

### Latest Folder

The latest folder is a stable path for the most recent result.

```text
artifacts/latest/
```

The latest folder may be recreated for each run by copying or generating the current run's review artifacts.

The main human-readable entry point is:

```text
artifacts/latest/index.html
```

This is the report a reviewer should open after running the demo.

---

## Required Artifact Summary

| Artifact | Required | Owner | Description |
|---|---:|---|---|
| `summary.json` | Yes | Test framework | Machine-readable execution result. |
| `offsets.csv` | Yes for sync tests | Test framework | Frame offset measurements across displays. |
| `latency.csv` | Yes for latency mode | Test framework | First-frame and observed render latency measurements. |
| `failure-summary.md` | Required on failure; optional on pass | Test framework | Human-readable failure explanation. |
| `failure-stacktrace.txt` | Required on framework/test exception | Test framework | Stack trace for debugging. |
| `generated-frames/` | Yes for deterministic frame mode | Orchestration API | Source PNG frames used by FFmpeg. |
| `source-sequence/sequence-manifest.json` | Yes | Orchestration API | Source sequence reference metadata. |
| `screenshots/` | Yes | Test framework | Captured browser-rendered evidence. |
| `logs/test-runner.log` | Yes | Test framework | Framework execution log. |
| `logs/ffmpeg.log` | Expected when FFmpeg is used | Orchestration API | FFmpeg publishing log. |
| `logs/mediamtx.log` | Expected when available | Infrastructure | Media server log. |
| `logs/control-api.log` | Expected | Orchestration API | API lifecycle log. |
| `raw/marker-samples.jsonl` | Yes | Test framework | Decoded marker samples from browser output. |
| `network/*.pcap` | Required when capture enabled | Network capture agent | Packet capture files. |
| `network/capture-metadata.json` | Required when capture enabled | Network capture agent | Capture configuration and file references. |
| `latest/index.html` | Yes | Report module / test framework | Main demo report. |

---

## `summary.json` Format

`summary.json` is the primary machine-readable result for the run.

It should be concise enough for CI and dashboards, but detailed enough to explain the result without opening every raw file.

### Location

```text
artifacts/runs/{testRunId}/summary.json
artifacts/latest/summary.json
```

### Required Fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `schemaVersion` | string | Yes | Evidence schema version. |
| `testRunId` | string | Yes | Unique run identifier. |
| `sequenceId` | string | Yes | Generated frame sequence ID. |
| `sourceId` | string | Yes | Expected stream source ID. |
| `protocol` | string | Yes | Streaming protocol, initially `HLS`. |
| `mediaServer` | string | Yes | Media server, initially `MediaMTX`. |
| `hlsUrl` | string | Yes for HLS | Run-specific HLS URL. |
| `startedAt` | string | Yes | Run start timestamp in ISO-8601 UTC. |
| `completedAt` | string | Yes when completed | Run completion timestamp in ISO-8601 UTC. |
| `durationMs` | number | Yes when completed | Total execution duration. |
| `status` | string | Yes | `PASS`, `FAIL`, `ERROR`, or `ABORTED`. |
| `mode` | string | Yes | `SYNC`, `LATENCY`, `NETWORK_ANALYSIS`, or combined mode. |
| `environment` | object | Yes | Execution environment details. |
| `configuration` | object | Yes | Key validation settings. |
| `displays` | array | Yes | Per-display summary. |
| `checks` | object | Yes | Structured pass/fail checks. |
| `sync` | object | Required for sync tests | Offset summary and tolerance. |
| `latency` | object | Required for latency mode | Latency summary. |
| `networkCapture` | object | Required when capture configured | Packet capture status and file references. |
| `artifacts` | object | Yes | Relative paths to important artifacts. |
| `failure` | object or null | Yes | Failure details when applicable. |

### Example: Passing Synchronization Run

```json
{
  "schemaVersion": "1.0",
  "testRunId": "SYNC_RUN_20260605_001",
  "sequenceId": "SEQ_001",
  "sourceId": "CAMERA_SYNC_TEST",
  "protocol": "HLS",
  "mediaServer": "MediaMTX",
  "hlsUrl": "http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8",
  "startedAt": "2026-06-05T14:00:00.000Z",
  "completedAt": "2026-06-05T14:01:05.000Z",
  "durationMs": 65000,
  "status": "PASS",
  "mode": "SYNC",
  "environment": {
    "runner": "java",
    "runnerVersion": "0.1.0",
    "executionMode": "docker-compose",
    "os": "linux",
    "ci": false
  },
  "configuration": {
    "fps": 30,
    "durationSeconds": 60,
    "displayIds": ["DISPLAY_01", "DISPLAY_02", "DISPLAY_03"],
    "sampleIntervalMs": 500,
    "warmupSeconds": 5,
    "toleranceFrames": 3,
    "frozenDisplayTimeoutSeconds": 5,
    "networkMode": "docker",
    "impairmentProfile": "none"
  },
  "displays": [
    {
      "displayId": "DISPLAY_01",
      "status": "PASS",
      "markerDetected": true,
      "sourceMatched": true,
      "testRunMatched": true,
      "firstObservedFrame": 145,
      "lastObservedFrame": 1760,
      "samplesCollected": 110,
      "frozen": false,
      "lastScreenshot": "screenshots/display_01/final.jpg"
    },
    {
      "displayId": "DISPLAY_02",
      "status": "PASS",
      "markerDetected": true,
      "sourceMatched": true,
      "testRunMatched": true,
      "firstObservedFrame": 144,
      "lastObservedFrame": 1759,
      "samplesCollected": 110,
      "frozen": false,
      "lastScreenshot": "screenshots/display_02/final.jpg"
    },
    {
      "displayId": "DISPLAY_03",
      "status": "PASS",
      "markerDetected": true,
      "sourceMatched": true,
      "testRunMatched": true,
      "firstObservedFrame": 146,
      "lastObservedFrame": 1761,
      "samplesCollected": 110,
      "frozen": false,
      "lastScreenshot": "screenshots/display_03/final.jpg"
    }
  ],
  "checks": {
    "streamAvailability": "PASS",
    "displayLoad": "PASS",
    "markerDetection": "PASS",
    "sourceConsistency": "PASS",
    "testRunConsistency": "PASS",
    "frameProgression": "PASS",
    "frozenDisplayDetection": "PASS",
    "synchronization": "PASS",
    "artifactGeneration": "PASS"
  },
  "sync": {
    "toleranceFrames": 3,
    "fps": 30,
    "toleranceApproxMs": 100,
    "maxObservedOffsetFrames": 2,
    "averageOffsetFrames": 1.1,
    "samplesEvaluated": 100,
    "samplesFailed": 0,
    "offsetsCsv": "offsets.csv"
  },
  "latency": {
    "enabled": false
  },
  "networkCapture": {
    "enabled": true,
    "status": "CAPTURED",
    "files": [
      "network/full-run.pcap",
      "network/tshark-summary.txt",
      "network/capture-metadata.json"
    ]
  },
  "artifacts": {
    "report": "../../latest/index.html",
    "summary": "summary.json",
    "offsets": "offsets.csv",
    "markerSamples": "raw/marker-samples.jsonl",
    "screenshots": "screenshots/",
    "logs": "logs/",
    "network": "network/"
  },
  "failure": null
}
```

### Example: Failed Synchronization Run

```json
{
  "schemaVersion": "1.0",
  "testRunId": "SYNC_RUN_20260605_002",
  "sequenceId": "SEQ_001",
  "sourceId": "CAMERA_SYNC_TEST",
  "protocol": "HLS",
  "mediaServer": "MediaMTX",
  "hlsUrl": "http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_002/index.m3u8",
  "startedAt": "2026-06-05T14:10:00.000Z",
  "completedAt": "2026-06-05T14:11:05.000Z",
  "durationMs": 65000,
  "status": "FAIL",
  "mode": "SYNC",
  "environment": {
    "runner": "java",
    "runnerVersion": "0.1.0",
    "executionMode": "docker-compose",
    "ci": false
  },
  "configuration": {
    "fps": 30,
    "displayIds": ["DISPLAY_01", "DISPLAY_02", "DISPLAY_03"],
    "toleranceFrames": 3,
    "frozenDisplayTimeoutSeconds": 5,
    "networkMode": "docker"
  },
  "checks": {
    "streamAvailability": "PASS",
    "displayLoad": "PASS",
    "markerDetection": "PASS",
    "sourceConsistency": "PASS",
    "testRunConsistency": "PASS",
    "frameProgression": "PASS",
    "frozenDisplayDetection": "PASS",
    "synchronization": "FAIL",
    "artifactGeneration": "PASS"
  },
  "sync": {
    "toleranceFrames": 3,
    "fps": 30,
    "toleranceApproxMs": 100,
    "maxObservedOffsetFrames": 7,
    "averageOffsetFrames": 3.8,
    "samplesEvaluated": 100,
    "samplesFailed": 14,
    "offsetsCsv": "offsets.csv"
  },
  "failure": {
    "failureType": "SYNCHRONIZATION_TOLERANCE_EXCEEDED",
    "message": "Synchronization offset exceeded tolerance.",
    "displayIds": ["DISPLAY_01", "DISPLAY_02", "DISPLAY_03"],
    "expected": "maxFrameOffset <= 3",
    "actual": "maxFrameOffset = 7",
    "firstFailedAt": "2026-06-05T14:10:24.500Z",
    "failureSummary": "failure-summary.md",
    "stackTrace": "failure-stacktrace.txt"
  },
  "artifacts": {
    "report": "../../latest/index.html",
    "summary": "summary.json",
    "offsets": "offsets.csv",
    "markerSamples": "raw/marker-samples.jsonl",
    "screenshots": "screenshots/",
    "logs": "logs/",
    "network": "network/"
  }
}
```

---

## `offsets.csv` Format

`offsets.csv` stores synchronization measurements over time.

Each row represents one evaluated synchronization sample across the participating displays.

### Location

```text
artifacts/runs/{testRunId}/offsets.csv
artifacts/latest/offsets.csv
```

### Required Columns

| Column | Description |
|---|---|
| `sampleIndex` | Sequential sample number. |
| `sampleCapturedAt` | Timestamp when the sample was evaluated, ISO-8601 UTC. |
| `elapsedMs` | Milliseconds elapsed since validation start. |
| `testRunId` | Active test run ID. |
| `sequenceId` | Source sequence ID. |
| `sourceId` | Expected source ID. |
| `displayFrames` | Compact representation of display-to-frame values. |
| `minFrame` | Lowest decoded frame number among displays. |
| `maxFrame` | Highest decoded frame number among displays. |
| `maxOffsetFrames` | `maxFrame - minFrame`. |
| `fps` | Frames per second. |
| `maxOffsetMsApprox` | Approximate offset in milliseconds, calculated from FPS. |
| `toleranceFrames` | Configured frame tolerance. |
| `withinTolerance` | `true` or `false`. |
| `failedDisplayIds` | Displays contributing to failure, if known. |
| `notes` | Optional diagnostic note. |

### Example

```csv
sampleIndex,sampleCapturedAt,elapsedMs,testRunId,sequenceId,sourceId,displayFrames,minFrame,maxFrame,maxOffsetFrames,fps,maxOffsetMsApprox,toleranceFrames,withinTolerance,failedDisplayIds,notes
1,2026-06-05T14:00:06.000Z,1000,SYNC_RUN_20260605_001,SEQ_001,CAMERA_SYNC_TEST,"DISPLAY_01=180;DISPLAY_02=179;DISPLAY_03=181",179,181,2,30,66.67,3,true,,
2,2026-06-05T14:00:06.500Z,1500,SYNC_RUN_20260605_001,SEQ_001,CAMERA_SYNC_TEST,"DISPLAY_01=195;DISPLAY_02=192;DISPLAY_03=196",192,196,4,30,133.33,3,false,"DISPLAY_02","Offset exceeded tolerance"
```

### Rules

* `offsets.csv` should include only samples where all required displays produced a valid decoded marker.
* Samples with missing markers should be recorded in `raw/marker-samples.jsonl` and summarized in `summary.json`.
* Startup warm-up samples may be excluded from pass/fail evaluation, but should be recorded with a note if useful.
* `maxOffsetMsApprox` is approximate and should be derived from `maxOffsetFrames / fps * 1000`.
* The CSV must be readable by spreadsheet tools, Python, Node, Java, and CI systems.

---

## `latency.csv` Format

`latency.csv` stores observed render latency measurements when latency mode is enabled.

Latency mode is stricter than synchronization mode because it tries to capture the earliest visible frame after stream start.

### Location

```text
artifacts/runs/{testRunId}/latency.csv
artifacts/latest/latency.csv
```

### Required Columns

| Column | Description |
|---|---|
| `displayId` | Display identifier. |
| `testRunId` | Active test run ID. |
| `sequenceId` | Source sequence ID. |
| `sourceId` | Expected source ID. |
| `streamStartedAt` | Stream start timestamp, ISO-8601 UTC. |
| `firstCaptureAt` | Browser capture timestamp for first decoded marker. |
| `firstObservedFrame` | First decoded frame number. |
| `fps` | Frames per second. |
| `expectedPresentationEpochMs` | Expected presentation time encoded in marker. |
| `observedRenderLatencyMs` | `firstCaptureAt - expectedPresentationEpochMs`. |
| `requireFirstFrame` | Whether frame 0 or frame 1 policy is enabled. |
| `maxAllowedFirstFrame` | Maximum acceptable first observed frame when required. |
| `firstFramePolicyResult` | `PASS`, `FAIL`, or `NOT_APPLICABLE`. |
| `notes` | Optional diagnostic note. |

### Example

```csv
displayId,testRunId,sequenceId,sourceId,streamStartedAt,firstCaptureAt,firstObservedFrame,fps,expectedPresentationEpochMs,observedRenderLatencyMs,requireFirstFrame,maxAllowedFirstFrame,firstFramePolicyResult,notes
DISPLAY_01,SYNC_RUN_20260605_001,SEQ_001,CAMERA_SYNC_TEST,2026-06-05T14:00:00.000Z,2026-06-05T14:00:02.120Z,1,30,1780689600033,2087,true,1,PASS,
DISPLAY_02,SYNC_RUN_20260605_001,SEQ_001,CAMERA_SYNC_TEST,2026-06-05T14:00:00.000Z,2026-06-05T14:00:02.350Z,4,30,1780689600133,2217,true,1,FAIL,"First observed frame exceeded policy"
```

### Rules

* For HLS, this metric should be called **observed render latency** or **glass-to-browser latency**, not pure network latency.
* The value includes stream generation, encoding, MediaMTX processing, HLS segmenting, playlist/segment download, browser buffering, and browser rendering.
* If latency mode is disabled, `latency.csv` may be omitted or generated with only a header row. `summary.json` must still indicate that latency mode was disabled.

---

## Screenshots

Screenshots are visual evidence of what each browser display rendered.

They support debugging, stakeholder review, and correlation with decoded marker samples.

### Location

```text
artifacts/runs/{testRunId}/screenshots/
artifacts/latest/screenshots/
```

Recommended layout:

```text
screenshots/
  display_01/
    sample_000001.jpg
    sample_000002.jpg
    failure.jpg
    final.jpg
  display_02/
    sample_000001.jpg
    sample_000002.jpg
    failure.jpg
    final.jpg
  display_03/
    sample_000001.jpg
    sample_000002.jpg
    failure.jpg
    final.jpg
```

### Naming Rules

Use lowercase display folders to keep paths stable across operating systems.

Recommended screenshot naming pattern:

```text
sample_{sampleIndex}.jpg
failure.jpg
final.jpg
```

Examples:

```text
sample_000001.jpg
sample_000025.jpg
failure.jpg
final.jpg
```

### Minimum Screenshot Requirements

| Scenario | Required Screenshots |
|---|---|
| Passing sync run | At least one final screenshot per display. |
| Failed sync run | Failure screenshot for each affected display, plus final screenshot where possible. |
| Missing marker | Screenshot of the display where marker was not decoded. |
| Frozen display | Screenshot of the frozen display and last known progressing sample. |
| Source mismatch | Screenshot showing the wrong or stale source. |
| Latency mode | Screenshot or captured frame for first observed marker per display where possible. |

### Screenshot Metadata

Screenshot files should be referenced from `summary.json` and `raw/marker-samples.jsonl`.

Each screenshot reference should include:

* display ID;
* capture timestamp;
* sample index;
* decoded marker fields if available;
* reason for capture: `sample`, `failure`, `final`, or `first-observed`.

---

## Logs

Logs provide execution context for debugging failures.

### Location

```text
artifacts/runs/{testRunId}/logs/
artifacts/latest/logs/
```

### Recommended Logs

| Log File | Owner | Description |
|---|---|---|
| `test-runner.log` | Test framework | Test lifecycle, API calls, browser actions, assertions, artifact writing. |
| `ffmpeg.log` | Orchestration API | FFmpeg command, stream publishing output, process exit information. |
| `mediamtx.log` | Media server / infrastructure | Stream ingest, HLS serving, client connection details. |
| `control-api.log` | Orchestration API | Test run creation, sequence generation, stream lifecycle, network capture lifecycle. |
| `browser-console-{displayId}.log` | Test framework | Browser console messages for each display client. |
| `browser-network-{displayId}.log` | Optional | Browser-side request/response summary if captured through Playwright. |

### Log Rules

* Logs should be plain text.
* Logs should include timestamps.
* Logs should include the `testRunId` when possible.
* Logs should avoid secrets or environment-sensitive values.
* Logs should be written even when the run fails.
* The final report should link to key logs, especially `test-runner.log`, `ffmpeg.log`, and `control-api.log`.

---

## `failure-summary.md` Format

`failure-summary.md` is the human-readable explanation of why the run failed.

It should be useful for both senior stakeholders and engineers.

It should start with a concise summary, then provide technical detail and artifact links.

### Location

```text
artifacts/runs/{testRunId}/failure-summary.md
artifacts/latest/failure-summary.md
```

### Required When

`failure-summary.md` is required when:

* the test result is `FAIL`;
* the test result is `ERROR`;
* a required artifact is missing;
* the framework aborts after partial evidence collection.

It may be omitted for passing runs, or generated with:

```text
No failure detected.
```

### Recommended Template

```md
# Failure Summary

## Result

FAIL

## Primary Failure

Synchronization offset exceeded tolerance.

## Business-Level Impact

The displays did not remain synchronized within the configured tolerance. In a real multi-display streaming scenario, this could result in visibly inconsistent playback across screens.

## Technical Details

| Field | Value |
|---|---|
| Test Run ID | SYNC_RUN_20260605_002 |
| Source ID | CAMERA_SYNC_TEST |
| Sequence ID | SEQ_001 |
| Protocol | HLS |
| Tolerance | 3 frames |
| Max Observed Offset | 7 frames |
| Approx Offset | 233.33 ms |
| First Failed At | 2026-06-05T14:10:24.500Z |

## Affected Displays

| Display | Last Observed Frame | Status |
|---|---:|---|
| DISPLAY_01 | 780 | PASS |
| DISPLAY_02 | 773 | LAGGING |
| DISPLAY_03 | 779 | PASS |

## Most Relevant Evidence

| Evidence | Path |
|---|---|
| Summary | summary.json |
| Offsets | offsets.csv |
| Marker samples | raw/marker-samples.jsonl |
| DISPLAY_02 screenshot | screenshots/display_02/failure.jpg |
| Test runner log | logs/test-runner.log |
| FFmpeg log | logs/ffmpeg.log |
| Packet capture metadata | network/capture-metadata.json |

## Suggested Investigation Steps

1. Review `offsets.csv` around the first failed timestamp.
2. Compare marker samples for the affected display.
3. Open the failure screenshot for visual confirmation.
4. Review browser console logs for playback or buffering errors.
5. Review MediaMTX and FFmpeg logs for stream interruptions.
6. Review packet capture summaries if network capture was enabled.

## Stack Trace

See `failure-stacktrace.txt` if the failure was caused by a framework exception.
```

### Writing Rules

* Avoid vague failure messages such as “test failed.”
* State the failed rule explicitly.
* Include expected vs actual values.
* Include the affected displays.
* Link to the most relevant evidence files.
* Keep the first section understandable for non-technical stakeholders.
* Keep technical details structured for QA and engineering review.

---

## `failure-stacktrace.txt`

`failure-stacktrace.txt` is engineering-level evidence.

### Location

```text
artifacts/runs/{testRunId}/failure-stacktrace.txt
artifacts/latest/failure-stacktrace.txt
```

### Required When

It is required when:

* the framework throws an exception;
* a test assertion fails and the framework can capture the assertion stack;
* the run aborts unexpectedly;
* the CI test runner exits non-zero due to an exception.

### Rules

* Preserve the original stack trace.
* Do not replace the human-readable failure summary with only a stack trace.
* Do not expose secrets.
* Include the test class, method, assertion name, and root cause where available.

---

## Raw Marker Samples

`raw/marker-samples.jsonl` stores decoded marker samples from browser-rendered output.

This is one of the most important engineering artifacts because it explains how the framework reached its pass/fail decision.

### Location

```text
artifacts/runs/{testRunId}/raw/marker-samples.jsonl
artifacts/latest/raw/marker-samples.jsonl
```

### Format

Use JSON Lines. Each line is one captured sample.

### Example

```json
{"sampleIndex":1,"capturedAt":"2026-06-05T14:00:06.000Z","elapsedMs":1000,"displayId":"DISPLAY_01","markerDetected":true,"source":"CAMERA_SYNC_TEST","testRunId":"SYNC_RUN_20260605_001","sequenceId":"SEQ_001","frame":180,"fps":30,"presentationTimeMs":6000,"expectedPresentationEpochMs":1780689606000,"screenshot":"screenshots/display_01/sample_000001.jpg"}
{"sampleIndex":1,"capturedAt":"2026-06-05T14:00:06.000Z","elapsedMs":1000,"displayId":"DISPLAY_02","markerDetected":true,"source":"CAMERA_SYNC_TEST","testRunId":"SYNC_RUN_20260605_001","sequenceId":"SEQ_001","frame":179,"fps":30,"presentationTimeMs":5966.667,"expectedPresentationEpochMs":1780689605966,"screenshot":"screenshots/display_02/sample_000001.jpg"}
{"sampleIndex":1,"capturedAt":"2026-06-05T14:00:06.000Z","elapsedMs":1000,"displayId":"DISPLAY_03","markerDetected":true,"source":"CAMERA_SYNC_TEST","testRunId":"SYNC_RUN_20260605_001","sequenceId":"SEQ_001","frame":181,"fps":30,"presentationTimeMs":6033.333,"expectedPresentationEpochMs":1780689606033,"screenshot":"screenshots/display_03/sample_000001.jpg"}
```

### Rules

* Store all useful samples, not only failed samples.
* Include failed marker-decoding attempts where useful.
* Reference screenshot paths when available.
* Keep field names consistent across Java, Node/TypeScript, and Python.

---

## Network Evidence

Network evidence supports investigation of HLS behavior, packet timing, segment downloads, and future network degradation scenarios.

It must not replace deterministic assertions based on marker decoding and frame analysis.

### Location

```text
artifacts/runs/{testRunId}/network/
artifacts/latest/network/
```

### Recommended Files

```text
network/
  full-run.pcap
  mediamtx-interface.pcap
  display-client-traffic.pcap
  tshark-summary.txt
  tshark-http-requests.json
  capture-metadata.json
```

### `capture-metadata.json` Example

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "captureEnabled": true,
  "captureTool": "tcpdump",
  "captureStartedAt": "2026-06-05T14:00:00.000Z",
  "captureStoppedAt": "2026-06-05T14:01:00.000Z",
  "interfaces": ["eth0"],
  "filters": ["port 8888", "port 8554"],
  "files": [
    "network/full-run.pcap",
    "network/tshark-summary.txt",
    "network/tshark-http-requests.json"
  ]
}
```

### Rules

* Packet capture is supporting evidence.
* The test should fail if network capture is configured as required but expected capture files are missing.
* Packet capture should be linked from the latest report when enabled.
* Future impairment profiles should be recorded in `summary.json` and capture metadata.

---

## Source Sequence Evidence

The generated source sequence is reference evidence for deterministic validation.

### Required Files

```text
generated-frames/
  frame_000000.png
  frame_000001.png
  frame_000002.png

source-sequence/
  sequence-manifest.json
```

### `sequence-manifest.json` Example

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "source": "CAMERA_SYNC_TEST",
  "sequenceId": "SEQ_001",
  "fps": 30,
  "frameCount": 1800,
  "durationSeconds": 60,
  "resolution": "1280x720",
  "firstFrame": 0,
  "lastFrame": 1799,
  "framePattern": "generated-frames/frame_%06d.png",
  "mediaPath": "camera_sync_test_SYNC_RUN_20260605_001",
  "hlsUrl": "http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8"
}
```

The test framework should read this manifest and use it as reference evidence for:

* source ID;
* test run ID;
* sequence ID;
* FPS;
* frame count;
* expected frame range;
* stream URL;
* latency calculations.

---

## Comparable Evidence Across Java, Node/TypeScript, and Python

Java is the first framework implementation, but the evidence format must be language-neutral.

Any future Node/TypeScript or Python implementation should produce the same evidence structure and field names.

### Language-Neutral Contract

All implementations must follow these rules:

| Area | Required Contract |
|---|---|
| Folder structure | Use the same `artifacts/runs/{testRunId}` and `artifacts/latest` layout. |
| Summary schema | Write compatible `summary.json` using the same field names and status values. |
| CSV headers | Use the same headers for `offsets.csv` and `latency.csv`. |
| Timestamps | Use ISO-8601 UTC strings for human-readable timestamps. |
| Epoch values | Use milliseconds for machine timing fields. |
| Status values | Use `PASS`, `FAIL`, `ERROR`, `ABORTED`, or `SKIPPED`. |
| Booleans | Use JSON booleans in JSON and lowercase `true`/`false` in CSV. |
| Display IDs | Use consistent display IDs such as `DISPLAY_01`. |
| Artifact paths | Use relative paths from the run folder. |
| Marker samples | Use JSON Lines for raw marker samples. |
| Screenshots | Use stable per-display folders and predictable filenames. |
| Failure summaries | Use the same Markdown sections where possible. |

### Java Responsibilities

The Java implementation should initially produce:

* `summary.json`;
* `offsets.csv`;
* `latency.csv` when latency mode is enabled;
* screenshots from Playwright;
* `raw/marker-samples.jsonl`;
* `failure-summary.md`;
* `failure-stacktrace.txt`;
* `test-runner.log`;
* `latest/index.html`.

Java should also verify that orchestration-owned artifacts exist:

* `generated-frames/`;
* `source-sequence/sequence-manifest.json`;
* `logs/ffmpeg.log`;
* `logs/control-api.log`;
* `network/capture-metadata.json` when network capture is enabled.

### Node/TypeScript Responsibilities

A future Node/TypeScript implementation should:

* use the same configuration values;
* use Playwright to capture comparable screenshots;
* decode the same marker payload;
* write the same JSON and CSV schemas;
* produce equivalent reports;
* preserve the same pass/fail rules.

Node/TypeScript may be useful for browser-heavy experimentation, but it should not create a different evidence model.

### Python Responsibilities

A future Python implementation should:

* consume the same `summary.json`, `offsets.csv`, and marker samples;
* produce the same schemas if it runs tests directly;
* support OpenCV or QR-marker analysis where useful;
* support packet-capture post-processing;
* support report and artifact analysis;
* avoid changing the deterministic pass/fail contract.

Python is a good fit for computer vision and post-run analysis, but its output must remain compatible with the shared evidence format.

---

## Artifact Validation Rules

At the end of every run, the framework should validate required evidence before returning the final result.

### Minimum Required Artifacts for Synchronization Mode

| Artifact | Required |
|---|---:|
| `summary.json` | Yes |
| `offsets.csv` | Yes |
| `raw/marker-samples.jsonl` | Yes |
| `screenshots/` | Yes |
| `logs/test-runner.log` | Yes |
| `source-sequence/sequence-manifest.json` | Yes |
| `latest/index.html` | Yes |

### Minimum Required Artifacts for Latency Mode

| Artifact | Required |
|---|---:|
| `summary.json` | Yes |
| `latency.csv` | Yes |
| `raw/marker-samples.jsonl` | Yes |
| first-observed screenshot or frame evidence | Expected |
| `source-sequence/sequence-manifest.json` | Yes |
| `latest/index.html` | Yes |

### Minimum Required Artifacts for Network Capture Mode

| Artifact | Required |
|---|---:|
| `network/capture-metadata.json` | Yes |
| at least one `.pcap` file | Yes if capture is required |
| `network/tshark-summary.txt` | Expected if `tshark` is configured |
| `network/tshark-http-requests.json` | Expected if HTTP summary is configured |

### Missing Artifact Handling

If a required artifact is missing, the run should be marked as `FAIL` or `ERROR`.

Example failure type:

```text
REQUIRED_ARTIFACT_MISSING
```

Example failure message:

```text
Required artifact missing: raw/marker-samples.jsonl
```

The failure should be recorded in:

* `summary.json`;
* `failure-summary.md`;
* `test-runner.log`.

---

## Latest Report Expectations

The latest report is not the primary schema, but it is the main review entry point.

```text
artifacts/latest/index.html
```

It should include:

* overall result;
* test run ID;
* source ID;
* protocol;
* media server;
* network mode;
* impairment profile when applicable;
* display IDs;
* validation mode;
* source consistency result;
* test run consistency result;
* frame progression result;
* frozen display status;
* max frame offset;
* average frame offset;
* configured tolerance;
* latency summary when enabled;
* first observed frame per display when latency mode is enabled;
* packet-capture links when enabled;
* screenshots;
* failure summary when applicable;
* stack trace link when applicable;
* links to raw artifacts.

The report should be understandable without opening the source code.

---

## CI/CD Artifact Handling

CI should preserve evidence even when the test fails.

Recommended behavior:

* run framework verification and streaming E2E workflows separately;
* write artifacts under `artifacts/`;
* upload the full `artifacts/` folder with `if: always()`;
* fail the workflow using the test-runner exit code;
* keep reports available for failed runs;
* optionally publish `artifacts/latest/` through GitHub Pages later.

The test-runner exit code and the evidence result should align:

| Exit Code | Expected Evidence Result |
|---:|---|
| `0` | `summary.json.status = PASS` |
| Non-zero | `summary.json.status = FAIL`, `ERROR`, or `ABORTED` |

---

## Implementation Checklist

Before implementation, the team should confirm:

* [ ] The `artifacts/runs/{testRunId}` structure is accepted.
* [ ] The `artifacts/latest` structure is accepted.
* [ ] `summary.json` required fields are stable enough for MVP.
* [ ] `offsets.csv` headers are stable.
* [ ] `latency.csv` headers are stable.
* [ ] Screenshot naming is accepted.
* [ ] Log ownership is clear.
* [ ] Failure summary template is accepted.
* [ ] Java will implement the first writer.
* [ ] Node/TypeScript and Python will follow the same schemas later.
* [ ] CI will upload `artifacts/` even when E2E tests fail.

---

## Final Decision

The project will use a shared, language-neutral evidence format.

Java will produce the first implementation, but the artifact structure and schemas must be stable enough for future Node/TypeScript and Python implementations to produce comparable evidence.

The evidence model should make the project credible to both QA reviewers and senior stakeholders by showing not only whether the test passed or failed, but also what was observed, how the result was calculated, and where to inspect the raw evidence.
