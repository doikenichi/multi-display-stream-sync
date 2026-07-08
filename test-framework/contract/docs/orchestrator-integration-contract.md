# Orchestrator Integration Contract

`streaming-lab-orchestration-api` is the test orchestrator service provider for Streaming Lab.

It owns:

- `testRunId` generation
- Test-run lifecycle state
- Valid and invalid lifecycle transitions
- Stream lifecycle actions
- API response contract

Framework implementations must not generate their own canonical run IDs or implement a parallel lifecycle state machine.

## Required endpoints

```text
POST /api/test-runs
POST /api/test-runs/{id}/prepare
POST /api/test-runs/{id}/stream
POST /api/test-runs/{id}/stop
POST /api/test-runs/{id}/fail
GET  /api/test-runs/{id}
```

## Required happy-path flow

```text
create -> prepare -> stream -> display playback -> stop -> verify stopped -> write evidence
```

## Scenario context fields

Each implementation must maintain equivalent scenario context:

```text
testRunId
currentTestRunStatus
streamId
hlsStreamUrl
displayClientUrl
artifactDirectory
startedAt
implementationName
profile
```

## Failure handling

If a framework implementation fails after receiving a `testRunId`, it should attempt to mark the run as `FAILED` or safely stop it through the orchestrator before exiting.
