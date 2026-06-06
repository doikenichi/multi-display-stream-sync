import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import type { PlaybackError, PlaybackStatus } from "../types/display";

interface UseHlsPlayerOptions {
  streamUrl: string;
  autoplay: boolean;
}

export function useHlsPlayer({ streamUrl, autoplay }: UseHlsPlayerOptions) {
  // This gives React a way to access the real browser <video> element.
  // later we connect it like <video ref={videoRef} />
  const videoRef = useRef<HTMLVideoElement | null>(null);
  // These state variables track the current playback status and any errors that occur.
  const [playbackStatus, setPlaybackStatus] =
    useState<PlaybackStatus>("INITIALIZING");
  const [playbackError, setPlaybackError] = useState<PlaybackError | null>(
    null,
  );

  // When the stream URL changes, run the player setup logic.
  useEffect(() => {
    const videoElement = videoRef.current;

    if (!videoElement) {
      return;
    }

    setPlaybackStatus("LOADING_STREAM");
    setPlaybackError(null);

    if (Hls.isSupported()) {
      const hls = new Hls();

      hls.loadSource(streamUrl);
      hls.attachMedia(videoElement);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        setPlaybackStatus("MANIFEST_LOADED");

        if (autoplay) {
          videoElement
            .play()
            .then(() => {
              setPlaybackStatus("PLAYING");
            })
            .catch((error: unknown) => {
              setPlaybackStatus("ERROR");
              setPlaybackError({
                type: "PLAYBACK_ERROR",
                fatal: false,
                message:
                  error instanceof Error
                    ? error.message
                    : "Browser blocked or failed playback.",
              });
            });
        }
      });

      hls.on(Hls.Events.ERROR, (_event, data) => {
        setPlaybackStatus("ERROR");
        setPlaybackError({
          type: data.type,
          details: data.details,
          fatal: data.fatal,
          message: "HLS playback error.",
        });
      });

      return () => {
        hls.destroy();
      };
    }

    if (videoElement.canPlayType("application/vnd.apple.mpegurl")) {
      videoElement.src = streamUrl;

      videoElement.addEventListener("loadedmetadata", () => {
        setPlaybackStatus("MANIFEST_LOADED");

        if (autoplay) {
          void videoElement.play();
        }
      });

      return;
    }

    setPlaybackStatus("UNSUPPORTED");
    setPlaybackError({
      type: "UNSUPPORTED",
      fatal: true,
      message: "This browser does not support HLS playback.",
    });
  }, [streamUrl, autoplay]);

  return {
    videoRef,
    playbackStatus,
    playbackError,
  };
}
