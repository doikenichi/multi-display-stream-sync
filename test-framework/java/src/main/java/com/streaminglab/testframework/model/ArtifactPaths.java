package com.streaminglab.testframework.model;

import java.nio.file.Path;

/** Standard artifact paths for one scenario execution. */
public record ArtifactPaths(
    Path rootDirectory,
    Path screenshotsDirectory,
    Path rawResultsDirectory,
    Path browserDirectory,
    Path playbackStatusJson,
    Path runSummaryJson,
    Path playbackLog) {}
