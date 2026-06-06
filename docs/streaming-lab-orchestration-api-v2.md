# Streaming Lab Orchestration API

## Purpose

This document defines the service layer responsible for preparing and controlling the streaming test lab environment.

The service is called the **Streaming Lab Orchestration API**.

Its purpose is to provide a controlled, repeatable, run-specific streaming environment for automated validation.

This service is not the test framework and is not the deterministic test oracle. It is a test-lab service provider that prepares the video source, starts and stops streaming components, manages packet capture, and exposes run metadata to the automation framework.

The Java test framework remains responsible for browser automation, frame capture, marker decoding, assertions, pass/fail decisions, and final report validation.

---

## Scope

The Streaming Lab Orchestration API is responsible for the lifecycle of a test run from the infrastructure and stream-generation perspective.

It owns:

* creating a test run ID;
* generating run-specific synthetic frames;
* embedding marker metadata into generated frames;
* producing a sequence manifest;
* starting FFmpeg using the generated frame sequence;
* publishing the stream to MediaMTX;
* exposing the run-specific HLS URL;
* starting and stopping packet capture;
* exposing stream status;
* stopping stream processes;
* exposing artifact locations.

It does not own:

* browser automation;
* screenshot or rendered-frame capture from browser clients;
* marker decoding from browser output;
* synchronization assertions;
* latency assertions;
* frozen display detection;
* pass/fail decisions;
* final test result interpretation.

---

## Position in the Architecture

The service sits between the test framework and the streaming infrastructure.

```text
Java Test Framework
    ↓
Streaming Lab Orchestration API
    ↓
Run-specific frame generation
    ↓
FFmpeg
    ↓
MediaMTX
    ↓
HLS endpoint
    ↓
Browser Display Clients
```

The framework calls the orchestration API to prepare and control the test environment.

The browser display clients render the stream.

The framework captures browser output and validates the result.

---

## Naming Rationale

The name **Streaming Lab Orchestration API** is intentionally explicit.

It communicates that the service:

* belongs to the test lab;
* orchestrates infrastructure and stream lifecycle;
* is not the deterministic assertion layer;
* is not necessarily the real product API;
* simulates the type of stream-control layer that a real product might expose.

Short internal names may be used in code, such as:

```text
stream-orchestrator
stream-lab-api
streaming-lab-api
```

Recommended repository folder:

```text
stream-orchestrator/
```

---

## Core Design Decision

The service will use **pre-generated, run-specific synthetic frames** as the target video generation strategy.

For every test run, the service creates a unique `testRunId` and generates a deterministic PNG frame sequence for that run.

Each generated frame contains:

* a machine-readable marker, preferably QR code;
* human-readable debug text;
* source ID;
* test run ID;
* sequence ID;
* frame number;
* FPS;
* presentation time;
* expected presentation timestamp.

The generated frames are then streamed through FFmpeg into MediaMTX.

This approach is preferred because it supports:

* deterministic frame numbers;
* run-specific content;
* stale-stream detection;
* first-frame validation;
* latency measurement;
* reproducible artifacts;
* comparison between generated source frames and captured browser frames.

---

## Target Streaming Flow

```text
testRunId
  ↓
Generate PNG frames with QR metadata
  ↓
Write sequence-manifest.json
  ↓
Start packet capture, if enabled
  ↓
Start FFmpeg with image sequence input
  ↓
FFmpeg publishes to MediaMTX
  ↓
MediaMTX exposes run-specific HLS URL
  ↓
Browser display clients load HLS stream
  ↓
Java test framework validates browser-rendered output
```

---

## Run-Specific Stream Strategy

Each run should receive a unique media path to prevent stale content or old playlist reuse.

Example values:

```text
sourceId: CAMERA_SYNC_TEST
testRunId: SYNC_RUN_20260605_001
sequenceId: SEQ_001
mediaPath: camera_sync_test_SYNC_RUN_20260605_001
```

RTSP publish URL:

```text
rtsp://mediamtx:8554/camera_sync_test_SYNC_RUN_20260605_001
```

HLS playback URL:

```text
http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8
```

This makes every test run independently identifiable.

The display client or test framework can verify that the browser-rendered frame contains the same `testRunId` that was created for the run.

---

## Frame Generation Strategy

The orchestration API should generate frames before streaming begins.

Example generated frame folder:

```text
artifacts/
  runs/
    SYNC_RUN_20260605_001/
      generated-frames/
        frame_000000.png
        frame_000001.png
        frame_000002.png
```

The sequence should start at frame 0.

Example file naming pattern:

```text
frame_%06d.png
```

Example generated files:

```text
frame_000000.png
frame_000001.png
frame_000002.png
frame_000003.png
```

---

## Frame Marker Metadata

Each frame should encode structured metadata in a machine-readable marker.

Recommended marker payload:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_20260605_001",
  "sequenceId": "SEQ_001",
  "frame": 0,
  "fps": 30,
  "presentationTimeMs": 0,
  "expectedPresentationEpochMs": 1780689600000
}
```

For frame 1 at 30 FPS:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_20260605_001",
  "sequenceId": "SEQ_001",
  "frame": 1,
  "fps": 30,
  "presentationTimeMs": 33.333,
  "expectedPresentationEpochMs": 1780689600033
}
```

The field `expectedPresentationEpochMs` should represent the expected presentation time relative to the stream start.

It should not represent when the PNG file was created.

---

## Human-Readable Frame Text

Each generated frame should also include human-readable text for debugging.

Example:

```text
SOURCE: CAMERA_SYNC_TEST
TEST RUN: SYNC_RUN_20260605_001
SEQUENCE: SEQ_001
FRAME: 000000
FPS: 30
PRESENTATION: 0 ms
EXPECTED PRESENTATION: 2026-06-05T14:00:00.000Z
```

The QR code is the primary automation oracle.

The readable text is supporting evidence for screenshots, reports, and manual inspection.

---

## Sequence Manifest

The service should generate a sequence manifest with metadata about the generated source frames.

Recommended file:

```text
artifacts/runs/{testRunId}/source-sequence/sequence-manifest.json
```

Example:

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

The test framework can use this manifest as reference evidence when validating browser-rendered frames.

---

## FFmpeg Streaming Strategy

After generating the frame sequence, the orchestration API starts FFmpeg using the generated images as input.

Example FFmpeg command:

```bash
ffmpeg -re   -framerate 30   -i artifacts/runs/SYNC_RUN_20260605_001/generated-frames/frame_%06d.png   -c:v libx264   -preset veryfast   -tune zerolatency   -pix_fmt yuv420p   -f rtsp   rtsp://mediamtx:8554/camera_sync_test_SYNC_RUN_20260605_001
```

MediaMTX then exposes the stream as HLS:

```text
http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8
```

The exact FFmpeg command may evolve, but the service should preserve the principle:

```text
Generated frame sequence → FFmpeg → MediaMTX → HLS
```

---

## API Lifecycle

Recommended lifecycle:

```text
1. Create test run.
2. Generate run-specific frame sequence.
3. Start packet capture, if enabled.
4. Start stream.
5. Wait until HLS endpoint is ready.
6. Return stream status and HLS URL.
7. Stop stream after test completion.
8. Stop packet capture.
9. Expose artifact locations.
```

---

## API Endpoints

### Create Test Run

```http
POST /api/test-runs
```

Creates a new test run.

Example request:

```json
{
  "sourceId": "CAMERA_SYNC_TEST",
  "requestedBy": "java-test-framework"
}
```

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "sourceId": "CAMERA_SYNC_TEST",
  "status": "CREATED",
  "createdAt": "2026-06-05T14:00:00.000Z"
}
```

---

### Generate Frame Sequence

```http
POST /api/test-runs/{testRunId}/sequence/generate
```

Generates the deterministic PNG frame sequence for the run.

Example request:

```json
{
  "sourceId": "CAMERA_SYNC_TEST",
  "fps": 30,
  "durationSeconds": 60,
  "resolution": "1280x720",
  "markerType": "QR",
  "startFrame": 0
}
```

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "sourceId": "CAMERA_SYNC_TEST",
  "sequenceId": "SEQ_001",
  "status": "SEQUENCE_GENERATED",
  "frameDirectory": "artifacts/runs/SYNC_RUN_20260605_001/generated-frames",
  "manifestPath": "artifacts/runs/SYNC_RUN_20260605_001/source-sequence/sequence-manifest.json",
  "frameCount": 1800,
  "firstFrame": 0,
  "lastFrame": 1799
}
```

---

### Start Network Capture

```http
POST /api/test-runs/{testRunId}/network-capture/start
```

Starts packet capture for the run.

Example request:

```json
{
  "enabled": true,
  "tool": "tcpdump",
  "interfaces": ["eth0"],
  "filters": ["port 8888", "port 8554"]
}
```

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "CAPTURING",
  "captureStartedAt": "2026-06-05T14:00:01.000Z",
  "captureFiles": [
    "artifacts/runs/SYNC_RUN_20260605_001/network/full-run.pcap"
  ]
}
```

---

### Start Stream

```http
POST /api/test-runs/{testRunId}/stream/start
```

Starts FFmpeg using the generated frame sequence.

Example request:

```json
{
  "protocol": "HLS",
  "publishProtocol": "RTSP",
  "mediaServer": "MediaMTX"
}
```

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "sourceId": "CAMERA_SYNC_TEST",
  "sequenceId": "SEQ_001",
  "status": "STREAMING",
  "mediaPath": "camera_sync_test_SYNC_RUN_20260605_001",
  "rtspPublishUrl": "rtsp://mediamtx:8554/camera_sync_test_SYNC_RUN_20260605_001",
  "hlsUrl": "http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8",
  "streamStartedAt": "2026-06-05T14:00:02.000Z"
}
```

---

### Get Stream Status

```http
GET /api/test-runs/{testRunId}/stream/status
```

Returns stream status and readiness information.

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "STREAMING",
  "ffmpegPid": 12345,
  "mediaPath": "camera_sync_test_SYNC_RUN_20260605_001",
  "hlsReady": true,
  "hlsUrl": "http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8",
  "lastCheckedAt": "2026-06-05T14:00:05.000Z"
}
```

---

### Stop Stream

```http
POST /api/test-runs/{testRunId}/stream/stop
```

Stops FFmpeg for the run.

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "STREAM_STOPPED",
  "streamStoppedAt": "2026-06-05T14:01:02.000Z"
}
```

---

### Stop Network Capture

```http
POST /api/test-runs/{testRunId}/network-capture/stop
```

Stops packet capture and optionally generates summaries.

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "CAPTURE_STOPPED",
  "captureStoppedAt": "2026-06-05T14:01:03.000Z",
  "files": [
    "artifacts/runs/SYNC_RUN_20260605_001/network/full-run.pcap",
    "artifacts/runs/SYNC_RUN_20260605_001/network/tshark-summary.txt"
  ]
}
```

---

### Get Artifacts

```http
GET /api/test-runs/{testRunId}/artifacts
```

Returns known artifact locations for the run.

Example response:

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "artifactRoot": "artifacts/runs/SYNC_RUN_20260605_001",
  "generatedFrames": "artifacts/runs/SYNC_RUN_20260605_001/generated-frames",
  "sequenceManifest": "artifacts/runs/SYNC_RUN_20260605_001/source-sequence/sequence-manifest.json",
  "network": "artifacts/runs/SYNC_RUN_20260605_001/network",
  "logs": "artifacts/runs/SYNC_RUN_20260605_001/logs"
}
```

---

## State Model

A test run should follow a predictable state model.

Recommended states:

```text
CREATED
SEQUENCE_GENERATED
CAPTURING
STREAMING
STREAM_STOPPED
CAPTURE_STOPPED
COMPLETED
FAILED
```

Example state flow:

```text
CREATED
  ↓
SEQUENCE_GENERATED
  ↓
CAPTURING
  ↓
STREAMING
  ↓
STREAM_STOPPED
  ↓
CAPTURE_STOPPED
  ↓
COMPLETED
```

Failure may occur at any step.

The service should expose clear error messages when state transitions fail.

---

## Artifact Ownership

The Streaming Lab Orchestration API owns infrastructure and source-generation artifacts.

It should create or write:

```text
artifacts/
  runs/
    {testRunId}/
      generated-frames/
      source-sequence/
        sequence-manifest.json
      network/
        full-run.pcap
        tshark-summary.txt
        tshark-http-requests.json
        capture-metadata.json
      logs/
        ffmpeg.log
        mediamtx.log
        control-api.log
```

The Java test framework owns validation artifacts.

It should create or write:

```text
artifacts/
  runs/
    {testRunId}/
      summary.json
      offsets.csv
      latency.csv
      screenshots/
      raw/
        marker-samples.jsonl
      failure-summary.md
      failure-stacktrace.txt
```

The final report may be generated by the Java test framework, or by a dedicated report module, but the deterministic test result should come from the test framework.

---

## Responsibility Boundary

| Responsibility | Streaming Lab Orchestration API | Java Test Framework |
|---|---:|---:|
| Create test run ID | Yes | Calls API |
| Generate run-specific frames | Yes | No |
| Embed QR marker metadata | Yes | No |
| Write sequence manifest | Yes | Reads/uses |
| Start FFmpeg | Yes | No |
| Publish to MediaMTX | Yes | No |
| Return HLS URL | Yes | Uses URL |
| Start packet capture | Yes | Calls API |
| Stop packet capture | Yes | Calls API |
| Open browser display clients | Optional | Yes |
| Capture browser output | No | Yes |
| Decode rendered markers | No | Yes |
| Validate source/test run ID | No | Yes |
| Validate frame progression | No | Yes |
| Calculate sync offset | No | Yes |
| Calculate observed render latency | No | Yes |
| Decide pass/fail | No | Yes |
| Generate final validation report | Optional support | Yes |

---

## Error Handling

The service should return clear failure information.

Examples:

### Frame Generation Failure

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "FAILED",
  "errorCode": "FRAME_GENERATION_FAILED",
  "message": "Unable to generate frame sequence.",
  "details": "QR marker generation failed for frame 000042."
}
```

### Stream Start Failure

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "FAILED",
  "errorCode": "STREAM_START_FAILED",
  "message": "Unable to start FFmpeg stream.",
  "details": "FFmpeg exited with code 1. See logs/ffmpeg.log."
}
```

### HLS Readiness Failure

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "FAILED",
  "errorCode": "HLS_NOT_READY",
  "message": "HLS endpoint did not become ready before timeout.",
  "hlsUrl": "http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8"
}
```

### Network Capture Failure

```json
{
  "testRunId": "SYNC_RUN_20260605_001",
  "status": "FAILED",
  "errorCode": "NETWORK_CAPTURE_FAILED",
  "message": "Unable to start packet capture.",
  "details": "tcpdump permission denied on interface eth0."
}
```

---

## Configuration

The service should read configuration from a shared config file when possible.

Example:

```yaml
orchestration:
  artifactRoot: artifacts
  generatedFramesDir: generated-frames
  sourceSequenceDir: source-sequence
  logsDir: logs

stream:
  sourceId: CAMERA_SYNC_TEST
  fps: 30
  resolution: 1280x720
  durationSeconds: 60
  protocol: HLS
  publishProtocol: RTSP
  mediaServer: MediaMTX

marker:
  type: QR
  includeHumanReadableText: true

networkCapture:
  enabled: true
  tool: tcpdump
  interfaces:
    - eth0
  filters:
    - port 8888
    - port 8554
```

---

---

## Cloud-Native Service Role

The Streaming Lab Orchestration API should be implemented as a **long-running cloud-native service**.

Its role is different from the Java test framework.

```text
Streaming Lab Orchestration API = long-running service
Java Test Framework = ephemeral test-runner job
```

The orchestration service should be available before the test runner starts. The test runner calls the service to create test runs, generate frame sequences, start streams, stop streams, and retrieve artifact locations.

The orchestration service should expose stable HTTP endpoints and health/readiness checks so that Docker Compose, CI, and future Kubernetes deployments can determine whether the service is ready.

Recommended health endpoints:

```http
GET /actuator/health
GET /actuator/info
GET /api/ready
```

The service should be designed so it can run:

* locally from the IDE;
* locally through Docker Compose;
* in GitHub Actions;
* later as a Kubernetes Deployment.

---

## Containerization Strategy

The Streaming Lab Orchestration API should be packaged as a Docker container.

Recommended internal service name:

```text
stream-orchestrator
```

Recommended container role:

```text
A long-running API service that controls stream preparation, frame generation, FFmpeg lifecycle, packet capture, and artifact locations.
```

The service should use environment variables or configuration files for runtime settings such as:

* artifact root;
* MediaMTX hostname;
* MediaMTX RTSP port;
* HLS external base URL;
* frame output directory;
* FFmpeg path;
* network-capture settings;
* service port.

Example Docker Compose configuration:

```yaml
services:
  stream-orchestrator:
    build:
      context: ./stream-orchestrator
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      ARTIFACTS_DIR: /app/artifacts
      MEDIAMTX_RTSP_BASE_URL: rtsp://mediamtx:8554
      MEDIAMTX_HLS_BASE_URL: http://mediamtx:8888
      STREAM_SOURCE_ID: CAMERA_SYNC_TEST
    volumes:
      - ./artifacts:/app/artifacts
    depends_on:
      - mediamtx

  mediamtx:
    image: bluenviron/mediamtx:latest
    ports:
      - "8554:8554"
      - "8888:8888"
```

Inside Docker Compose, the orchestration service should call MediaMTX through the Docker service name:

```text
rtsp://mediamtx:8554
http://mediamtx:8888
```

It should not use `localhost` for container-to-container communication.

---

## Integration with the Cloud-Native Test Runner

The Java test framework should run as a separate containerized test runner.

The orchestration service should not execute the Java tests directly.

The expected Docker Compose relationship is:

```text
stream-orchestrator = service
mediamtx = service
display-client = service
streamsync-test-runner = ephemeral job
```

The test runner should call the orchestration service using the Docker service name:

```text
http://stream-orchestrator:8080
```

Example E2E execution flow:

```text
docker compose up -d stream-orchestrator mediamtx display-client
docker compose run --rm streamsync-test-runner mvn verify -Pe2e
docker compose down
```

The orchestration service prepares and controls the streaming environment.

The test runner validates the rendered video behavior and exits with a pass/fail status code.

---

## Service Discovery and URL Strategy

The orchestration service should distinguish between internal container URLs and externally visible URLs.

For example, inside Docker Compose:

```text
Internal RTSP publish URL:
rtsp://mediamtx:8554/camera_sync_test_SYNC_RUN_20260605_001

Internal HLS URL:
http://mediamtx:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8
```

For browser clients running from the host machine, the externally visible URL may be:

```text
http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8
```

For browser clients running inside the Playwright test-runner container, the framework should normally use the internal Docker service URL:

```text
http://mediamtx:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8
```

The orchestration API may expose both values when needed:

```json
{
  "internalHlsUrl": "http://mediamtx:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8",
  "externalHlsUrl": "http://localhost:8888/camera_sync_test_SYNC_RUN_20260605_001/index.m3u8"
}
```

The Java framework should select the appropriate URL based on execution mode.

---

## Operational Logging

The orchestration service should produce structured logs.

Every log line related to a test run should include:

* service name;
* test run ID;
* source ID;
* operation;
* state;
* timestamp;
* error code when applicable.

Example structured log fields:

```json
{
  "service": "stream-orchestrator",
  "testRunId": "SYNC_RUN_20260605_001",
  "operation": "stream.start",
  "status": "STREAMING",
  "mediaPath": "camera_sync_test_SYNC_RUN_20260605_001"
}
```

This makes the service easier to debug locally, in CI, and in a future Kubernetes environment.

## Security and Safety Boundary

This service is intended for a local test lab.

It should not be exposed publicly without additional controls.

Recommended local safeguards:

* bind to localhost by default;
* avoid arbitrary command execution from request payloads;
* whitelist supported FFmpeg arguments;
* validate `testRunId` and paths;
* prevent path traversal;
* restrict packet-capture interfaces;
* write artifacts only under the configured artifact root;
* log all lifecycle operations.

---

## Initial Implementation Recommendation

The first implementation should support the following minimum capabilities:

1. create a test run;
2. generate a small frame sequence for that run;
3. write a sequence manifest;
4. start FFmpeg using the generated frames;
5. publish to MediaMTX;
6. return the HLS URL;
7. stop FFmpeg;
8. write FFmpeg logs;
9. expose artifact locations.

Network capture can be added immediately after the streaming lifecycle is stable.

Recommended MVP endpoints:

```text
POST /api/test-runs
POST /api/test-runs/{testRunId}/sequence/generate
POST /api/test-runs/{testRunId}/stream/start
GET  /api/test-runs/{testRunId}/stream/status
POST /api/test-runs/{testRunId}/stream/stop
GET  /api/test-runs/{testRunId}/artifacts
```

Recommended next endpoints:

```text
POST /api/test-runs/{testRunId}/network-capture/start
POST /api/test-runs/{testRunId}/network-capture/stop
```

---

## Future Extensions

Future versions may add:

* WebRTC stream start and status endpoints;
* network impairment profiles;
* display registration;
* display arming;
* stream restart scenarios;
* media server restart scenarios;
* cleanup policies for old generated frames;
* artifact compression;
* comparison between generated source frames and captured browser frames;
* GitHub Actions integration;
* GitHub Pages report publishing;
* Gen AI-assisted post-run explanation.

---

## Summary

The Streaming Lab Orchestration API is a cloud-native test-lab service provider.

It runs as a long-running service and prepares a run-specific streaming environment by creating a test run, generating deterministic synthetic frames, starting FFmpeg, publishing to MediaMTX, exposing an HLS URL, managing packet capture, and exposing artifact locations.

Its most important design boundary is that it controls the environment but does not validate the result.

The Java test framework remains responsible for observing browser-rendered output, decoding markers, calculating synchronization and latency, detecting failures, and deciding pass/fail.

The final target flow is:

```text
testRunId
  ↓
generated PNG frames with QR metadata
  ↓
FFmpeg image sequence input
  ↓
MediaMTX
  ↓
HLS endpoint
  ↓
browser display clients
  ↓
Java test framework validation
```
