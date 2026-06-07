import {useDisplayConfig} from "../hooks/useDisplayConfig";
import {useHlsPlayer} from "../hooks/useHlsPlayer";
import type {PlaybackError, PlaybackStatus} from "../types/display";
import {DebugPanel} from "./DebugPanel";
import {PlaybackErrorView} from "./PlaybackErrorView";
import {PlaybackStatusView} from "./PlaybackStatusView";
import {VideoPlayer} from "./VideoPlayer";

export function DisplayPage() {
  const {config, error} = useDisplayConfig();

  const {
    videoRef,
    playbackStatus: hlsPlaybackStatus,
    playbackError: hlsPlaybackError,
    playbackMode,
    videoDiagnostics
  } = useHlsPlayer({
    streamUrl: config?.streamUrl,
    autoplay: config?.autoplay ?? true,
  });

  const playbackStatus: PlaybackStatus = error ? "ERROR" : hlsPlaybackStatus;

  const playbackError: PlaybackError | null = error
    ? {
      message: error,
      type: "CONFIG_ERROR",
      fatal: true,
    }
    : hlsPlaybackError;

  if (!config) {
    return (
      <main className="page">
        <section className="display-card">
          <header className="display-header">
            <h1>Display Client</h1>
            <p>Browser-based virtual display</p>
          </header>

          <PlaybackStatusView status={playbackStatus}/>
          <PlaybackErrorView error={playbackError}/>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="display-card">
        <header className="display-header">
          <h1 data-testid="display-id">{config.displayId}</h1>
          <p>Browser-based virtual display</p>
        </header>

        <VideoPlayer config={config} videoRef={videoRef}/>

        <PlaybackStatusView status={playbackStatus}/>

        <PlaybackErrorView error={playbackError}/>

        {config.debug && (
          <DebugPanel
            config={config}
            playbackStatus={playbackStatus}
            playbackError={playbackError}
            playbackMode={playbackMode}
            videoDiagnostics={videoDiagnostics}
          />
        )}
      </section>
    </main>
  );
}
