import type { PlaybackError } from "../types/display";

interface PlaybackErrorViewProps {
  error: PlaybackError | null;
}

export function PlaybackErrorView({ error }: PlaybackErrorViewProps) {
  if (!error) {
    return <div data-testid="playback-error" className="error-message" />;
  }

  return (
    <div role="alert" data-testid="playback-error" className="error-message">
      <strong>Error:</strong> {error.message}
    </div>
  );
}
