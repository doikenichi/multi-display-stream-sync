# Display Client Specification

## Purpose

This document defines the specification for the **Display Client** component in the Multi-Display Stream Synchronization Test Lab.

The Display Client is a browser-based virtual display used to render a video stream during automated synchronization testing.

It is intentionally separate from:

- the **Streaming Lab Orchestration API**, which controls stream lifecycle and infrastructure;
- the **Test Framework**, which opens display clients, captures rendered output, decodes markers, validates synchronization, and produces evidence.

The Display Client should behave like a product-facing browser endpoint: it receives a stream URL, renders the video, exposes display identity, and provides stable browser elements for automation.

---

## Audience

This document is intended for:

- frontend engineers;
- automation engineers;
- QA engineers;
- software architects;
- senior technical stakeholders reviewing system boundaries and implementation quality.

---

## Component Boundary

### The Display Client Is Responsible For

The Display Client is responsible for:

- rendering the HLS stream in a browser;
- accepting a display identity such as `DISPLAY_01`, `DISPLAY_02`, or `DISPLAY_03`;
- accepting the stream URL through query parameters or configuration;
- showing display identity and playback state;
- exposing stable DOM selectors for Playwright automation;
- supporting local development;
- supporting Docker-based execution;
- participating in Docker Compose with MediaMTX, the Streaming Lab Orchestration API, and the Java test runner.

### The Display Client Is Not Responsible For

The Display Client must not:

- start or stop FFmpeg;
- control MediaMTX;
- create test runs;
- generate synthetic frames;
- decode QR markers;
- calculate frame offsets;
- calculate latency;
- detect frozen displays;
- decide pass or fail;
- generate evidence artifacts;
- replace the Java test framework.

The clean responsibility model is:

```text
Streaming Lab Orchestration API
  Controls stream lifecycle and exposes the HLS URL

Display Client
  Renders the stream as a browser-based virtual display

Test Framework
  Opens display clients, captures output, validates behavior, and writes evidence
```

---

## Technology Requirements

The Display Client will be implemented as a cloud-native frontend application using:

- React;
- TypeScript;
- Vite;
- hls.js;
- Docker;
- Nginx or another lightweight static server for production container execution.

This stack provides a realistic frontend implementation while keeping the component small, testable, and easy to containerize.

---

## High-Level Runtime Flow

```text
Test Framework
  ↓ opens browser URL
Display Client
  ↓ loads HLS URL
MediaMTX
  ↓ serves HLS stream
Browser Video Element
  ↓ renders stream
Test Framework
  ↓ captures screenshot or rendered frame
Marker Decoder and Sync Analyzer
  ↓ validates result
Evidence Artifacts
```

The Display Client does not validate the video. It only renders it in a stable and observable way.

---

## URL Contract

The Display Client should support query-parameter based configuration.

### Required Query Parameters

| Parameter | Required | Description |
|---|---:|---|
| `displayId` | Yes | Logical display identity, such as `DISPLAY_01`. |
| `streamUrl` | Yes | HLS playback URL served by MediaMTX. |

### Optional Query Parameters

| Parameter | Required | Description |
|---|---:|---|
| `sourceId` | No | Expected source identity, such as `CAMERA_SYNC_TEST`. Used for display/debug purposes only. |
| `testRunId` | No | Active test run ID. Used for display/debug purposes only. |
| `autoplay` | No | Enables or disables automatic playback. Default should be `true`. |
| `muted` | No | Enables muted playback. Default should be `true` to support browser autoplay. |
| `debug` | No | Shows additional diagnostic information. Default should be `false`. |

### Example Local URL

```text
http://localhost:3000/display?displayId=DISPLAY_01&streamUrl=http://localhost:8888/camera_sync_test/
```

### Example Docker Compose Internal URL

When the Java test runner runs inside Docker Compose, it should use service names:

```text
http://display-client:3000/display?displayId=DISPLAY_01&streamUrl=http://mediamtx:8888/camera_sync_test/
```

### Important Localhost Rule

Inside Docker containers, `localhost` refers to the current container, not the host machine.

Therefore:

- host browser URLs can use `localhost`;
- container-to-container URLs should use Docker Compose service names.

---

## Functional Requirements

### 1. Display Identity

The Display Client must show the current display ID.

Example:

```text
DISPLAY_01
```

The display ID must also be exposed through a stable selector:

```html
<div data-testid="display-id">DISPLAY_01</div>
```

### 2. Stream URL Handling

The Display Client must read the HLS stream URL from the `streamUrl` query parameter.

The stream URL should be displayed in debug mode and exposed through a stable selector:

```html
<div data-testid="stream-url">http://localhost:8888/camera_sync_test/</div>
```

If the stream URL is missing, the Display Client must show an error state instead of silently failing.

### 3. HLS Playback

The Display Client must use `hls.js` for HLS playback in browsers that do not support HLS natively.

Expected behavior:

1. locate the video element;
2. initialize `hls.js`;
3. load the HLS stream URL;
4. attach the stream to the video element;
5. attempt playback;
6. update playback state;
7. expose errors when playback fails.

The browser video element should be stable for automation:

```html
<video data-testid="video-player" autoplay muted playsinline controls></video>
```

### 4. Native HLS Fallback

If the browser supports native HLS playback, the Display Client may fall back to the native video element behavior.

Expected fallback:

```text
if hls.js is supported:
  use hls.js
else if browser supports application/vnd.apple.mpegurl:
  use native HLS
else:
  show unsupported state
```

### 5. Playback State

The Display Client must expose playback state using stable text values.

Recommended states:

| State | Meaning |
|---|---|
| `INITIALIZING` | Page loaded and player setup is starting. |
| `LOADING_STREAM` | HLS stream is being loaded. |
| `MANIFEST_LOADED` | HLS manifest has been parsed. |
| `PLAYING` | Browser playback has started. |
| `BUFFERING` | Playback is waiting for data. |
| `PAUSED` | Video is paused. |
| `ERROR` | Playback or stream loading failed. |
| `UNSUPPORTED` | Browser cannot play HLS through hls.js or native support. |

Stable selector:

```html
<div data-testid="playback-status">PLAYING</div>
```

### 6. Playback Errors

Playback errors must be visible and automation-readable.

Stable selector:

```html
<div data-testid="playback-error"></div>
```

If an error occurs, the Display Client should show:

- error type;
- error details where available;
- whether the error is fatal;
- current stream URL;
- current display ID.

### 7. Debug Panel

The Display Client should provide a lightweight debug panel.

Recommended fields:

- display ID;
- stream URL;
- source ID;
- test run ID;
- playback status;
- browser HLS mode: `hls.js`, `native`, or `unsupported`;
- last playback error;
- video ready state;
- video current time;
- video dimensions.

The debug panel supports manual troubleshooting and automated evidence screenshots.

### 8. Automation-Friendly DOM

The Display Client must expose stable selectors for Playwright.

Required selectors:

| Selector | Purpose |
|---|---|
| `[data-testid="display-id"]` | Display identity. |
| `[data-testid="video-player"]` | Video element for playback and screenshot targeting. |
| `[data-testid="playback-status"]` | Current playback status. |
| `[data-testid="stream-url"]` | Current stream URL. |
| `[data-testid="playback-error"]` | Current playback error. |
| `[data-testid="debug-panel"]` | Diagnostic information. |

Selectors must not be tied to CSS styling or layout-only classes.

---

## Non-Functional Requirements

### 1. Cloud-Native Execution

The Display Client must run as a containerized service.

It should support:

- local development through Vite;
- production build through `npm run build`;
- production serving through Nginx or a lightweight static server;
- Docker Compose execution;
- future CI execution.

### 2. Lightweight Runtime

The production container should serve static files only.

The Display Client should not require a Node.js runtime in the final production container unless there is a specific reason.

Recommended production model:

```text
Vite build
  ↓
static files
  ↓
nginx container
```

### 3. Testability

The Display Client should be easy to test through Playwright.

It must provide:

- deterministic URL parameters;
- stable DOM selectors;
- visible playback state;
- visible error state;
- no hidden validation logic.

### 4. Observability

The Display Client should make browser-side playback status visible.

It should not hide playback errors in the JavaScript console only.

Errors should be reflected in the UI and available through stable DOM elements.

### 5. Configuration Simplicity

The initial version should prefer query parameters over complex configuration.

This keeps the early implementation simple and allows the Java test framework to control each virtual display URL independently.

---

## Recommended Project Structure

```text
display-client/
  Dockerfile
  nginx.conf
  package.json
  package-lock.json
  tsconfig.json
  vite.config.ts
  index.html
  src/
    main.tsx
    App.tsx
    components/
      DisplayPage.tsx
      VideoPlayer.tsx
      PlaybackStatus.tsx
      DebugPanel.tsx
    hooks/
      useQueryParams.ts
      useHlsPlayer.ts
    types/
      display.ts
    styles/
      app.css
```

---

## React Component Design

### App

`App` should define the application root and route to the display page.

Initial implementation may use a single page only.

### DisplayPage

`DisplayPage` should:

- read query parameters;
- validate required parameters;
- pass stream configuration to the video player;
- render display identity;
- render playback status;
- render debug panel.

### VideoPlayer

`VideoPlayer` should:

- own the video element;
- initialize hls.js;
- attach the stream to the video element;
- update playback state;
- report playback errors.

### PlaybackStatus

`PlaybackStatus` should render the current playback state and expose it through `data-testid`.

### DebugPanel

`DebugPanel` should render useful runtime information for manual review and Playwright screenshots.

---

## TypeScript Model

Recommended playback state type:

```ts
export type PlaybackStatus =
  | "INITIALIZING"
  | "LOADING_STREAM"
  | "MANIFEST_LOADED"
  | "PLAYING"
  | "BUFFERING"
  | "PAUSED"
  | "ERROR"
  | "UNSUPPORTED";
```

Recommended display configuration type:

```ts
export interface DisplayConfig {
  displayId: string;
  streamUrl: string;
  sourceId?: string;
  testRunId?: string;
  autoplay: boolean;
  muted: boolean;
  debug: boolean;
}
```

Recommended playback error type:

```ts
export interface PlaybackError {
  type?: string;
  details?: string;
  fatal?: boolean;
  message: string;
}
```

---

## hls.js Integration Requirements

The hls.js integration should be isolated in a hook or utility.

Recommended hook:

```text
useHlsPlayer
```

Responsibilities:

- receive a video element reference;
- receive a stream URL;
- initialize hls.js when supported;
- attach media;
- listen for manifest and playback events;
- listen for errors;
- clean up the hls.js instance on unmount;
- return playback status and error information.

The hook should not include test validation logic.

---

## Docker Requirements

### Dockerfile

Recommended Dockerfile:

```dockerfile
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
```

### Nginx Configuration

Recommended `nginx.conf`:

```nginx
server {
    listen 3000;

    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri /index.html;
    }
}
```

### Docker Compose Service

Recommended service:

```yaml
display-client:
  build:
    context: ./display-client
    dockerfile: Dockerfile
  ports:
    - "3000:3000"
  depends_on:
    - mediamtx
```

---

## Local Development Commands

Create the React application:

```bash
npm create vite@latest display-client -- --template react-ts
```

Install dependencies:

```bash
cd display-client
npm install
npm install hls.js
```

Run locally:

```bash
npm run dev -- --host 0.0.0.0 --port 3000
```

Build production assets:

```bash
npm run build
```

Preview production build:

```bash
npm run preview -- --host 0.0.0.0 --port 3000
```

---

## Example Display URLs

### Host Browser

```text
http://localhost:3000/display?displayId=DISPLAY_01&streamUrl=http://localhost:8888/camera_sync_test/
```

### Docker Compose Internal Access

```text
http://display-client:3000/display?displayId=DISPLAY_01&streamUrl=http://mediamtx:8888/camera_sync_test/
```

### Multiple Displays

```text
http://localhost:3000/display?displayId=DISPLAY_01&streamUrl=http://localhost:8888/camera_sync_test/
http://localhost:3000/display?displayId=DISPLAY_02&streamUrl=http://localhost:8888/camera_sync_test/
http://localhost:3000/display?displayId=DISPLAY_03&streamUrl=http://localhost:8888/camera_sync_test/
```

---

## Playwright Integration Expectations

The Java test framework should be able to:

1. open the Display Client URL;
2. wait for `[data-testid="playback-status"]` to become `PLAYING`;
3. capture a screenshot of the page or video element;
4. save the screenshot under the run artifact directory;
5. later decode the QR marker from the captured output;
6. record decoded marker samples and synchronization evidence.

Example Playwright-oriented readiness rule:

```text
The display is considered ready when:
  [data-testid="display-id"] matches the requested display ID
  [data-testid="playback-status"] is PLAYING
  [data-testid="video-player"] is visible
```

Readiness does not mean the stream is correct. Correctness is determined later through marker decoding by the test framework.

---

## Error Handling Requirements

The Display Client must handle the following cases:

| Scenario | Expected Behavior |
|---|---|
| Missing `displayId` | Show `ERROR` state and clear message. |
| Missing `streamUrl` | Show `ERROR` state and clear message. |
| Invalid stream URL | Show `ERROR` state and clear message. |
| HLS manifest unavailable | Show `ERROR` state and hls.js error details. |
| Browser does not support HLS | Show `UNSUPPORTED` state. |
| Playback blocked | Show `ERROR` or `PAUSED` with useful message. |
| Stream buffering | Show `BUFFERING`. |

Errors should be visible on the page because browser console logs alone are not enough for evidence review.

---

## Security and Safety Considerations

The Display Client will accept a stream URL as a query parameter.

For the local proof of concept this is acceptable, but the application should avoid using the stream URL as raw HTML.

The stream URL should be treated as text and passed only to the video/HLS loading logic.

The Display Client should not evaluate script content from query parameters.

---

## Accessibility and Usability Requirements

The Display Client should remain simple but readable.

Recommended UI elements:

- clear display title;
- visible playback status;
- visible stream URL in debug mode;
- video controls enabled for manual debugging;
- high-contrast background;
- readable error messages.

The UI is not intended to be a polished product interface. It is a test lab display surface.

---

## Version 1 Scope

The first implementation should include:

- React + TypeScript + Vite setup;
- hls.js playback;
- query parameter handling;
- display identity;
- playback status;
- error display;
- stable `data-testid` selectors;
- Dockerfile;
- Nginx config;
- Docker Compose service entry.

Version 1 should not include:

- QR decoding;
- synchronization calculations;
- artifact writing;
- backend API calls;
- authentication;
- advanced routing;
- visual dashboards.

---

## Future Enhancements

Potential future additions:

- WebRTC playback mode;
- protocol selector: HLS or WebRTC;
- visual network status indicator;
- connection retry policy;
- display layout profiles;
- full-screen/kiosk mode;
- health endpoint for container checks;
- optional call to orchestration API for stream metadata;
- browser performance metrics;
- support for multiple stream sources.

---

## Acceptance Criteria

The Display Client is considered complete for the first milestone when:

1. it can run locally with Vite;
2. it can run as a Docker container;
3. it can load the MediaMTX HLS URL;
4. it can render the stream in Chrome or Edge;
5. it shows the requested `displayId`;
6. it exposes stable Playwright selectors;
7. it shows playback status;
8. it shows useful error messages;
9. it can be opened three times with different display IDs;
10. it does not contain validation or pass/fail logic.

---

## Summary

The Display Client is a thin, cloud-native React frontend that renders an HLS video stream as a browser-based virtual display.

It is important because it gives the test framework a realistic browser surface to automate and capture.

The Display Client should remain simple, observable, and automation-friendly.

The final validation authority remains the Java test framework, not the React application.
