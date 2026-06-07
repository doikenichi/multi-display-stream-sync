export type PlaybackStatus =
  | "INITIALIZING"
  | "LOADING_STREAM"
  | "MANIFEST_LOADED"
  | "PLAYING"
  | "BUFFERING"
  | "PAUSED"
  | "ERROR"
  | "UNSUPPORTED";

export interface DisplayConfig {
  displayId: string;
  streamUrl: string;
  sourceId?: string;
  testRunId?: string;
  autoplay: boolean;
  muted: boolean;
  debug: boolean;
}

export interface PlaybackError {
  type?: string;
  details?: string;
  fatal?: boolean;
  message: string;
}

export type PlaybackMode = "none" | "hls.js" | "native" | "unsupported";

export interface VideoDiagnostics {
  readyState: number;
  currentTime: number;
  videoWidth: number;
  videoHeight: number;
}
