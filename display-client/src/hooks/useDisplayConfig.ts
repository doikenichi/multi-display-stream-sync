import type { DisplayConfig } from "../types/display";

function parseBoolean(value: string | null, defaultValue: boolean): boolean {
  if (value === null) {
    return defaultValue;
  }

  return value.toLowerCase() === "true";
}

export function useDisplayConfig():
  | { config: DisplayConfig; error: null }
  | { config: null; error: string } {
  const params = new URLSearchParams(window.location.search);

  const displayId = params.get("displayId");
  const streamUrl = params.get("streamUrl");

  if (!displayId) {
    return {
      config: null,
      error: "Missing required query parameter: displayId",
    };
  }

  if (!streamUrl) {
    return {
      config: null,
      error: "Missing required query parameter: streamUrl",
    };
  }

  return {
    config: {
      displayId,
      streamUrl,
      sourceId: params.get("sourceId") ?? undefined,
      testRunId: params.get("testRunId") ?? undefined,
      autoplay: parseBoolean(params.get("autoplay"), true),
      muted: parseBoolean(params.get("muted"), true),
      debug: parseBoolean(params.get("debug"), false),
    },
    error: null,
  };
}