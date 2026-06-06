# Streaming Plan

## Purpose

This document explains the streaming approach for the **Multi-Display Stream Synchronization Test Lab**.

The goal of the streaming design is to provide a controlled, repeatable, network-based video source that can be rendered by multiple browser-based display clients and validated by automated tests.

The first implementation uses:

```text
FFmpeg → MediaMTX → HLS → Browser Display Clients
```

This decision keeps the first version practical, testable, and demo-friendly while leaving room for future WebRTC and network degradation testing.

---

## Streaming Decision Summary

The project will start with **HLS** as the first streaming protocol.

WebRTC will be treated as a future extension after the project proves the core automation strategy:

* generate a controlled synthetic video stream;
* serve the stream through a media server;
* render the stream in multiple browser clients;
* capture displayed frames or screenshots;
* decode frame markers;
* validate source consistency;
* validate frame progression;
* compare frame offsets between displays;
* store test artifacts and demo reports.

The initial goal is not to build a production-grade low-latency streaming platform. The initial goal is to prove that automated tests can validate multi-display streaming behavior in a measurable and repeatable way.

---

## Why HLS First?

HLS is selected first because it is the most practical protocol for the initial proof of concept.

HLS provides a simple HTTP-based streaming path that works well with browser clients and local infrastructure. It also fits naturally with MediaMTX, which can receive a published stream and expose it as HLS.

Starting with HLS reduces early complexity and allows the project to focus on the most important testing problem: whether automation can verify that multiple display clients are showing the correct stream and staying within an acceptable synchronization tolerance.

### Main reasons for starting with HLS

| Reason                    | Explanation                                                                                                                                 |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Browser-friendly          | HLS can be played in browser-based display clients using `hls.js`.                                                                          |
| Easier to debug           | HLS uses HTTP-based playlists and media segments, which are easier to inspect than real-time peer connections.                              |
| Good local demo fit       | HLS works well in a local Docker-based proof of concept.                                                                                    |
| MediaMTX support          | MediaMTX can expose a published stream through HLS endpoints.                                                                               |
| Lower implementation risk | The first milestone can focus on test automation, artifacts, and synchronization analysis rather than signaling and connection negotiation. |
| Clear upgrade path        | Once the test strategy works with HLS, the same validation concepts can later be applied to WebRTC.                                         |

### Known limitation

HLS is not the lowest-latency protocol.

That is acceptable for the first version because the project is not yet trying to prove real-time ultra-low-latency synchronization. The first version is trying to prove that the test framework can validate:

* stream availability;
* source correctness;
* frame progression;
* frame offset between displays;
* frozen or stale display behavior;
* useful artifact collection;
* report availability after execution.

---

## Why WebRTC Later?

WebRTC is planned as a future protocol because it is more suitable for low-latency and real-time streaming scenarios.

However, WebRTC introduces additional complexity that should not be part of the first milestone.

### Reasons to defer WebRTC

| Reason                   | Explanation                                                                                                     |
| ------------------------ | --------------------------------------------------------------------------------------------------------------- |
| Signaling complexity     | WebRTC usually requires signaling/session setup before media playback can begin.                                |
| Harder browser behavior  | Connection state, permissions, autoplay behavior, and browser negotiation can make tests more complex.          |
| More failure modes       | ICE negotiation, peer connection state, reconnect behavior, and network changes introduce additional variables. |
| More difficult debugging | WebRTC failures are usually harder to inspect than HTTP playlist or segment issues.                             |
| Baseline needed first    | The project should first prove the stream validation and artifact strategy with a simpler protocol.             |

WebRTC should be added after the project already has a working HLS baseline with reliable tests, screenshots, decoded markers, offset measurements, and reports.

### Planned protocol evolution

```text
Phase 1: HLS baseline
Phase 2: HLS synchronization validation
Phase 3: Docker network degradation testing
Phase 4: WebRTC extension
Phase 5: HLS vs WebRTC comparison
```

---

## Initial Streaming Flow

The initial streaming flow is:

```text
FFmpeg → MediaMTX → HLS → Browser Display Clients
```

### Component responsibilities

| Component               | Responsibility                                                                                       |
| ----------------------- | ---------------------------------------------------------------------------------------------------- |
| FFmpeg                  | Generates and publishes the synthetic test stream.                                                   |
| MediaMTX                | Receives the stream and exposes it through HLS.                                                      |
| HLS endpoint            | Provides the stream playlist and media segments over HTTP.                                           |
| Browser display clients | Load the HLS stream and render it as virtual displays.                                               |
| Test framework          | Opens the display clients, captures rendered output, decodes markers, and validates synchronization. |

---

## FFmpeg Video Generation Strategy

FFmpeg will be used to create the synthetic video stream.

The first version should use FFmpeg’s built-in generated video source. This allows the project to quickly validate the streaming pipeline without requiring a pre-recorded video file.

The initial generated video should include:

* a predictable test pattern;
* a source identifier;
* a frame counter;
* a timestamp;
* readable text overlays for manual debugging.

This first version is useful for proving that the full stream path works:

```text
FFmpeg generated video
        ↓
MediaMTX
        ↓
HLS endpoint
        ↓
Browser display clients
        ↓
Test framework capture
        ↓
Artifacts and report
```

---

## Initial FFmpeg Command

The first implementation can generate a live test video using FFmpeg’s `testsrc` filter and publish it to MediaMTX over RTSP.

Example command:

```bash
ffmpeg -re \
  -f lavfi -i "testsrc=size=1280x720:rate=30" \
  -vf "drawtext=text='SOURCE\: CAMERA_SYNC_TEST  FRAME\: %{n}  TIME\: %{pts\:hms}':x=40:y=40:fontsize=36:fontcolor=white:box=1:boxcolor=black@0.6" \
  -c:v libx264 \
  -preset veryfast \
  -tune zerolatency \
  -f rtsp \
  rtsp://localhost:8554/camera_sync_test
```

This command:

* generates a synthetic 1280x720 video;
* runs at 30 frames per second;
* overlays the source name, frame number, and timestamp;
* encodes the stream as H.264;
* publishes the stream to MediaMTX using RTSP;
* allows MediaMTX to expose the same stream as HLS.

The browser clients will then load the HLS endpoint:

```text
http://localhost:8888/camera_sync_test/index.m3u8
```

---

## MVP Video Marker Strategy

For the MVP, the generated FFmpeg video can use visible text overlays.

Example visible overlay:

```text
SOURCE: CAMERA_SYNC_TEST
FRAME: 000184
TIME: 00:00:06.133
```

This is enough to support early manual validation and basic screenshot review.

However, visible text should not be the final automation oracle. Text extraction through OCR can be unreliable because of:

* compression artifacts;
* browser scaling;
* font rendering;
* anti-aliasing;
* screenshot quality;
* resolution changes;
* timing differences between clients.

Therefore, the MVP overlay is only a temporary step to validate the streaming path.

---

## Target Video Marker Strategy

After the HLS pipeline is working, the synthetic video generator should be upgraded to include a machine-readable marker, preferably a QR code.

The target frame should include both:

* a QR code or equivalent machine-readable marker;
* human-readable text for debugging.

Example marker payload:

```json
{
  "source": "CAMERA_SYNC_TEST",
  "testRunId": "SYNC_RUN_001",
  "frame": 184,
  "timestamp": "2026-06-04T21:30:12.250Z"
}
```

Example human-readable text:

```text
SOURCE: CAMERA_SYNC_TEST
TEST_RUN_ID: SYNC_RUN_001
FRAME: 000184
TIMESTAMP: 2026-06-04T21:30:12.250Z
```

The machine-readable marker should become the primary validation source. Human-readable text should remain in the video because it makes screenshots and reports easier to understand.

---

## Future Generated Frame Approach

The first FFmpeg command can generate a simple live test pattern directly.

The later version may generate individual image frames first, then use FFmpeg to publish those frames as a stream.

Example future flow:

```text
Frame generation script
        ↓
PNG frames with QR code and readable text
        ↓
FFmpeg image sequence input
        ↓
MediaMTX
        ↓
HLS
        ↓
Browser clients
```

Example future command:

```bash
ffmpeg -re \
  -framerate 30 \
  -i generated-frames/frame_%06d.png \
  -c:v libx264 \
  -preset veryfast \
  -tune zerolatency \
  -f rtsp \
  rtsp://localhost:8554/camera_sync_test
```

In this approach, a script generates frames such as:

```text
generated-frames/
  frame_000001.png
  frame_000002.png
  frame_000003.png
```

Each frame contains a QR code with structured metadata and visible text for debugging.

This approach is more suitable for deterministic automation because each frame can contain an exact encoded frame number and timestamp.

---

## FFmpeg Role Boundary

FFmpeg is responsible for stream generation and publishing.

FFmpeg should not be responsible for test assertions.

FFmpeg should:

* generate the synthetic video source;
* include visual and machine-readable metadata;
* publish the stream to MediaMTX;
* run locally or inside Docker;
* write logs that can be collected as artifacts.

FFmpeg should not:

* decide whether a test passed or failed;
* compare display synchronization;
* inspect browser output;
* generate the final test report;
* replace the test framework.

The test framework remains responsible for validation.

---

## MediaMTX Media Server

MediaMTX is responsible for receiving the FFmpeg-published stream and exposing it to clients.

For the initial implementation, MediaMTX should expose the stream through HLS.

Example HLS endpoint:

```text
http://localhost:8888/camera_sync_test/index.m3u8
```

The media server gives the project a real network-based streaming path instead of simply playing a local video file in each browser.

This is important because the project is intended to validate streaming behavior over a network path, even when all components run on the same development machine.

---

## Browser Display Clients

Each display client represents one virtual screen.

Example display clients:

```text
DISPLAY_01
DISPLAY_02
DISPLAY_03
```

The first browser display client can be a simple HTML page using `hls.js`.

Example conceptual display URL:

```text
http://localhost:3000/display.html?displayId=DISPLAY_01&source=camera_sync_test
```

Each display client should:

* load the configured stream URL;
* render the video in the browser;
* expose a stable visual surface for screenshot or frame capture;
* include enough display identity information to support debugging;
* behave like a network-connected display endpoint.

The display client should not contain the synchronization logic. Synchronization validation belongs in the test framework.

---

## Docker Network Simulation

The project should run the streaming infrastructure inside a Docker network.

The purpose of Docker networking is to avoid testing only local OS playback. Even in a local development environment, the stream should pass through a networked path between services.

Initial Docker network:

```text
stream-sync-net
```

Expected Docker-managed services:

| Service                  | Runs in Docker? | Purpose                                                      |
| ------------------------ | --------------: | ------------------------------------------------------------ |
| MediaMTX                 |             Yes | Receives and serves the stream.                              |
| Stream generator         |         Planned | Runs FFmpeg and publishes the synthetic stream.              |
| Display client app       |         Planned | Serves the browser display page.                             |
| Network simulation tools |          Future | Adds latency, jitter, packet loss, and disconnect scenarios. |

The Java test framework may run on the host machine first. This simplifies local browser automation while still testing a network-based stream path.

Later, the test framework can also run in Docker or in GitHub Actions.

If the test framework runs in Docker, the artifacts directory should be mounted to the host:

```yaml
volumes:
  - ./artifacts:/app/artifacts
```

This keeps reports and failure evidence available after containers stop.

---

## Stream URL Strategy

Stream URLs should be configuration-driven, not hardcoded in test methods.

The test framework, display clients, and future protocol adapters should read stream configuration from a shared config file.

Example configuration:

```yaml
stream:
  protocol: HLS
  sourceId: camera_sync_test
  hlsUrl: http://localhost:8888/camera_sync_test/index.m3u8
  fps: 30
```

### URL responsibilities

| Area             | Responsibility                                                                 |
| ---------------- | ------------------------------------------------------------------------------ |
| Shared config    | Defines the active protocol and stream URL.                                    |
| Display client   | Loads the stream URL passed by configuration or query parameter.               |
| Test framework   | Uses the configured URL to open clients and validate expected source behavior. |
| Protocol adapter | Allows future HLS/WebRTC switching without rewriting test logic.               |

### Recommended display URL pattern

The browser display page should receive a display ID and source ID.

Example:

```text
http://localhost:3000/display.html?displayId=DISPLAY_01&source=camera_sync_test
```

The display client can then resolve the actual stream URL from configuration or from an API endpoint.

For the first version, passing the HLS URL directly may be acceptable for simplicity:

```text
http://localhost:3000/display.html?displayId=DISPLAY_01&streamUrl=http://localhost:8888/camera_sync_test/index.m3u8
```

However, the preferred long-term strategy is to avoid duplicating raw stream URLs across tests. A source-based strategy is cleaner:

```text
source=camera_sync_test
```

Then the client or backend resolves:

```text
camera_sync_test → http://localhost:8888/camera_sync_test/index.m3u8
```

This makes it easier to switch protocols later.

---

## Protocol Adapter Strategy

The test framework should avoid embedding protocol-specific logic directly into test assertions.

Instead, the framework should use a protocol adapter concept.

Example:

```text
ProtocolAdapter
  ├── HlsProtocolAdapter
  └── WebRtcProtocolAdapter
```

The adapter should handle protocol-specific setup such as:

* stream URL resolution;
* playback readiness checks;
* protocol-specific client initialization;
* future WebRTC connection state handling.

The core test should remain focused on behavior:

```text
Given the stream is available
When three display clients render the stream
Then all displays should show the expected source
And frame offset should remain within tolerance
```

This keeps the test strategy reusable when WebRTC is added later.

---

## Future Network Degradation Testing

Network degradation testing should be added after the HLS baseline is stable.

The initial project should first prove that the happy path works:

* MediaMTX is available;
* FFmpeg publishes the stream;
* display clients load the stream;
* frames progress;
* markers are decoded;
* synchronization offset is calculated;
* artifacts and reports are generated.

After that baseline is reliable, Docker-based network impairment can be introduced.

### Future degradation scenarios

| Scenario                 | Purpose                                                                         |
| ------------------------ | ------------------------------------------------------------------------------- |
| Added latency            | Validate whether displays remain within tolerance when network delay increases. |
| Jitter                   | Validate behavior when delay varies over time.                                  |
| Packet loss              | Validate whether playback continues or stalls under lossy conditions.           |
| Display disconnect       | Validate recovery when one display temporarily loses access to the stream.      |
| Media server restart     | Validate client and test behavior when the stream server restarts.              |
| Stream generator restart | Validate stale-frame and recovery detection.                                    |
| Bandwidth limitation     | Validate behavior under constrained network throughput.                         |

### Possible tooling

A future implementation may use tools such as:

* Toxiproxy;
* Linux `tc/netem`;
* Docker network controls;
* a custom network controller container.

The test framework should expose these as controlled test modes, not manual steps.

Example future configuration:

```yaml
network:
  mode: docker
  dockerNetwork: stream-sync-net
  impairmentEnabled: true
  impairmentProfile: latency_200ms_jitter_50ms
```

Example future impairment profiles:

```yaml
impairmentProfiles:
  latency_200ms:
    latencyMs: 200

  latency_200ms_jitter_50ms:
    latencyMs: 200
    jitterMs: 50

  packet_loss_2_percent:
    packetLossPercent: 2

  display_02_disconnect:
    targetDisplay: DISPLAY_02
    disconnectAfterSeconds: 20
    reconnectAfterSeconds: 10
```

---

## Testing Implications

The streaming plan directly supports the automation strategy.

The tests should validate behavior through captured display output, not by assuming that playback succeeded because services started successfully.

The test framework should verify:

* the stream endpoint is reachable;
* each browser display loaded the expected stream;
* each display shows the expected source ID;
* each display shows the current test run ID;
* frame numbers progress over time;
* frame offsets across displays stay within tolerance;
* frozen or stale displays are detected;
* artifacts are saved for review.

The main synchronization calculation should compare decoded frame numbers from each display.

Example:

```text
DISPLAY_01 frame = 1050
DISPLAY_02 frame = 1049
DISPLAY_03 frame = 1051
```

Then:

```text
max frame offset = max(frame) - min(frame)
max frame offset = 1051 - 1049 = 2 frames
```

If the tolerance is 3 frames:

```text
Expected tolerance: <= 3 frames
Actual offset: 2 frames
Result: PASS
```

---

## Artifact and Report Expectations

Streaming tests should produce persistent artifacts.

The project should not rely only on console logs because console output is not enough to explain streaming behavior after the run finishes.

Each run should store raw artifacts under:

```text
artifacts/runs/{testRunId}/
```

The latest demo report should be available under:

```text
artifacts/latest/index.html
```

Streaming-related artifacts should include:

* screenshots or captured frames from each display;
* decoded marker samples;
* frame offset measurements;
* stream protocol;
* stream URL or source ID;
* network mode;
* MediaMTX logs when available;
* FFmpeg logs when available;
* failure summary when applicable;
* stack trace when applicable.

This supports both local demo review and future GitHub Actions artifact upload.

---

## Initial Implementation Recommendation

The first implementation should use this path:

```text
FFmpeg synthetic testsrc stream
        ↓
MediaMTX media server
        ↓
HLS endpoint
        ↓
Browser display clients using hls.js
        ↓
Java test framework using Playwright
        ↓
Screenshot/frame capture
        ↓
Frame marker validation
        ↓
Frame offset validation
        ↓
Artifacts and latest demo report
```

The initial FFmpeg command should use `testsrc` and `drawtext` overlays to validate the streaming path quickly.

After the pipeline is working, the generator should be upgraded to produce QR-code-based frames so the automation can decode structured frame metadata instead of relying on OCR.

---

## Future Direction

After the HLS implementation is stable, the project can expand in this order:

1. Add QR-code-based synthetic frame generation.
2. Add controlled network degradation scenarios.
3. Compare display synchronization under different impairment profiles.
4. Add WebRTC as a second protocol.
5. Compare HLS and WebRTC behavior using the same marker and artifact model.
6. Add Python/OpenCV-based frame analysis if Java marker decoding becomes limiting.
7. Add GitHub Actions artifact upload.
8. Add GitHub Pages report publishing for portfolio demos.
9. Consider capture-card or physical-display validation as a separate future architecture.

---

## Final Decision

The project will start with HLS because it is practical, browser-friendly, easier to debug, and sufficient for proving the core automation strategy.

WebRTC will be added later because it is better suited for low-latency streaming but introduces additional complexity that should not block the first working demo.

The approved first streaming path is:

```text
FFmpeg → MediaMTX → HLS → Browser Display Clients
```

The approved first video generation approach is:

```text
FFmpeg testsrc + drawtext overlays
```

The target validation approach is:

```text
Generated frames with QR-code metadata + human-readable text
```

The streaming design must remain:

* configuration-driven;
* Docker-network aware;
* artifact-friendly;
* protocol-replaceable;
* compatible with Java-first automation;
* ready for QR-code-based marker validation;
* ready for future network degradation testing;
* ready for future WebRTC support.
