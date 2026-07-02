# ReportPortal Metadata Contract

ReportPortal is the selected central dashboard after at least one implementation is stable.

## Launch naming

```text
streaming-lab-contract-{profile}
```

Examples:

```text
streaming-lab-contract-local
streaming-lab-contract-ci
```

## Required attributes

```text
project:streaming-lab
implementation:jvm|node|python
framework:cucumber-jvm|cucumber-js|pytest-bdd
profile:local|ci|docker|k8s
layer:contract
component:orchestration-api
component:display-client
component:streaming
artifact-contract:v1
```

## Required attachments when enabled

```text
run-summary.json
playback-status.json
playback.log
screenshots
raw result files
```

ReportPortal should not be added before the first implementation can produce stable local artifacts.
