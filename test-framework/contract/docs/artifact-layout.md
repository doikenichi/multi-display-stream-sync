# Artifact Layout Contract

All implementations must write artifacts using the same logical layout.

```text
artifacts/
  {implementation}/
    {profile}/
      {testRunId}/
        run-summary.json
        playback-status.json
        playback.log
        screenshots/
          playback-progressed.png
        raw-results/
          cucumber-report.json
          junit-results.xml
        browser/
          console.log
          network.log
```

## Required successful-run files

```text
run-summary.json
playback-status.json
playback.log
at least one screenshot
raw test result file
```

## Failed-run behavior

When possible, failed runs should still write:

```text
run-summary.json
playback.log
failure screenshot if browser was opened
partial playback-status.json when playback data exists
raw result file
```

Runtime artifacts should not be committed to the repository. They should be produced locally and uploaded by CI.
