import { expect } from "@playwright/test";
import type { Page, Route } from "@playwright/test";
import { test } from "./fixtures/coverage-test";

test.describe("Display Client", (): void => {
  test("shows a playback error when the HLS manifest is unavailable", async ({
    page,
  }: {
    page: Page;
  }): Promise<void> => {
    // Arrange
    const displayId = "integration-test-display";
    const streamUrl =
      "http://127.0.0.1:8888/camera_sync_test/index.m3u8";

    const query: URLSearchParams = new URLSearchParams({
      displayId,
      streamUrl,
    });

    await page.route(streamUrl, async (route: Route): Promise<void> => {
      await route.fulfill({
        status: 503,
        contentType: "text/plain",
        body: "Mock stream is not configured yet.",
        headers: {
          "access-control-allow-origin": "*",
        },
      });
    });

    // Act
    await page.goto(`/display?${query.toString()}`);

    // Assert
    await expect(page.locator("video")).toBeVisible();

    const playbackError = page.getByRole("alert");

    await expect(playbackError).toBeVisible();
    await expect(playbackError).toContainText(
      "Error: Unable to load the video stream.",
    );
  });
});
