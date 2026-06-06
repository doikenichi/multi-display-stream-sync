import { useEffect } from "react";
import type {
  DisplayConfig,
  PlaybackError,
  PlaybackStatus,
} from "../types/display";
import { useHlsPlayer } from "../hooks/useHlsPlayer";

interface VideoPlayerProps {
  config: DisplayConfig;
  onPlaybackStatusChange: (status: PlaybackStatus) => void;
  onPlaybackErrorChange: (error: PlaybackError | null) => void;
}

export function VideoPlayer({
  config,
  onPlaybackStatusChange,
  onPlaybackErrorChange,
}: VideoPlayerProps) {
  const { videoRef, playbackStatus, playbackError } = useHlsPlayer({
    streamUrl: config.streamUrl,
    autoplay: config.autoplay,
  });

  useEffect(() => {
    onPlaybackStatusChange(playbackStatus);
  }, [playbackStatus, onPlaybackStatusChange]);

  useEffect(() => {
    onPlaybackErrorChange(playbackError);
  }, [playbackError, onPlaybackErrorChange]);

  return (
    <video
      ref={videoRef}
      data-testid="video-player"
      className="video-player"
      autoPlay={config.autoplay}
      muted={config.muted}
      playsInline
      controls
    />
  );
}
