package com.streaminglab.testframework.evidence;

import com.streaminglab.testframework.model.ArtifactPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Creates the standardized artifact directory layout for one test run. */
public class ArtifactLayoutManager {

  public ArtifactPaths create(
      String outputDir, String implementation, String profile, UUID testRunId) throws IOException {
    Path rootDirectory = Path.of(outputDir, implementation, profile, testRunId.toString());
    Path screenshotsDirectory = rootDirectory.resolve("screenshots");
    Path rawResultsDirectory = rootDirectory.resolve("raw-results");
    Path browserDirectory = rootDirectory.resolve("browser");

    Files.createDirectories(screenshotsDirectory);
    Files.createDirectories(rawResultsDirectory);
    Files.createDirectories(browserDirectory);

    return new ArtifactPaths(
        rootDirectory,
        screenshotsDirectory,
        rawResultsDirectory,
        browserDirectory,
        rootDirectory.resolve("playback-status.json"),
        rootDirectory.resolve("run-summary.json"),
        rootDirectory.resolve("playback.log"));
  }
}
