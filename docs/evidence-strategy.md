# Evidence Strategy

Streaming Lab stores test-run evidence under:

`artifacts/test-runs/{testRunId}/`

Initial playback smoke evidence includes:

- `playback-status.json` — machine-readable playback diagnostics
- `screenshot.png` — human-readable proof of playback state

Generated evidence files are ignored by Git. The folder convention is committed using `.gitkeep`.

The Display Client exposes diagnostics, but it does not write evidence files. Evidence capture is the responsibility of the test/evidence layer.

## Playback Status Template

A sample playback evidence contract is stored at:

`docs/templates/playback-status.example.json`

This file documents the expected structure for future automated playback evidence.

## Playback Status Schema

The playback evidence contract is documented with:

- `docs/templates/playback-status.example.json`
- `docs/templates/playback-status.schema.json`

Future automated playback smoke tests should generate `playback-status.json` files that conform to this schema.


## Sync Report Template

A sample synchronization evidence contract is stored at:

`docs/templates/sync-report.example.json`

This report compares multiple display captures using observed frame markers, PTS values, screenshots, and playback status evidence.

## Evidence Schema Validation

Evidence templates are validated with:

`npm run validate:evidence-schemas`

This validates:

- `playback-status.example.json` against `playback-status.schema.json`
- `sync-report.example.json` against `sync-report.schema.json`

The validation script is written in TypeScript and uses AJV with JSON Schema draft 2020-12 support.