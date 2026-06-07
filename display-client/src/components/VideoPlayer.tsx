import type { RefObject } from "react";
import type { DisplayConfig } from "../types/display";

interface VideoPlayerProps {
  config: DisplayConfig;
  videoRef: RefObject<HTMLVideoElement | null>;
}

export function VideoPlayer({ config, videoRef }: VideoPlayerProps) {
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
