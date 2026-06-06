import { useState } from "react";
import type { PlaybackStatus, PlaybackError } from "../types/display";
import { useDisplayConfig } from "../hooks/useDisplayConfig";
import { DebugPanel } from "./DebugPanel";
import { PlaybackStatusView } from "./PlaybackStatusView";
import { VideoPlayer } from "./VideoPlayer";
import { PlaybackErrorView } from "./PlaybackErrorView";

export function DisplayPage() {
  const { config, error } = useDisplayConfig();

  const [playbackStatus, setPlaybackStatus] = useState<PlaybackStatus>(
    error ? "ERROR" : "INITIALIZING",
  );

  const [playbackError, setPlaybackError] = useState<PlaybackError | null>(
    error
      ? {
          message: error,
          type: "CONFIG_ERROR",
          fatal: true,
        }
      : null,
  );

  if (!config) {
    return (
      <main className="page">
        <section className="display-card">
          <header className="display-header">
            <h1>Display Client</h1>
            <p>Browser-based virtual display</p>
          </header>

          <PlaybackStatusView status={playbackStatus} />
          <PlaybackErrorView error={playbackError} />
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

        <VideoPlayer
          config={config}
          onPlaybackStatusChange={setPlaybackStatus}
          onPlaybackErrorChange={setPlaybackError}
        />

        <PlaybackStatusView status={playbackStatus} />

        <PlaybackErrorView error={playbackError} />

        {config.debug && (
          <DebugPanel
            config={config}
            playbackStatus={playbackStatus}
            playbackError={playbackError}
          />
        )}
      </section>
    </main>
  );
}
