# Java quality gates

Gradle is the source of truth for Java quality checks. IntelliJ plugins provide earlier feedback, while local and CI builds enforce the same qualityCheck tasks.

## What runs

| Tool | Purpose | Gate |
|---|---|---|
| Checkstyle 13.7.0 | Imports and basic source conventions | Any violation fails |
| PMD 7.24.0 | Correctness and maintainability analysis | Priority 1-5 violations fail |
| SpotBugs 4.10.2 | Bytecode defect analysis | Medium-confidence or higher findings fail |
| JaCoCo 0.8.15 | Unit-test coverage | Module baseline; regressions fail |
| Spotless | Orchestration API formatting | Formatting differences fail |

The shared policy files live under **config/**. Keep those files as the common IDE, local-build, and CI configuration.

## Local use

Run both active Java builds from the repository root:

    .\scripts\java-quality-check.ps1

Run one module while developing:

    cd streaming-lab-orchestration-api
    .\gradlew.bat qualityCheck

    cd ..\test-framework\java
    .\gradlew.bat qualityCheck

For the orchestration API, apply formatting before checking:

    .\gradlew.bat spotlessApply

Reports are written below each module's **build/reports/** directory:

- **checkstyle/**
- **pmd/**
- **spotbugs/**
- **jacoco/test/html/index.html**
- **tests/test/index.html**

The check task also enforces JaCoCo coverage. The separate jacocoTestReport task produces the browsable HTML and CI XML reports.

Current coverage floors are:

- Orchestration API: 70% line and 60% branch.
- Java test framework: 45% line and 40% branch.

The test-framework floor starts at its measured baseline. Raise it as its pending BDD steps acquire executable implementations and tests.

## IntelliJ IDEA

Import both Gradle builds:

- **streaming-lab-orchestration-api**
- **test-framework/java**

The repository includes shared **Orchestration API - qualityCheck** and **Test Framework Java - qualityCheck** run configurations under **.run/**.

Install these Marketplace plugins for editor feedback:

- **CheckStyle-IDEA**: activate **config/checkstyle/checkstyle.xml** and select Checkstyle 13.7.0.
- **PMD**: add **config/pmd/ruleset.xml** as the custom ruleset.
- **SpotBugs**: use **config/spotbugs/exclude.xml** as the exclusion filter and build before scanning.

JaCoCo does not require a Marketplace plugin. Use IntelliJ's **Run with Coverage** for exploration and the Gradle jacocoTestReport and jacocoTestCoverageVerification tasks for the official result.

If an IDE plugin and Gradle disagree, Gradle defines the gate.

## Suppressions

Prefer a source fix. Add a suppression only for a confirmed false positive or an intentional construct that cannot be expressed more clearly.

- Keep the suppression as narrow as possible.
- Add a comment explaining the reason.
- Do not lower a global threshold to hide one finding.
- Review suppressions when upgrading a tool.

## CI

GitHub Actions runs **./gradlew clean qualityCheck** for each changed Java build. Shared **config/** changes trigger both builds. Reports and test results are uploaded even when a gate fails so the failure remains diagnosable.
