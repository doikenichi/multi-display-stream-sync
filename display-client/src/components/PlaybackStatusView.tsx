import type { PlaybackStatus } from "../types/display";

interface PlaybackStatusViewProps {
  status: PlaybackStatus;
}
export function PlaybackStatusView({ status }: PlaybackStatusViewProps) {
  return (
    <div className="status-row">
      <strong>Status:</strong>
      <span data-testid="playback-status">{status}</span>
    </div>
  );
}
