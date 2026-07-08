# Streaming Lab Polyglot BDD Test Framework Architecture

_Last updated: July 1, 2026_

## 1. Overview of the solution

The Streaming Lab test framework is designed as a **polyglot BDD conformance test framework**.

The goal is not to create three unrelated test frameworks. The goal is to define **one shared testing contract** and prove that it can be implemented consistently in three technology stacks:

```text
One BDD-defined test suite
One shared test-framework contract
Three independent implementations:
  Java + JUnit Platform + Cucumber JVM
  Node.js + TypeScript + CucumberJS
  Python + pytest + pytest-bdd
One evidence/artifact standard
One dashboard vocabulary
One CI/CD execution model
```

The core architectural principle is:

```text
The BDD feature files are the source of truth.
Each language implements the same behavior.
Each implementation must satisfy the same config, evidence, artifact, and reporting contract.
```

This creates a strong portfolio story:

> Designed a language-agnostic BDD test-framework contract for a multi-display streaming system and implemented it independently in Java, TypeScript, and Python. Each implementation executes the same shared Gherkin specifications, validates the same streaming behavior, produces schema-compliant evidence correlated by `testRunId`, and publishes results to a centralized ReportPortal dashboard.

The project should stay focused on:

- BDD as readable executable test documentation.
- Git-based feature files as the test-case repository.
- Schema-validated evidence for playback and system validation.
- ReportPortal as the central dashboard for results, logs, screenshots, and trends.
- Docker Compose for local system confidence.
- Kubernetes later for portability and AWS migration readiness.
- Jenkins later as a DevOps learning track, not as a replacement for the test framework.

---

## 2. Problem statement

The Streaming Lab project validates a multi-display streaming system. The system includes:

```text
MediaMTX
  -> serves RTSP/HLS streams

FFmpeg generator
  -> generates synthetic video stream with visual timing/frame evidence

Display Client
  -> React + TypeScript + Vite application that plays HLS streams

Orchestration API
  -> Spring Boot API that manages test-run lifecycle

Test Framework
  -> drives the system, validates behavior, and produces evidence
```

The test framework should answer questions such as:

- Can the Orchestration API create and manage a test run?
- Can the stream be prepared and started correctly?
- Can the Display Client load and play the HLS stream?
- Does playback actually progress?
- Are screenshots, logs, and playback evidence generated?
- Are artifacts correlated by `testRunId`?
- Can results be consumed by dashboards and future automated analysis?
- Can the same test contract be implemented in Java, TypeScript, and Python?

---

## 3. Design goals

### 3.1 Functional goals

The framework must support:

```text
Create test run
Prepare test run
Start stream
Open Display Client
Verify playback progress
Capture screenshot
Write playback evidence
Stop test run
Validate final status
Publish results
```

### 3.2 Portfolio goals

The framework should demonstrate:

- Senior QA/SDET architecture thinking.
- BDD and executable specification design.
- Polyglot automation across Java, TypeScript, and Python.
- API, browser, and evidence-driven testing.
- Docker-based system testing.
- CI/CD readiness.
- Dashboard/test observability with ReportPortal.
- Future Kubernetes/AWS migration awareness.
- DevOps learning through Jenkins without confusing Jenkins with the test framework.

### 3.3 Engineering goals

The framework should be:

- Explicitly contract-driven.
- Repeatable locally and in CI.
- Evidence-first.
- Easy to explain in interviews.
- Incrementally buildable.
- Open-source friendly.
- Runnable in Docker.
- Portable to Kubernetes later.

---

## 4. Key architectural decision

The chosen architecture is:

```text
test-framework-contract/
  shared feature files
  shared schemas
  shared artifact rules
  shared config model
  shared ReportPortal metadata rules
  shared implementation conformance rules

test-framework-jvm/
  Java implementation
  JUnit Platform
  Cucumber JVM
  optional Playwright Java for browser validation

test-framework-node/
  Node.js + TypeScript implementation
  CucumberJS
  Playwright TypeScript for browser validation

test-framework-python/
  Python implementation
  pytest
  pytest-bdd
  optional Playwright Python for browser validation

reporting/
  ReportPortal integration

ci/
  GitHub Actions primary
  Jenkins later for DevOps learning
```

The architecture is not:

```text
Java tests for API only
TypeScript tests for browser only
Python tests for evidence only
```

That was considered but rejected based on the final requirement.

The final requirement is stronger:

```text
All three implementations must execute the same BDD-defined behavior and satisfy the same contract.
```

This turns the project into a **conformance framework**.

---

## 5. What BDD means in this project

BDD is used as the readable specification and test-case repository.

A traditional test-management tool may store:

```text
Test case title
Preconditions
Steps
Expected results
Execution status
Evidence
```

In this project, the equivalent is:

```text
Feature file
  Feature
  Rule
  Scenario
  Given / When / Then
  Examples
  Tags

Git repository
  Review history
  Pull requests
  Ownership
  Change tracking

ReportPortal
  Execution results
  Logs
  Screenshots
  Attachments
  Trends
```

Important clarification:

```text
BDD can replace the test-step repository aspect of tools like Jira/TestRail.
BDD should not be treated as a full replacement for backlog management, defect tracking, or release planning.
```

For lightweight project management, GitHub Issues can be used for:

- Bugs.
- Technical tasks.
- Test debt.
- Framework enhancements.
- Risk notes.

BDD feature files should be treated as version-controlled living documentation.

---

## 6. Shared BDD feature files

The shared feature files must live in one place only:

```text
test-framework-contract/
  features/
    test-run-lifecycle.feature
    hls-playback-validation.feature
    playback-evidence-contract.feature
```

Avoid this:

```text
test-framework-jvm/features/test-run-lifecycle.feature
test-framework-node/features/test-run-lifecycle.feature
test-framework-python/features/test-run-lifecycle.feature
```

That would create drift.

The rule is:

```text
No implementation may fork or rewrite the feature files to make its own code easier.
If a scenario changes, the shared contract changes first.
All implementations must adapt to the shared contract.
```

### Example feature

```gherkin
@contract @smoke @playback
Feature: HLS playback validation

  Rule: A streaming test run must produce playback evidence

  Scenario: Validate playback progression for a display client
    Given a new test run exists
    And the test run is prepared
    When the stream is started
    And the display client is opened for the stream
    Then the display client should load the video
    And playback time should progress
    And playback evidence should be written
    And a playback screenshot should be captured
    When the test run is stopped
    Then the test run should be stopped
    And all required artifacts should contain the test run id
```

---

## 7. Portable Gherkin subset

Because the same feature files must run in Java, TypeScript, and Python, the project should use a portable Gherkin subset.

Use initially:

```text
Feature
Rule
Background
Scenario
Scenario Outline
Examples
Given / When / Then / And
Tags
Simple parameters
```

Use carefully after compatibility is proven:

```text
Data tables
Doc strings
Custom parameter types
Complex hooks
```

Avoid at first:

```text
Runner-specific syntax
Language-specific step tricks
Complex custom transformers
Plugin-specific assumptions
Formatter-specific behavior
```

The reason is that `pytest-bdd` implements a subset of Gherkin, while Cucumber JVM and CucumberJS are official Cucumber implementations. The shared feature files should therefore target the most portable common behavior.

---

## 8. BDD step design rules

BDD step duplication is a real maintainability risk. The framework should intentionally prevent uncontrolled step growth.

Rules:

```text
Use domain language, not implementation language.
Keep steps stable across languages.
Reuse existing step phrases.
Do not create near-duplicate steps.
Keep technical details out of Gherkin.
Put automation details in support/client classes.
Review feature-file changes carefully.
```

Good:

```gherkin
Given a new test run exists
When the stream is started
Then playback time should progress
```

Bad:

```gherkin
Given I send a POST request to /api/test-runs
When I wait 5000 milliseconds for currentTime to be greater than zero
Then the JSON field status should equal STREAMING
```

The Gherkin should describe the business/test behavior. The implementation should decide how to call APIs, wait, retry, parse JSON, and collect evidence.

---

## 9. Shared test-framework contract

The contract defines what it means for Java, TypeScript, and Python to satisfy the same test framework.

Recommended structure:

```text
test-framework-contract/
  features/
    test-run-lifecycle.feature
    hls-playback-validation.feature
    playback-evidence-contract.feature

  schemas/
    playback-status.schema.json
    test-framework-config.schema.json
    run-summary.schema.json

  examples/
    playback-status.example.json
    test-framework-local.yaml
    test-framework-ci.yaml

  docs/
    architecture.md
    bdd-portability-rules.md
    config-contract.md
    artifact-layout.md
    evidence-contract.md
    reportportal-tags.md
    implementation-conformance.md
```

The contract must define:

```text
BDD scenarios
Config fields
Artifact layout
Evidence schemas
Required files
Naming conventions
testRunId rules
Dashboard tags
Exit-code expectations
Failure behavior
```

---

## 10. Configuration contract

Each implementation must support the same profile concept.

### Profile resolution

Examples:

```text
Java:
  -Dtest.framework.profile=local

Node/TypeScript:
  TEST_FRAMEWORK_PROFILE=local

Python:
  TEST_FRAMEWORK_PROFILE=local
```

The profile resolves to a YAML config file.

Example mapping:

```text
local  -> test-framework-local.yaml
ci     -> test-framework-ci.yaml
docker -> test-framework-docker.yaml
k8s    -> test-framework-k8s.yaml
```

### Example config

```yaml
profile: local

orchestrationApi:
  baseUrl: http://localhost:8080
  timeoutMs: 5000

displayClient:
  baseUrl: http://localhost:3000
  timeoutMs: 15000

streaming:
  hlsBaseUrl: http://localhost:8888
  streamId: camera_sync_test

browser:
  headless: true
  viewport:
    width: 1280
    height: 720

playback:
  minimumProgressSeconds: 2
  timeoutMs: 30000
  pollIntervalMs: 500

evidence:
  outputDir: build/evidence
  screenshotsEnabled: true
  logsEnabled: true

reporting:
  reportPortalEnabled: false
  launchName: streaming-lab-contract-local
```

Each implementation must load the same logical fields. The language-specific parser can differ, but the meaning must not.

### Config responsibilities

The config should answer:

- Where is the Orchestration API?
- Where is the Display Client?
- What stream should be tested?
- What timeout values should be used?
- Where should evidence be written?
- Should screenshots be captured?
- Should ReportPortal publishing be enabled?

### Config should not contain

- Test logic.
- Assertions.
- Hardcoded scenario-specific behavior.
- Implementation-specific hacks.

---

## 11. `testRunId` correlation rule

The `testRunId` is the first-class correlation identifier.

Decision:

```text
Use testRunId everywhere.
Do not introduce testExecutionId.
```

Reason:

```text
testExecutionId is too similar and confusing, especially when multiple test-framework implementations exist.
```

The `testRunId` is generated by the Orchestration API and must appear in:

```text
playback-status.json
run-summary.json
playback.log
screenshot filenames or metadata
artifact folder names
ReportPortal attributes
CI uploaded artifact names
future LLM/RAG analysis correlation
```

Each implementation normally creates its own independent test run, so each implementation will usually have a different `testRunId`.

Example:

```text
artifacts/jvm/{testRunId}/...
artifacts/node/{testRunId}/...
artifacts/python/{testRunId}/...
```

The contract requirement is not that all implementations share the same `testRunId`. The requirement is that each implementation consistently uses its own generated `testRunId` across all artifacts.

---

## 12. Artifact layout contract

All implementations must write the same artifact structure.

Recommended layout:

```text
artifacts/
  {implementation}/
    {profile}/
      {testRunId}/
        run-summary.json
        playback-status.json
        playback.log
        screenshots/
          display-client-loaded.png
          playback-progressed.png
        raw-results/
          cucumber-report.json
          junit-results.xml
        browser/
          console.log
          network.log
```

Example:

```text
artifacts/
  jvm/
    local/
      8f6b2a4e-.../
        run-summary.json
        playback-status.json
        playback.log
        screenshots/
        raw-results/

  node/
    local/
      0d73c6aa-.../
        run-summary.json
        playback-status.json
        playback.log
        screenshots/
        raw-results/

  python/
    local/
      b1a93a0c-.../
        run-summary.json
        playback-status.json
        playback.log
        screenshots/
        raw-results/
```

### Required files

Minimum required files per successful run:

```text
run-summary.json
playback-status.json
playback.log
at least one screenshot
raw test result file
```

For failed runs, the framework should still attempt to write:

```text
run-summary.json
playback.log
failure screenshot if browser was opened
partial playback-status.json when possible
raw result file
```

---

## 13. Evidence JSON vs logs

The project separates structured evidence from logs.

### Structured evidence

Example:

```text
playback-status.json
run-summary.json
```

Purpose:

- Machine-readable evidence.
- Schema validation.
- Dashboard attachment.
- CI artifact validation.
- Future LLM/RAG analysis.
- Historical comparison.

### Logs

Example:

```text
playback.log
browser/console.log
browser/network.log
```

Purpose:

- Debugging.
- Timeline reconstruction.
- Human investigation.
- Future automated failure classification.

Logs should be consistent and analysis-friendly, but they are not schema-validated evidence.

---

## 14. Playback evidence contract

Each implementation must produce equivalent playback evidence.

A minimal `playback-status.json` should include:

```json
{
  "testRunId": "8f6b2a4e-0000-4000-9000-000000000000",
  "implementation": "jvm",
  "profile": "local",
  "streamId": "camera_sync_test",
  "displayId": "DISPLAY_01",
  "status": "PASSED",
  "playback": {
    "videoLoaded": true,
    "videoWidth": 1280,
    "videoHeight": 720,
    "initialCurrentTime": 0.25,
    "finalCurrentTime": 3.12,
    "progressed": true
  },
  "artifacts": {
    "logFile": "playback.log",
    "screenshots": [
      "screenshots/display-client-loaded.png",
      "screenshots/playback-progressed.png"
    ]
  }
}
```

The exact schema should live in:

```text
test-framework-contract/schemas/playback-status.schema.json
```

All three implementations must validate against this schema before completing successfully.

---

## 15. Run summary contract

Each implementation should also write `run-summary.json`.

Purpose:

```text
Summarize the execution at framework level, independent of playback details.
```

Suggested fields:

```json
{
  "implementation": "node",
  "framework": "cucumber-js",
  "profile": "local",
  "testRunId": "0d73c6aa-0000-4000-9000-000000000000",
  "scenario": {
    "feature": "HLS playback validation",
    "name": "Validate playback progression for a display client",
    "tags": ["@contract", "@smoke", "@playback"]
  },
  "status": "PASSED",
  "startedAt": "2026-07-01T10:00:00Z",
  "finishedAt": "2026-07-01T10:00:25Z",
  "durationMs": 25000,
  "artifactDirectory": "artifacts/node/local/0d73c6aa-..."
}
```

---

## 16. Dashboard and reporting decision

### Selected tool: ReportPortal

ReportPortal is the selected dashboard tool.

It is responsible for:

```text
Centralized test launches
Scenario results
Logs
Screenshots
Attachments
History
Trends
Failure analysis
Cross-implementation visibility
```

ReportPortal should receive results from:

```text
Java/Cucumber JVM
Node/CucumberJS
Python/pytest-bdd or pytest
```

### Why ReportPortal instead of Allure

Allure was considered but is intentionally out of scope for now.

Allure is useful for local or CI-generated HTML reports, but it is not the selected central dashboard for this project.

Current decision:

```text
Do not use Allure now.
Use ReportPortal as the single dashboard.
```

### Why ReportPortal instead of Testkube

Testkube was considered but is intentionally out of scope for now.

Reason:

```text
The current project needs dashboard/result/artifact visibility more than Kubernetes-native test orchestration.
```

Testkube can be reviewed later after the system already runs in Kubernetes.

Current decision:

```text
Do not use Testkube now.
Review it later after Kubernetes maturity.
```

---

## 17. ReportPortal metadata contract

All implementations must use consistent ReportPortal metadata.

Recommended launch naming:

```text
streaming-lab-contract-{profile}
```

Examples:

```text
streaming-lab-contract-local
streaming-lab-contract-ci
streaming-lab-contract-docker
```

Recommended attributes:

```text
project:streaming-lab
implementation:jvm
implementation:node
implementation:python
framework:cucumber-jvm
framework:cucumber-js
framework:pytest-bdd
profile:local
profile:ci
layer:contract
component:orchestration-api
component:display-client
component:streaming
artifact-contract:v1
```

Each run should attach:

```text
run-summary.json
playback-status.json
playback.log
screenshots
raw result files
```

---

## 18. Java implementation

### Purpose

The Java implementation proves that the contract can be implemented in the JVM ecosystem.

Recommended stack:

```text
Java
JUnit Platform
Cucumber JVM
Gradle
SnakeYAML for YAML config
Java HTTP Client or Spring WebClient
Optional Playwright Java for browser validation
ReportPortal Java/Cucumber integration
```

### Module structure

```text
test-framework-jvm/
  build.gradle.kts
  src/test/java/
    config/
      TestFrameworkConfig.java
      TestFrameworkConfigLoader.java
    client/
      OrchestrationApiClient.java
      DisplayClientDriver.java
    evidence/
      ArtifactLayoutManager.java
      PlaybackStatusWriter.java
      RunSummaryWriter.java
    wait/
      ConditionWaiter.java
    steps/
      TestRunSteps.java
      PlaybackSteps.java
      EvidenceSteps.java
    hooks/
      EvidenceHooks.java
  src/test/resources/
    cucumber.properties
```

### Java responsibilities

```text
Load shared YAML config
Run shared feature files
Create test run through API
Prepare/start/stop through API
Open Display Client if browser validation is part of the scenario
Verify playback progress
Write evidence
Attach artifacts to ReportPortal
Exit non-zero on failed contract validation
```

---

## 19. Node.js + TypeScript implementation

### Purpose

The Node/TypeScript implementation proves that the same contract can be implemented in a frontend/browser-friendly ecosystem.

Recommended stack:

```text
Node.js
TypeScript
CucumberJS
Playwright TypeScript
YAML parser
AJV for JSON schema validation
ReportPortal CucumberJS integration or compatible reporter
```

### Module structure

```text
test-framework-node/
  package.json
  tsconfig.json
  cucumber.js
  src/
    config/
      testFrameworkConfig.ts
      testFrameworkConfigLoader.ts
    client/
      orchestrationApiClient.ts
      displayClientDriver.ts
    evidence/
      artifactLayoutManager.ts
      playbackStatusWriter.ts
      runSummaryWriter.ts
    wait/
      conditionWaiter.ts
    steps/
      testRun.steps.ts
      playback.steps.ts
      evidence.steps.ts
    hooks/
      evidenceHooks.ts
```

### Node responsibilities

```text
Load shared YAML config
Run shared feature files
Create test run through API
Prepare/start/stop through API
Open Display Client with Playwright
Verify playback progress
Write evidence
Validate JSON schema
Attach artifacts to ReportPortal
Exit non-zero on failed contract validation
```

---

## 20. Python implementation

### Purpose

The Python implementation proves that the same BDD contract can be implemented in the pytest ecosystem.

Recommended stack:

```text
Python
pytest
pytest-bdd
requests or httpx
PyYAML
jsonschema
Optional Playwright Python for browser validation
ReportPortal pytest integration
```

### Module structure

```text
test-framework-python/
  pyproject.toml
  tests/
    test_contract_features.py
  src/
    config/
      test_framework_config.py
      test_framework_config_loader.py
    client/
      orchestration_api_client.py
      display_client_driver.py
    evidence/
      artifact_layout_manager.py
      playback_status_writer.py
      run_summary_writer.py
    wait/
      condition_waiter.py
    steps/
      test_run_steps.py
      playback_steps.py
      evidence_steps.py
```

### Python responsibilities

```text
Load shared YAML config
Run shared feature files through pytest-bdd
Create test run through API
Prepare/start/stop through API
Open Display Client if browser validation is part of the scenario
Verify playback progress
Write evidence
Validate JSON schema
Attach artifacts to ReportPortal
Exit non-zero on failed contract validation
```

---

## 21. Common implementation components

Each language implementation should have equivalent conceptual components.

```text
ConfigLoader
  Loads YAML by profile.

OrchestrationApiClient
  Calls the Spring Boot Orchestration API.

DisplayClientDriver
  Opens the Display Client and extracts browser/video diagnostics.

ConditionWaiter
  Polls until conditions become true or timeout occurs.

ArtifactLayoutManager
  Creates standardized artifact directories.

PlaybackStatusWriter
  Writes schema-compliant playback-status.json.

RunSummaryWriter
  Writes run-summary.json.

EvidenceLogger
  Writes playback.log and correlation-friendly log messages.

ReportPortalPublisher
  Sends results, logs, screenshots, and JSON attachments to ReportPortal.

StepDefinitions
  Bind shared Gherkin steps to implementation code.
```

These classes do not need identical names in every language, but the responsibilities should remain equivalent.

---

## 22. System behavior contract

Every implementation must execute the same observable behavior.

Required behavior:

```text
1. Load active test framework config.
2. Create a new test run using the Orchestration API.
3. Store the returned testRunId.
4. Prepare the test run.
5. Start the stream.
6. Open the Display Client for the configured stream.
7. Wait until the video element is loaded.
8. Capture initial playback time.
9. Wait until playback progresses.
10. Capture final playback time.
11. Capture screenshot.
12. Write playback-status.json.
13. Write playback.log.
14. Stop the test run.
15. Verify final test run status.
16. Write run-summary.json.
17. Validate evidence against schema.
18. Publish or attach results to ReportPortal when enabled.
```

---

## 23. Orchestration API lifecycle

The framework interacts with these API endpoints:

```text
POST /api/test-runs
POST /api/test-runs/{id}/prepare
POST /api/test-runs/{id}/stream
POST /api/test-runs/{id}/stop
POST /api/test-runs/{id}/fail
GET  /api/test-runs/{id}
```

Expected lifecycle:

```text
CREATED -> PREPARING -> STREAMING -> STOPPED
                              |
                              -> FAILED
```

The framework should validate both happy-path and failure-path behavior, but the first conformance scenario should remain focused:

```text
create -> prepare -> stream -> display playback -> stop -> verify stopped -> write evidence
```

---

## 24. Conformance matrix

The conformance matrix defines what every implementation must support.

| Contract requirement | Java/JUnit/Cucumber JVM | Node/TS/CucumberJS | Python/pytest-bdd |
|---|---:|---:|---:|
| Uses shared `.feature` files | Required | Required | Required |
| Loads YAML config by profile | Required | Required | Required |
| Calls Orchestration API | Required | Required | Required |
| Creates `testRunId` through API | Required | Required | Required |
| Prepares test run | Required | Required | Required |
| Starts stream | Required | Required | Required |
| Opens Display Client | Required | Required | Required |
| Verifies video loaded | Required | Required | Required |
| Verifies playback progresses | Required | Required | Required |
| Captures screenshot | Required | Required | Required |
| Writes `playback-status.json` | Required | Required | Required |
| Writes `run-summary.json` | Required | Required | Required |
| Writes `playback.log` | Required | Required | Required |
| Validates evidence schema | Required | Required | Required |
| Publishes or attaches to ReportPortal | Required when enabled | Required when enabled | Required when enabled |
| Uses common dashboard tags | Required | Required | Required |
| Exits non-zero on failed scenario | Required | Required | Required |

---

## 25. CI/CD model

### Primary CI

GitHub Actions remains the primary CI system because it already exists in the project and is repo-native.

Recommended jobs:

```text
api-ci
  build and test Orchestration API

display-ci
  build and test Display Client later

contract-jvm
  run Java implementation

contract-node
  run Node/TypeScript implementation

contract-python
  run Python implementation

contract-artifact-validation
  validate artifacts from all implementations

publish-artifacts
  upload evidence/logs/screenshots/raw results
```

### Jenkins role

Jenkins should be added later as a DevOps learning track.

Jenkins should not introduce a new test framework.

Jenkins should run the same commands as GitHub Actions:

```text
Start Docker Compose
Run Java contract implementation
Run Node contract implementation
Run Python contract implementation
Collect artifacts
Publish/attach results
Stop Docker Compose
```

Jenkins portfolio skills gained:

```text
Pipeline-as-code
Jenkinsfile
Self-hosted CI
Build agents
Credentials
Artifact publishing
Docker-based CI
Report publishing
Pipeline troubleshooting
Eventually Kubernetes-based Jenkins agents
```

Current decision:

```text
GitHub Actions remains primary.
Jenkins is added later for DevOps skill-building.
Do not replace GitHub Actions now.
```

---

## 26. Docker Compose runtime

Docker Compose remains the local system runtime.

It should run:

```text
orchestration-api
display-client
mediamtx
ffmpeg-generator
reportportal
optional acceptance-test containers later
```

Current local mental model:

```text
Docker Compose provides system/environment confidence.
Gradle/npm/pytest provide test execution.
ReportPortal provides dashboard visibility.
```

The test implementations can initially run from the host against Compose services.

Later, each implementation can also be containerized:

```text
test-framework-jvm-runner
test-framework-node-runner
test-framework-python-runner
```

---

## 27. Kubernetes and AWS migration path

Kubernetes is a future phase, not the immediate priority.

The clean path is:

```text
Phase 1:
  Docker Compose locally

Phase 2:
  Local Kubernetes with kind, minikube, or Docker Desktop Kubernetes

Phase 3:
  Kubernetes manifests or Helm charts for Streaming Lab services

Phase 4:
  Run tests as Kubernetes Jobs

Phase 5:
  AWS EKS migration
```

Important distinction:

```text
Kubernetes control plane
  manages the cluster

Worker nodes
  run Pods/workloads

Test framework
  runs as containers, Jobs, or CI-driven processes
```

The test framework does not become a Kubernetes worker.

On AWS, Amazon EKS can provide managed Kubernetes control-plane capabilities, reducing the need to manually operate the Kubernetes control plane.

---

## 28. Testkube decision

Testkube is intentionally excluded for now.

Reason:

```text
The project currently needs a dashboard and artifact/result analysis first.
ReportPortal is the better fit for that requirement.
```

Testkube is more relevant when the project already runs tests natively inside Kubernetes and needs Kubernetes-native test orchestration.

Current decision:

```text
Do not use Testkube now.
Review Testkube later after Kubernetes maturity.
```

### Testkube licensing limitation recap

The concern is not that Testkube cannot be used.

The concern is that Testkube’s ecosystem has a distinction between:

```text
Open-source/standalone agent functionality
Commercial/connected control plane and advanced platform features
```

For the current project requirement — free/open-source dashboard and artifact analysis — ReportPortal is the better first choice.

### Testkube vs regular Pod/Job recap

```text
Regular Kubernetes Job
  Runs a containerized test command.
  You design artifact collection, logs, history, and result publishing.

Testkube
  Adds a Kubernetes-native testing orchestration layer above raw Pods/Jobs.
  Useful later, but premature now.
```

---

## 29. Allure decision

Allure is intentionally excluded for now.

Reason:

```text
It would add a second reporting system before the main dashboard is stable.
```

Allure may be useful later for local HTML reports, but it is not necessary for the current architecture.

Current decision:

```text
Do not use Allure now.
Use ReportPortal as the single dashboard/reporting system.
```

---

## 30. ReportPortal deployment model

ReportPortal should be introduced after at least one implementation is stable.

Recommended order:

```text
1. Run ReportPortal with Docker Compose.
2. Configure one project for Streaming Lab.
3. Publish Java implementation results first.
4. Add Node results.
5. Add Python results.
6. Standardize launch names and attributes.
7. Attach artifacts consistently.
8. Later migrate ReportPortal to Kubernetes if needed.
```

Do not start with ReportPortal before the contract has at least one working implementation.

---

## 31. Recommended implementation phases

### Phase 0 — Preserve current smoke tests

Keep the existing Gradle smoke tests working.

Do not break current CI while building the new framework.

### Phase 1 — Create the shared contract

Create:

```text
test-framework-contract/features/
test-framework-contract/schemas/
test-framework-contract/examples/
test-framework-contract/docs/
```

Define:

```text
BDD feature files
Config schema
Evidence schema
Artifact layout
ReportPortal tags
Conformance matrix
```

### Phase 2 — Implement Java first

Reason:

```text
The Orchestration API is Java/Spring Boot.
The current smoke test is Gradle-based.
Java is the easiest first implementation.
```

Goal:

```text
Run shared feature file through Cucumber JVM/JUnit Platform.
Produce contract-compliant artifacts.
```

### Phase 3 — Implement Node/TypeScript second

Reason:

```text
The Display Client is React + TypeScript.
Playwright TypeScript is natural for browser validation.
```

Goal:

```text
Run the same shared feature file through CucumberJS.
Produce the same contract-compliant artifacts.
```

### Phase 4 — Implement Python third

Reason:

```text
Python strengthens evidence validation, QA analytics, and data/LLM-readiness skills.
```

Goal:

```text
Run the same shared feature file through pytest-bdd.
Produce the same contract-compliant artifacts.
```

### Phase 5 — Add ReportPortal

Add dashboard publishing after artifacts are stable.

Goal:

```text
One dashboard showing all three implementations with consistent tags and attachments.
```

### Phase 6 — Add Jenkins

Add Jenkins after GitHub Actions remains stable.

Goal:

```text
Demonstrate pipeline-as-code and self-hosted CI skills without changing the framework design.
```

### Phase 7 — Kubernetes later

Move from Docker Compose to Kubernetes only after the framework is stable.

Goal:

```text
Run Streaming Lab services and test runners as Kubernetes workloads.
```

### Phase 8 — Review Testkube later

Only review Testkube after Kubernetes-based test execution exists.

---

## 32. Suggested repository layout

Recommended monorepo layout:

```text
multi-display-stream-sync/
  docker-compose.yml
  docker-compose-smoke.yml

  docs/
    test-framework/
      architecture.md
      bdd-portability-rules.md
      config-contract.md
      artifact-layout.md
      evidence-contract.md
      reportportal-tags.md
      implementation-conformance.md

  test-framework-contract/
    features/
      test-run-lifecycle.feature
      hls-playback-validation.feature
      playback-evidence-contract.feature
    schemas/
      playback-status.schema.json
      test-framework-config.schema.json
      run-summary.schema.json
    examples/
      test-framework-local.yaml
      test-framework-ci.yaml
      playback-status.example.json

  test-framework-jvm/
    build.gradle.kts
    src/test/java/...
    src/test/resources/...

  test-framework-node/
    package.json
    tsconfig.json
    cucumber.js
    src/...

  test-framework-python/
    pyproject.toml
    tests/...
    src/...

  streaming-lab-orchestration-api/
    ...

  display-client/
    ...
```

Alternative: keep the first Java implementation inside `streaming-lab-orchestration-api` temporarily, then extract later. But for the final portfolio architecture, separate test-framework modules are cleaner.

---

## 33. Suggested commands

### Java

```bash
./gradlew :test-framework-jvm:test \
  -Dtest.framework.profile=local
```

### Node/TypeScript

```bash
cd test-framework-node
TEST_FRAMEWORK_PROFILE=local npm run test:bdd
```

### Python

```bash
cd test-framework-python
TEST_FRAMEWORK_PROFILE=local pytest
```

### Future parent command

```bash
./scripts/run-contract-conformance.sh local
```

The parent script should eventually:

```text
Start/verify Docker Compose services
Run Java implementation
Run Node implementation
Run Python implementation
Validate artifacts
Collect outputs
Exit non-zero if any implementation fails
```

---

## 34. Quality gates

The framework should eventually enforce these quality gates:

```text
All shared feature files are parsed successfully.
All required scenarios run in all implementations.
All implementations produce artifacts.
All playback-status.json files pass schema validation.
All run-summary.json files pass schema validation.
All artifact folders include testRunId.
All logs include testRunId.
All screenshots are stored in the expected folder.
ReportPortal publishing succeeds when enabled.
CI uploads artifacts even when tests fail.
```

---

## 35. Risks and mitigations

### Risk 1: Three implementations become too much work

Mitigation:

```text
Build incrementally.
Start with Java.
Add Node only after Java is stable.
Add Python only after Node is stable.
```

### Risk 2: Feature files drift

Mitigation:

```text
Only one shared feature directory.
No language-specific feature copies.
Contract changes first; implementations adapt.
```

### Risk 3: Step definitions diverge semantically

Mitigation:

```text
Use a conformance matrix.
Keep step names identical.
Review step additions.
Write domain-level steps.
```

### Risk 4: Artifact structures differ by language

Mitigation:

```text
Schema validation.
Artifact layout validation.
Shared examples.
CI quality gate.
```

### Risk 5: Too many tools confuse the architecture

Mitigation:

```text
Only use ReportPortal for dashboard.
Do not use Allure now.
Do not use Testkube now.
Jenkins is CI learning only, not part of the test framework.
```

### Risk 6: Browser validation differs across languages

Mitigation:

```text
Define observable playback behavior, not implementation details.
Use equivalent browser diagnostics.
Validate final evidence schema rather than internal code paths.
```

---

## 36. Interview explanation

A concise interview explanation:

> I designed the Streaming Lab test framework as a polyglot BDD conformance platform. The source of truth is a shared set of Gherkin feature files and a shared contract for configuration, evidence, artifacts, and dashboard metadata. I then implemented the same contract in Java with JUnit/Cucumber JVM, Node.js with TypeScript/CucumberJS, and Python with pytest-bdd. Each implementation drives the same streaming workflow, validates playback progression, produces schema-compliant evidence correlated by `testRunId`, and publishes results to ReportPortal.

A deeper explanation:

> The reason for multiple implementations was not to duplicate tools randomly. It was to prove that the test framework contract is language-agnostic. Java aligns with backend/API automation, TypeScript aligns with browser and frontend ecosystems, and Python aligns with evidence validation and QA analytics. The common BDD contract prevents fragmentation, while ReportPortal gives one dashboard across all implementations.

---

## 37. What to avoid saying

Avoid saying:

```text
I used many test frameworks because I wanted to learn tools.
```

Better:

```text
I designed a shared test-framework contract and implemented it in multiple languages to demonstrate conformance, portability, and cross-stack automation design.
```

Avoid saying:

```text
BDD replaced Jira.
```

Better:

```text
BDD replaced the manual test-step repository for executable acceptance scenarios, while backlog and defect tracking remain separate concerns.
```

Avoid saying:

```text
Jenkins is part of the test framework.
```

Better:

```text
Jenkins is an additional CI runner used to practice DevOps pipeline-as-code. The same test framework can run locally, in GitHub Actions, or in Jenkins.
```

---

## 38. Immediate next step

The immediate next step should be:

```text
Create the shared test-framework contract before adding more implementation code.
```

Start with:

```text
docs/test-framework/implementation-conformance.md
docs/test-framework/artifact-layout.md
docs/test-framework/config-contract.md
test-framework-contract/features/hls-playback-validation.feature
test-framework-contract/schemas/playback-status.schema.json
test-framework-contract/examples/test-framework-local.yaml
```

Do not add ReportPortal, Jenkins, or Kubernetes until the first contract implementation works.

Recommended first milestone:

```text
Java implementation runs one shared BDD scenario and produces valid artifacts.
```

---

## 39. Future new-chat prompt

Use this prompt to continue in a new chat:

```text
I am working on the Streaming Lab project in the multi-display-stream-sync repository.

I want to build a polyglot BDD conformance test framework.

Architecture decision:
- One shared BDD contract is the source of truth.
- Shared Gherkin feature files live in test-framework-contract/features.
- Java, Node/TypeScript, and Python implementations must all run the same feature files.
- Java uses JUnit Platform + Cucumber JVM.
- Node uses TypeScript + CucumberJS.
- Python uses pytest + pytest-bdd.
- All implementations must satisfy the same config contract, evidence schema, artifact layout, testRunId correlation rules, and ReportPortal tag rules.
- ReportPortal is the selected dashboard tool.
- Allure is out of scope for now.
- Testkube is out of scope for now and will be reviewed after Kubernetes maturity.
- GitHub Actions remains primary CI.
- Jenkins may be added later as a DevOps learning track, not as a new test framework.
- Docker Compose is the current runtime.
- Kubernetes/AWS EKS migration is a later phase.

Important project rules:
- Use testRunId as the only first-class correlation ID.
- Do not introduce testExecutionId.
- Evidence JSON and logs are both first-class artifacts, but only JSON evidence is schema-validated.
- Logs should be designed for human debugging and future LLM-based analysis.
- Feature files should use portable Gherkin and avoid runner-specific behavior.
- The implementations must not fork feature files.
- The immediate next step is to define the shared contract documents and create the first Java implementation.

Please guide me step by step as an educator, software architect, senior backend engineer, staff QA/test strategy advisor, and DevOps advisor. Do not give code unless I ask. Help me think critically and be interview-ready.
```

---

## 40. References checked during planning

These sources were checked to confirm tool direction and current status as of July 1, 2026:

- Cucumber JVM installation documentation: https://cucumber.io/docs/installation/java/
- CucumberJS installation documentation: https://cucumber.io/docs/installation/javascript/
- Cucumber API/run documentation: https://cucumber.io/docs/cucumber/api/
- pytest-bdd documentation: https://pytest-bdd.readthedocs.io/en/latest/
- ReportPortal documentation: https://reportportal.io/docs/
- ReportPortal Docker deployment documentation: https://reportportal.io/docs/installation-steps/DeployWithDocker/
- ReportPortal GitHub repository: https://github.com/reportportal/reportportal
- Testkube Open Source documentation: https://docs.testkube.io/articles/open-source
- Testkube licensing FAQ: https://docs.testkube.io/articles/testkube-licensing-FAQ
- Jenkins Pipeline documentation: https://www.jenkins.io/doc/book/pipeline/
- Jenkins Pipeline-as-Code documentation: https://www.jenkins.io/doc/book/pipeline/pipeline-as-code/
- Kubernetes architecture documentation: https://kubernetes.io/docs/concepts/architecture/
- Kubernetes components documentation: https://kubernetes.io/docs/concepts/overview/components/
- Amazon EKS documentation: https://docs.aws.amazon.com/eks/latest/userguide/what-is-eks.html
```
