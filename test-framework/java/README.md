# Streaming Lab Java Test Framework

This module is the Java implementation of the shared Streaming Lab BDD contract.

It consumes shared feature files from:

```text
../features
```

It consumes shared schemas/config/examples/docs from:

```text
../contract
```

Current milestone:

```text
- Gradle + JUnit Platform + Cucumber JVM are wired.
- Shared feature files are discovered from the Java test runtime.
- Step definitions are mapped and intentionally pending.
- ScenarioContext is injectable across step classes through cucumber-picocontainer.
```

Run locally from this folder:

```powershell
.\gradlew.bat test --rerun-tasks "-Dtest.framework.profile=local"
```
