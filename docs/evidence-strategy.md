# Evidence Strategy

Streaming Lab stores test-run evidence under:

`artifacts/test-runs/{testRunId}/`

Initial playback smoke evidence includes:

- `playback-status.json` — machine-readable playback diagnostics
- `screenshot.png` — human-readable proof of playback state

Generated evidence files are ignored by Git. The folder convention is committed using `.gitkeep`.

The Display Client exposes diagnostics, but it does not write evidence files. Evidence capture is the responsibility of the test/evidence layer.