package com.streaminglab.testframework.client;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.streaminglab.testframework.config.TestFrameworkConfig;
import com.streaminglab.testframework.model.PlaybackObservation;
import java.nio.file.Path;

/** Browser/display-client boundary backed by Playwright. */
public class DisplayClientDriver implements AutoCloseable {

  private final TestFrameworkConfig.BrowserConfig browserConfig;
  private final TestFrameworkConfig.DisplayClientConfig displayClientConfig;

  private Playwright playwright;
  private Browser browser;
  private BrowserContext browserContext;
  private Page page;

  public DisplayClientDriver(
          TestFrameworkConfig.BrowserConfig browserConfig,
          TestFrameworkConfig.DisplayClientConfig displayClientConfig) {
    this.browserConfig = browserConfig;
    this.displayClientConfig = displayClientConfig;
  }

  public void open(String displayClientUrl) {
    playwright = Playwright.create();

    browser =
            playwright
                    .chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(browserConfig.headless()));

    browserContext =
            browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(
                                    browserConfig.viewport().width(),
                                    browserConfig.viewport().height()));

    page = browserContext.newPage();
    page.setDefaultTimeout(displayClientConfig.timeoutMs());

    page.navigate(displayClientUrl);
    page.waitForLoadState(LoadState.DOMCONTENTLOADED);

    // This only proves the Display Client accepted config and rendered the video element.
    // Actual video loading/progression belongs to the next BDD steps.
    page.waitForSelector("video");
  }

  public PlaybackObservation capturePlaybackObservation() {
    throw new UnsupportedOperationException("Playback observation capture is not implemented yet.");
  }

  public Path captureScreenshot(Path screenshotPath) {
    throw new UnsupportedOperationException("Screenshot capture is not implemented yet.");
  }

  @Override
  public void close() {
    if (page != null) {
      page.close();
    }
    if (browserContext != null) {
      browserContext.close();
    }
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }
}