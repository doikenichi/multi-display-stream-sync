/// <reference types="node" />
import { defineConfig } from "@playwright/test";

const coverageEnabled = process.env.VITE_COVERAGE === "true";

const applicationPort = coverageEnabled ? 3001 : 3000;

const applicationUrl = `http://127.0.0.1:${applicationPort}`;

export default defineConfig({
  tsconfig: "./tsconfig.playwright.json",
  testDir: "./test/integration",

  timeout: 30_000,

  expect: {
    timeout: 5_000,
  },

  fullyParallel: true,

  forbidOnly: Boolean(process.env.CI),

  retries: process.env.CI ? 2 : 0,

  workers: process.env.CI ? 1 : undefined,

  reporter: [["list"], ["html", { open: "never" }]],

  use: {
    baseURL: applicationUrl,

    /*
     * Use installed Google Chrome instead of Playwright Chromium
     * because the playback tests require media codec support.
     */
    channel: "chrome",

    headless: true,

    /*
     * Ensures page.route() and context.route() can intercept requests
     * instead of allowing a service worker to handle them first.
     */
    serviceWorkers: "block",

    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },

  webServer: {
    command: [
      "npm run dev --",
      "--host 127.0.0.1",
      `--port ${applicationPort}`,
      "--strictPort",
    ].join(" "),

    url: applicationUrl,

    /*
     * A coverage run must always start an instrumented
     * Vite process.
     */
    reuseExistingServer: !coverageEnabled && !process.env.CI,

    timeout: 120_000,
  },
});
