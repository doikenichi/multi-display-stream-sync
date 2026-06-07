# Display Client

The Display Client is a React, TypeScript, and Vite browser client that renders an HLS stream as a virtual display endpoint. It is used by automation to verify display identity, video visibility, playback state, and playback errors.

## How to Run Display Client Locally

Install dependencies and start the Vite development server:

```powershell
npm ci
npm run dev
```

From the repository root, the same commands are:

```powershell
cd display-client
npm ci
npm run dev
```

Useful validation commands:

```powershell
npm run lint
npm run build
npm run preview
```

## How to Run It with Docker Compose

From the repository root:

```powershell
docker compose up --build
```

This starts:

* `mediamtx` on RTSP port `8554` and HLS port `8888`
* `display-client` on HTTP port `3000`

After Compose is running, publish the synthetic stream to MediaMTX:

```powershell
ffmpeg -re -f lavfi -i "testsrc=size=1280x720:rate=30" -vf "drawtext=fontfile='C\:/Windows/Fonts/arial.ttf':text='SOURCE\: CAMERA_SYNC_TEST FRAME\: %{n} TIME\: %{pts\:hms}':x=40:y=40:fontsize=36:fontcolor=white:box=1:boxcolor=black@0.6" -c:v libx264 -preset veryfast -tune zerolatency -pix_fmt yuv420p -rtsp_transport tcp -f rtsp rtsp://localhost:8554/camera_sync_test
```

## What URL to Open

Open this URL for a local or Docker Compose run:

```text
http://localhost:3000/display?displayId=DISPLAY_01&streamUrl=http%3A%2F%2Flocalhost%3A8888%2Fcamera_sync_test%2Findex.m3u8&debug=true
```

Required query parameters:

| Parameter | Purpose |
| --- | --- |
| `displayId` | Display identity shown in the UI and checked by automation |
| `streamUrl` | URL-encoded HLS stream URL |

Optional query parameters:

| Parameter | Default | Purpose |
| --- | --- | --- |
| `sourceId` | unset | Source identifier for test context |
| `testRunId` | unset | Test run identifier for automation context |
| `autoplay` | `true` | Enables video autoplay |
| `muted` | `true` | Mutes the video so autoplay can work in browsers |
| `debug` | `false` | Shows diagnostics used during development |

## What Readiness Means

The Display Client is ready for automation when all of these conditions are true:

* `[data-testid="display-id"]` equals the expected display ID.
* `[data-testid="video-player"]` is visible.
* `[data-testid="playback-status"]` equals `PLAYING`.
* `[data-testid="playback-error"]` is empty and has no active error text.

Readiness means the page has accepted its configuration, created the video element, loaded the stream, and reached active playback.

## What Selectors Are Used by Automation

| Selector | Expected value or state |
| --- | --- |
| `[data-testid="display-id"]` | Text equals the expected `displayId` |
| `[data-testid="video-player"]` | Element is visible |
| `[data-testid="playback-status"]` | Text equals `PLAYING` when playback is ready |
| `[data-testid="playback-error"]` | Empty when there is no active playback or configuration error |
