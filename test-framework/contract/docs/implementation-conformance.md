# Implementation Conformance Contract

Every implementation must satisfy the same observable behavior.

| Requirement | JVM | Node/TypeScript | Python |
|---|---:|---:|---:|
| Uses shared `.feature` files | Required | Required | Required |
| Loads YAML config by profile | Required | Required | Required |
| Calls `streaming-lab-orchestration-api` | Required | Required | Required |
| Consumes orchestrator-generated `testRunId` | Required | Required | Required |
| Prepares test run | Required | Required | Required |
| Starts stream | Required | Required | Required |
| Opens Display Client | Required | Required | Required |
| Verifies video loaded | Required | Required | Required |
| Verifies playback progresses | Required | Required | Required |
| Captures screenshot | Required | Required | Required |
| Writes `playback-status.json` | Required | Required | Required |
| Writes `run-summary.json` | Required | Required | Required |
| Writes `playback.log` | Required | Required | Required |
| Validates schemas | Required | Required | Required |
| Exits non-zero on failure | Required | Required | Required |

## First implementation target

The first implementation should be JVM-based because the orchestrator module is already Java/Spring Boot and the repo already uses Gradle-based smoke testing.
