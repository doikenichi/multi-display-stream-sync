# Evidence Contract

Streaming Lab separates structured evidence from logs.

## Structured evidence

Structured evidence is schema-validated JSON.

Required first-milestone files:

```text
playback-status.json
run-summary.json
```

## Logs

Logs are not schema-validated, but they must be correlation-friendly and include the `testRunId`.

Required first-milestone log:

```text
playback.log
```

## First-milestone evidence focus

The first milestone proves single-display HLS playback progression.

Multi-display synchronization evidence, such as frame/PTS comparison across displays, is deferred until the playback contract is stable.
