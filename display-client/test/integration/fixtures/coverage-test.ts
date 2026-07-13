import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { expect, test as baseTest } from "@playwright/test";

type IstanbulCoverage = Record<string, unknown>;

declare global {
  interface Window {
    __coverage__?: IstanbulCoverage;

    collectIstanbulCoverage?: (coverageJson: string) => Promise<void>;
  }
}

const coverageDirectory = resolve(process.cwd(), ".nyc_output");

async function persistCoverage(
  coverageJson: string | undefined,
): Promise<boolean> {
  if (!coverageJson) {
    return false;
  }

  const coverage = JSON.parse(coverageJson) as IstanbulCoverage;

  if (Object.keys(coverage).length === 0) {
    return false;
  }

  await mkdir(coverageDirectory, {
    recursive: true,
  });

  const outputFile = join(
    coverageDirectory,
    `playwright-${process.pid}-${randomUUID()}.json`,
  );

  await writeFile(outputFile, JSON.stringify(coverage), "utf8");

  return true;
}

// Extend basic test by providing a "todoPage" fixture.
export const test = baseTest.extend({
  context: async ({ context }, use) => {
    /*
     * Normal integration tests should not require coverage
     * instrumentation or produce coverage files.
     */
    if (process.env.VITE_COVERAGE !== "true") {
      await use(context);
      return;
    }

    await mkdir(coverageDirectory, {
      recursive: true,
    });

    let coverageCollected = false;

    /*
     * Expose a Node function to every browser page.
     * Browser code calls it before navigation or reload.
     */
    await context.exposeFunction(
      "collectIstanbulCoverage",
      async (coverageJson: string) => {
        const persisted = await persistCoverage(coverageJson);

        coverageCollected = coverageCollected || persisted;
      },
    );

    /*
     * Preserve coverage before the browser navigates away.
     * A navigation replaces window.__coverage__ with a new
     * document-level coverage object.
     */
    await context.addInitScript(() => {
      window.addEventListener("beforeunload", () => {
        if (!window.__coverage__) {
          return;
        }

        void window.collectIstanbulCoverage?.(
          JSON.stringify(window.__coverage__),
        );
      });
    });

    await use(context);

    /*
     * Collect coverage from the final document after the
     * test finishes.
     */
    for (const page of context.pages()) {
      if (page.isClosed()) {
        continue;
      }

      const coverageJson = await page.evaluate(() =>
        window.__coverage__ ? JSON.stringify(window.__coverage__) : undefined,
      );

      const persisted = await persistCoverage(coverageJson);

      coverageCollected = coverageCollected || persisted;
    }

    if (!coverageCollected) {
      throw new Error(
        [
          "No Istanbul coverage was collected.",
          "Confirm that VITE_COVERAGE=true reached the Vite server",
          "and that the server was not an existing non-instrumented instance.",
        ].join(" "),
      );
    }
  },
});

export { expect };
