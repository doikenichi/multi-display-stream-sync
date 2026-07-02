# Config Contract

Every implementation must support the same logical configuration fields and profile concept.

## Profile resolution

```text
Java:              -Dtest.framework.profile=local
Node/TypeScript:   TEST_FRAMEWORK_PROFILE=local
Python:            TEST_FRAMEWORK_PROFILE=local
```

Profiles resolve to YAML files such as:

```text
test-framework-local.yaml
test-framework-ci.yaml
test-framework-docker.yaml
test-framework-k8s.yaml
```

## Config responsibilities

The config answers:

- Where `streaming-lab-orchestration-api` is running
- Where the Display Client is running
- Which stream is being tested
- Browser settings
- Playback timeout and polling settings
- Evidence output directory
- Reporting settings

## Config must not contain

- Test logic
- Assertions
- Scenario-specific hacks
- Language-specific behavior
