# Streaming Lab Orchestration API

## Purpose

The **Streaming Lab Orchestration API** is the control-plane service for the Multi-Display Stream Sync project.

Its main responsibility is to prepare and manage streaming test runs so that the test framework can validate video
playback and synchronization across browser-based display clients.

The initial workflow is:

```text
Create test run
→ prepare run metadata and artifacts folder
→ start or coordinate stream publishing
→ expose HLS stream URL
→ allow test framework/display client to consume the stream
→ stop the run
→ preserve evidence for analysis
```

This module does **not** perform video synchronization validation directly. Validation belongs to the test framework.

---

## Project Context

This module is part of the larger **Multi-Display Stream Sync / Streaming Lab** project.

High-level architecture:

```text
Java Test Framework
        |
        | calls REST API
        v
Streaming Lab Orchestration API
        |
        | coordinates stream publishing
        v
FFmpeg
        |
        | publishes stream
        v
MediaMTX
        |
        | serves HLS
        v
Display Client
        |
        | browser playback
        v
Java Test Framework validates synchronization
```

Current project decisions:

- Use **HLS first** for simpler browser playback and easier debugging.
- Add **WebRTC later** only after the HLS workflow is stable.
- Use **FFmpeg → MediaMTX → HLS → browser clients** as the first streaming pipeline.
- Use **Docker Compose locally first** as a cloud-native stepping stone.
- Use **Java + Spring Boot** for the Orchestration API.
- Use **Java + Playwright** for the test framework.
- Keep the Display Client separate from the Orchestration API.
- Start with a **modular monolith** instead of splitting into microservices too early.

---

## Advisory Personas

Development and review of this module should be guided by four personas.

### 1. Software Development Professor / Teacher

Focus:

- Teach concepts clearly.
- Explain architecture and design patterns.
- Connect implementation decisions to software engineering principles.
- Keep the learning path structured and progressive.
- Explain why each class, package, endpoint, and test exists.

Review questions:

- Is the concept academically sound?
- Is the design understandable?
- Is the learning sequence appropriate?
- Are responsibilities separated clearly?
- Is the implementation teaching transferable engineering concepts?

---

### 2. Senior Software Developer / Software Architect

Focus:

- Make the implementation maintainable and production-oriented.
- Review API design, package structure, domain modeling, and service boundaries.
- Avoid over-engineering while preserving future extensibility.
- Keep the code clean, testable, and understandable.
- Apply design patterns only when they solve a real problem.

Review questions:

- Is this design easy for another developer to understand?
- Is the module structured for future change?
- Are dependencies pointing in the right direction?
- Are we avoiding premature infrastructure complexity?
- Are we building clear seams for future database, streaming, and artifact implementations?

---

### 3. Senior DevOps / MLOps Engineer

Focus:

- Make the module cloud-native.
- Ensure Docker, CI/CD, configuration, and observability are considered early.
- Keep local, CI, and future deployment environments consistent.
- Separate fast tests from slower integration/E2E tests.
- Ensure logs and artifacts help with defect analysis.

Review questions:

- Can this service run consistently in Docker?
- Can CI validate it automatically?
- Are environment-specific values externalized?
- Are health/readiness endpoints available?
- Are artifacts and logs easy to collect after a failure?
- Can this design later move toward Kubernetes without major redesign?

---

### 4. Staff QA Analyst / Test Strategy Lead

Focus:

- Manual and automated test strategy.
- Test scoping and risk-based testing.
- Shift-left development.
- Early defect detection.
- Defect analysis and evidence collection.
- Ensuring quality is designed into the module, not added only at the end.
- Debating and reasoning about test strategy decisions instead of only listing test cases.
- Explaining why each testing choice is made, what risk it covers, and what alternatives were considered.
- Teaching QA concepts that improve both development and staff-level quality engineering judgment.

Review questions:

- What can fail?
- What should be tested earlier?
- What belongs in unit, service, API/controller, integration, contract, BDD, or E2E tests?
- What should not be tested through slow E2E tests?
- What evidence should be collected automatically when a failure happens?
- Are we designing the system to be testable?
- Should this behavior be developed through TDD?
- Should this behavior be described through BDD?
- Does this risk require E2E coverage, or can it be covered earlier and faster?
- What testing design pattern or best practice applies here?
- What tradeoff are we accepting with this test strategy?
- What QA concept should be learned from this decision?

Reasoning framework:

For every testing recommendation, the Staff QA should explain:

```text
Testing decision:
What are we testing, and why?

Risk covered:
What defect, failure mode, or quality risk are we trying to catch?

Best test level:
Should this be unit, service, API/controller, integration, contract, BDD, or E2E?

TDD angle:
Should a failing test be written first to drive the design?

BDD angle:
Is the behavior important enough to describe as a user/system scenario?

E2E angle:
Does this require a full-system test, or would E2E be too slow and fragile?

Testing pattern or best practice:
Examples include test pyramid, risk-based testing, shift-left testing, contract testing, test doubles, deterministic test data, test data builders, observability-driven testing, and failure evidence collection.

Alternatives:
What other testing approaches could be used, and why are they weaker or stronger?

SOLID connection:
Which SOLID principle is supported by this testing or design choice?

QA learning concept:
What should be learned from this decision as a QA/SDET or Staff QA?
```

Example:

```text
Decision:
Add API/controller tests for POST /api/test-runs.

Risk covered:
The endpoint may accept invalid input, return the wrong HTTP status, expose the wrong JSON contract, or fail to call the service correctly.

Best test level:
API/controller test with MockMvc.

TDD angle:
Yes. The controller test can define the expected API behavior before the endpoint is used by the test framework.

BDD angle:
The behavior can be described as:
Given a valid stream name, when a client creates a test run, then the API returns 201 and the created run details.

E2E angle:
Not needed yet. Full E2E would be slower and would mix API, FFmpeg, MediaMTX, and browser playback concerns.

Testing best practice:
This follows the test pyramid and shift-left testing. It catches API contract issues before running the full streaming workflow.

SOLID:
S — Single Responsibility Principle. The controller test validates only HTTP behavior, not repository storage or FFmpeg orchestration.

QA concept to learn:
Test scope discipline: choose the smallest test level that gives confidence for the risk being tested.
```

---

## Design Strategy

### Architectural Style

The recommended architecture is:

```text
Modular Monolith
+ Package by Feature
+ Lightweight Hexagonal Architecture
```

This means the module is one deployable Spring Boot application, but internally organized around domain capabilities.

Initial package direction:

```text
com.streaminglab.orchestration
├── health
│   ├── api
│   └── dto
└── testrun
    ├── api
    ├── application
    ├── domain
    ├── dto
    └── repository
```

Later packages may include:

```text
streaming
artifacts
networkcapture
configuration
```

### Why Modular Monolith First?

This module should not start as multiple microservices because the domain is still small, the team is currently one
developer, and the main risk is orchestration correctness rather than independent scaling.

Microservice extraction should be considered later only when there is a clear reason, such as:

- Independent scaling needs.
- Different deployment cadence.
- Different runtime lifecycle.
- Different failure isolation needs.
- Separate team ownership.
- Complex persistence or reporting requirements.

The current goal is to design clean internal boundaries so that future extraction is possible without paying the
operational cost too early.

---

## SOLID Principles Applied

Every major design decision should explicitly identify the relevant SOLID principle.

### S — Single Responsibility Principle

Used to keep each class and package focused.

Examples:

- `HealthController` exposes a simple health endpoint.
- `TestRun` represents the state of one streaming test run.
- `TestRunStatus` defines valid lifecycle states.
- `InMemoryTestRunRepository` stores and retrieves test runs in memory.
- Future `TestRunService` should coordinate test-run creation without directly managing every external detail.

### O — Open/Closed Principle

Used to allow future extension without rewriting core orchestration logic.

Examples:

- Add a future `PostgresTestRunRepository` without changing `TestRunService`.
- Add a future `WebRtcStreamPublisher` without rewriting the orchestration workflow.
- Add new artifact storage mechanisms later.

### L — Liskov Substitution Principle

Used when multiple implementations should be safely interchangeable.

Examples:

- `InMemoryTestRunRepository` and future `PostgresTestRunRepository` should both behave correctly as`TestRunRepository`.
- Future `FfmpegStreamPublisher` and `MockStreamPublisher` should both satisfy the same stream-publishing contract.

### I — Interface Segregation Principle

Used to avoid large interfaces that force classes to implement methods they do not need.

Examples:

- Keep `TestRunRepository` small.
- Create separate abstractions later for stream publishing, artifact storage, and network capture instead of one large
  orchestration interface.

### D — Dependency Inversion Principle

Used to keep application logic dependent on abstractions, not infrastructure details.

Examples:

```text
TestRunService
    depends on
TestRunRepository
    implemented by
InMemoryTestRunRepository now
PostgresTestRunRepository later
```

Future examples:

```text
TestRunService
    depends on
StreamPublisher
    implemented by
FfmpegStreamPublisher
```

---

## Development Strategy

### Current Implementation Sequence

The module should be built incrementally:

```text
1. Create Spring Boot skeleton.
2. Add GET /health.
3. Add a controller test for /health.
4. Create TestRun and TestRunStatus domain model.
5. Create TestRunRepository abstraction.
6. Create InMemoryTestRunRepository.
7. Add repository unit tests.
8. Create TestRunService using TDD.
9. Add DTOs for creating and returning test runs.
10. Add POST /test-runs.
11. Add GET /test-runs/{testRunId}.
12. Add stop behavior.
13. Add stream publishing integration.
14. Add Docker Compose integration.
15. Add CI workflow.
```

### Current Storage Decision

The module currently uses an in-memory repository implementation:

```text
TestRunRepository → InMemoryTestRunRepository → ConcurrentHashMap
```

This is intentional.

Reason:

- The first goal is orchestration behavior, not persistence.
- A database would add complexity before it provides value.
- The repository interface protects the service from future storage changes.
- Future persistence can be added with PostgreSQL and Testcontainers when historical test-run storage becomes a real
  requirement.

H2 was considered but deferred because H2 introduces database behavior, schema, entity mapping, and datasource
configuration before persistence is needed.

JPA was also deferred because a pure domain model should not be coupled to persistence annotations too early.

---

## Testing Strategy

### Testing Philosophy

The project should follow a shift-left testing strategy.

Do not wait for full E2E tests to discover problems that can be caught earlier through unit, service, API, or
integration tests.

Recommended test flow:

```text
Fastest
Unit tests
Service tests
API/controller tests
Integration tests
E2E tests
Slowest
```

### Unit Tests

Unit tests should validate small responsibilities.

Current examples:

- `HealthControllerTest`
    - Verifies `GET /health` returns `200 OK` and `{"status":"UP"}`.
- `InMemoryTestRunRepositoryTest`
    - Verifies a saved `TestRun` can be found by ID.
    - Verifies missing IDs return `Optional.empty()`.

Testing techniques used so far:

- Responsibility-based test scoping.
- Specification-based testing.
- Positive path testing.
- Negative path testing.
- Equivalence partitioning.

### TDD Usage

TDD should be used primarily by the developer implementing production code.

The cycle should be:

```text
Write failing test
→ implement minimal code
→ refactor safely
```

Good candidates for TDD:

- `TestRunService`
- `TestRunIdGenerator`
- `StreamUrlBuilder`
- `ArtifactPathBuilder`
- status transition rules
- future `StreamPublisher` abstraction

QA/SDET should contribute by reviewing test coverage, risk areas, edge cases, and acceptance criteria.

### BDD Usage

BDD is useful for describing system behavior.

BDD should not be used for every unit test. It is most valuable for API workflows and E2E scenarios.

Example E2E BDD scenario:

```gherkin
Feature: Streaming test run orchestration

  Scenario: Create and play a streaming test run
    Given the orchestration API is running
    And MediaMTX is available
    When I create a test run for stream "camera-sync-test"
    And I open the Display Client using the returned HLS URL
    Then the Display Client should start playing the stream
    And the test run should expose artifact information
```

Recommended approach:

- Use JUnit for unit and service tests.
- Use BDD-style names/comments for API and workflow tests.
- Add Cucumber later only if executable Gherkin becomes useful.
- Use Playwright Java for E2E validation.

### E2E Testing

E2E tests should cover only high-value integrated behaviors.

Initial high-value E2E scenarios:

1. Create test run and play stream successfully.
2. Stop test run successfully.
3. Fail clearly when stream cannot start.
4. Produce useful artifacts when validation fails.

Avoid making E2E tests responsible for every small behavior. Unit and service tests should catch most defects earlier.

---

## API Strategy

### Initial Endpoint

```http
GET /health
```

Expected response:

```json
{
  "status": "UP"
}
```

Purpose:

- Prove the API starts.
- Prove routing works.
- Provide a simple manual check.
- Provide a basic endpoint visible in Swagger.

### Planned Endpoints

```http
POST /test-runs
```

Creates a new streaming test run.

```http
GET /test-runs/{testRunId}
```

Returns current test-run state.

```http
POST /test-runs/{testRunId}/stop
```

Stops a running test run.

---

## Domain Model

### TestRun

Represents one streaming test run.

Expected fields:

```text
testRunId
streamName
status
hlsInternalUrl
hlsExternalUrl
rtspPublishUrl
artifactPath
createdAt
startedAt
stoppedAt
errorMessage
```

This is a domain model, not a database entity.

It exists because the application needs to understand and manage the concept of a streaming test run.

### TestRunStatus

Represents the lifecycle of a run.

Initial statuses:

```text
CREATED
PREPARING
STREAMING
STOPPING
STOPPED
FAILED
```

This avoids using unstructured string statuses and makes future state transition testing easier.

---

## Cloud-Native Strategy

### Configuration

Environment-specific values should be externalized.

Likely future configuration:

```yaml
streaming-lab:
  artifacts-root: ./artifacts
  mediamtx:
    rtsp-base-url: rtsp://mediamtx:8554
    hls-internal-base-url: http://mediamtx:8888
    hls-external-base-url: http://localhost:8888
  ffmpeg:
    path: ffmpeg
    startup-timeout-seconds: 10
```

### Internal vs External URLs

The API may need to return both internal and external URLs.

Example:

```text
Internal Docker URL:
http://mediamtx:8888/camera-sync-test/

External browser URL:
http://localhost:8888/camera-sync-test/
```

This is important because containers and host browsers resolve network addresses differently.

### Artifacts

Planned artifact structure:

```text
artifacts/
└── runs/
    └── {testRunId}/
        ├── ffmpeg.log
        ├── orchestration.json
        ├── generated-video/
        ├── screenshots/
        ├── offsets.csv
        ├── summary.json
        └── failure-summary.md
```

The Orchestration API should initially create and manage orchestration-related files. The test framework should later
add validation-related evidence.

---

## Local Development

### Run the application

```bash
./gradlew bootRun
```

### Health check

```bash
curl http://localhost:8080/health
```

Expected:

```json
{
  "status": "UP"
}
```

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

### Run tests

```bash
./gradlew test
```

---

## CI/CD Strategy

Initial CI should be simple and fast.

Recommended first GitHub Actions workflow:

```text
- Checkout code
- Set up Java
- Run ./gradlew test
- Publish test results if needed
```

Later workflows:

1. **Fast verification workflow**
    - compile
    - unit tests
    - service tests
    - API tests

2. **E2E streaming workflow**
    - start Docker Compose
    - start MediaMTX
    - start Orchestration API
    - create a test run
    - open Display Client
    - run Playwright validation
    - upload artifacts

Do not run full streaming E2E validation for every small change until the pipeline is stable.

---

### Orchestration API smoke test

The Orchestration API includes a separate `smokeTest` Gradle task.

This task validates the already-running Docker Compose API through real HTTP calls. It does not start the Spring
application inside the test JVM.

Run Docker Compose first:

```bash
docker compose up --build -d
````

Then run:

```bash
cd streaming-lab-orchestration-api
./gradlew smokeTest
```

Current smoke coverage:

* `GET /actuator/health` returns `UP`
* API can execute `create → prepare → stream → get` and reach `STREAMING`

Run:

```bash
./gradlew smokeTest
```

Then run:

```bash 
./gradlew spotlessCheck checkstyleMain checkstyleTest test
```

---

## When to Update This README

Update this README when any of the following happens:

- A new endpoint is added.
- A package structure changes.
- A new design pattern or architectural decision is introduced.
- The storage strategy changes, such as moving from in-memory to PostgreSQL.
- FFmpeg or MediaMTX integration is implemented.
- Docker Compose support is added or changed.
- CI/CD workflow is added or changed.
- Test strategy changes.
- E2E/BDD tooling is introduced.
- Artifact structure changes.
- The module becomes deployable outside local development.
- A major decision is reversed or refined.

Recommended update checkpoints:

```text
After completing POST /test-runs
After adding FFmpeg integration
After adding Docker Compose integration
After adding CI workflow
After adding E2E Playwright tests
After adding persistence, if needed
```

---

## Current Status

Implemented or planned so far:

- Spring Boot module created.
- `GET /health` implemented.
- Health controller test added.
- `TestRun` domain model created.
- `TestRunStatus` enum created.
- `TestRunRepository` abstraction created.
- `InMemoryTestRunRepository` implemented.
- Repository unit tests planned/added.
- TDD and BDD strategy discussed.
- Modular monolith, package-by-feature, and lightweight hexagonal architecture selected.
- In-memory storage selected for v1.
- JPA, H2, and database persistence deferred until there is a real persistence requirement.

---

## Next Recommended Step

The next implementation step is to create `TestRunService` using TDD.

First expected test:

```text
shouldCreateTestRunWithCreatedStatus
```

The service should eventually:

- generate a test run ID,
- assign initial status,
- build stream URLs,
- assign artifact path,
- save the run through `TestRunRepository`,
- return the created `TestRun`.

