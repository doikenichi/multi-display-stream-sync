import type {DisplayConfig, PlaybackError, PlaybackMode, PlaybackStatus, VideoDiagnostics,} from "../types/display";

interface DebugPanelProps {
  config: DisplayConfig;
  playbackStatus: PlaybackStatus;
  playbackError: PlaybackError | null;
  playbackMode: PlaybackMode;
  videoDiagnostics: VideoDiagnostics;
}

export function DebugPanel({
                             config,
                             playbackStatus,
                             playbackError,
                             playbackMode,
                             videoDiagnostics,
                           }: DebugPanelProps) {
  return (
    <section data-testid="debug-panel" className="debug-panel">
      <h2>Debug</h2>

      <dl>
        <dt>Display ID</dt>
        <dd>{config.displayId}</dd>

        <dt>Stream URL</dt>
        <dd data-testid="stream-url">{config.streamUrl}</dd>

        <dt>Source ID</dt>
        <dd>{config.sourceId ?? "N/A"}</dd>

        <dt>Test Run ID</dt>
        <dd>{config.testRunId ?? "N/A"}</dd>

        <dt>Playback Mode</dt>
        <dd>{playbackMode}</dd>

        <dt>Video Dimensions (w x h)</dt>
        <dd>{videoDiagnostics.videoWidth}x{videoDiagnostics.videoHeight}</dd>

        <dt>Current Time</dt>
        <dd>{videoDiagnostics.currentTime.toFixed(2)}s</dd>

        <dt>Ready State</dt>
        <dd>{videoDiagnostics.readyState}</dd>

        <dt>Autoplay</dt>
        <dd>{String(config.autoplay)}</dd>

        <dt>Muted</dt>
        <dd>{String(config.muted)}</dd>

        <dt>Playback Status</dt>
        <dd>{playbackStatus}</dd>

        <dt>Last Error</dt>
        <dd>{playbackError?.message ?? "None"}</dd>

        <dt>Error Type</dt>
        <dd>{playbackError?.type ?? "N/A"}</dd>

        <dt>Error Details</dt>
        <dd>{playbackError?.details ?? "N/A"}</dd>

        <dt>Fatal Error</dt>
        <dd>
          {playbackError?.fatal === undefined
            ? "N/A"
            : String(playbackError.fatal)}
        </dd>
      </dl>
    </section>
  );
}
