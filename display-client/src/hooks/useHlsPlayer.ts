import { useEffect, useRef, useState } from "react";
import shaka from "shaka-player";
import type {
  PlaybackError,
  PlaybackMode,
  PlaybackStatus,
  VideoDiagnostics,
} from "../types/display";

interface UseHlsPlayerOptions {
  streamUrl?: string;
  autoplay: boolean;
}

interface PlayerState {
  streamUrl: string;
  status: PlaybackStatus;
  error: PlaybackError | null;
  playbackMode: PlaybackMode;
}

type ShakaErrorEvent = Event & {
  detail: shaka.util.Error;
};

function mapShakaError(error: shaka.util.Error): PlaybackError {
  return {
    type: getShakaErrorType(error),
    details: [
      `category=${error.category}`,
      `code=${error.code}`,
      `data=${JSON.stringify(error.data)}`,
    ].join(", "),
    fatal: error.severity === shaka.util.Error.Severity.CRITICAL,
    message: getShakaErrorMessage(error),
  };
}

function getShakaErrorType(error: shaka.util.Error): string {
  switch (error.category) {
    case shaka.util.Error.Category.NETWORK:
      return "NETWORK_ERROR";

    case shaka.util.Error.Category.MANIFEST:
      return "MANIFEST_ERROR";

    case shaka.util.Error.Category.MEDIA:
      return "MEDIA_ERROR";

    default:
      return "PLAYBACK_ERROR";
  }
}

function getShakaErrorMessage(error: shaka.util.Error): string {
  switch (error.code) {
    case shaka.util.Error.Code.BAD_HTTP_STATUS:
      return "Unable to load the video stream.";

    default:
      return "Video playback failed.";
  }
}

function mapUnknownError(
  error: unknown,
  fallbackMessage: string,
): PlaybackError {
  return {
    type: "PLAYBACK_ERROR",
    fatal: false,
    message: error instanceof Error ? error.message : fallbackMessage,
  };
}

// Install Shaka browser compatibility polyfills once when this module loads.
if (typeof window !== "undefined") {
  shaka.polyfill.installAll();
}

export function useHlsPlayer({ streamUrl, autoplay }: UseHlsPlayerOptions) {
  // This gives React a way to access the real browser <video> element.
  // later we connect it like <video ref={videoRef} />
  const videoRef = useRef<HTMLVideoElement | null>(null);
  // These state variables track the current playback status and any errors that occur.
  const [playerState, setPlayerState] = useState<PlayerState | null>(null);

  const [videoDiagnostics, setVideoDiagnostics] = useState<VideoDiagnostics>({
    readyState: 0,
    currentTime: 0,
    videoWidth: 0,
    videoHeight: 0,
  });

  // When the stream URL changes, run the player setup logic.
  useEffect(() => {
    const videoElement = videoRef.current;

    if (!videoElement) {
      return;
    }

    let disposed = false;
    let activePlaybackMode: PlaybackMode = "unsupported";

    const updateVideoDiagnostics = () => {
      setVideoDiagnostics({
        readyState: videoElement.readyState,
        currentTime: videoElement.currentTime,
        videoWidth: videoElement.videoWidth,
        videoHeight: videoElement.videoHeight,
      });
    };

    if (!streamUrl) {
      videoElement.pause();
      videoElement.removeAttribute("src");
      videoElement.load();
      return;
    }

    const currentStreamUrl = streamUrl;

    const setStatus = (status: PlaybackStatus) => {
      setPlayerState({
        streamUrl: currentStreamUrl,
        status,
        error: null,
        playbackMode: activePlaybackMode,
      });
    };

    const setError = (error: PlaybackError) => {
      setPlayerState({
        streamUrl: currentStreamUrl,
        status: "ERROR",
        error,
        playbackMode: activePlaybackMode,
      });
    };

    const playIfRequested = async () => {
      if (!autoplay) {
        return;
      }

      try {
        await videoElement.play();
      } catch (error: unknown) {
        setError(mapUnknownError(error, "Browser blocked or failed playback."));
      }
    };

    const handlePlaying = () => {
      setStatus("PLAYING");
    };

    const handleWaiting = () => {
      setStatus("BUFFERING");
    };

    const handlePause = () => {
      setStatus("PAUSED");
    };

    const addVideoEventListeners = () => {
      videoElement.addEventListener("playing", handlePlaying);
      videoElement.addEventListener("waiting", handleWaiting);
      videoElement.addEventListener("pause", handlePause);
      videoElement.addEventListener("loadedmetadata", updateVideoDiagnostics);
      videoElement.addEventListener("timeupdate", updateVideoDiagnostics);
      videoElement.addEventListener("resize", updateVideoDiagnostics);
    };

    const removeVideoEventListeners = () => {
      videoElement.removeEventListener("playing", handlePlaying);
      videoElement.removeEventListener("waiting", handleWaiting);
      videoElement.removeEventListener("pause", handlePause);
      videoElement.removeEventListener(
        "loadedmetadata",
        updateVideoDiagnostics,
      );
      videoElement.removeEventListener("timeupdate", updateVideoDiagnostics);
      videoElement.removeEventListener("resize", updateVideoDiagnostics);
    };

    addVideoEventListeners();

    /*
     * Primary playback engine.
     */
    if (shaka.Player.isBrowserSupported()) {
      activePlaybackMode = "shaka";
      setStatus("LOADING_STREAM");
      const player = new shaka.Player();

      const handleShakaError = (event: Event) => {
        const error = (event as ShakaErrorEvent).detail;
        setError(mapShakaError(error));
      };

      player.addEventListener("error", handleShakaError);

      const initializePlayer = async () => {
        try {
          await player.attach(videoElement);
          await player.load(currentStreamUrl);

          if (disposed) {
            return;
          }

          setStatus("MANIFEST_LOADED");
          await playIfRequested();
        } catch (error: unknown) {
          if (disposed) {
            return;
          }

          /*
           * React StrictMode and normal cleanup can interrupt an
           * asynchronous Shaka operation. That is not a playback failure.
           */
          if (
            error instanceof shaka.util.Error &&
            error.code === shaka.util.Error.Code.OPERATION_ABORTED
          ) {
            return;
          }

          setError(
            error instanceof shaka.util.Error
              ? mapShakaError(error)
              : mapUnknownError(error, "Unable to load the HLS stream."),
          );
        }
      };

      void initializePlayer();

      return () => {
        disposed = true;

        player.removeEventListener("error", handleShakaError);

        removeVideoEventListeners();

        /*
         * destroy() releases networking, MediaSource resources,
         * listeners, and the attached video element.
         */
        void player.destroy();
      };
    }

    /*
     * Native browser fallback, mainly useful for browsers with direct
     * HLS support.
     */
    if (videoElement.canPlayType("application/vnd.apple.mpegurl")) {
      activePlaybackMode = "native-hls";
      setStatus("LOADING_STREAM");
      videoElement.src = currentStreamUrl;

      const handleLoadedMetadata = () => {
        setStatus("MANIFEST_LOADED");
        void playIfRequested();
      };

      videoElement.addEventListener("loadedmetadata", handleLoadedMetadata);

      videoElement.src = currentStreamUrl;

      return () => {
        disposed = true;

        videoElement.removeEventListener(
          "loadedmetadata",
          handleLoadedMetadata,
        );

        removeVideoEventListeners();

        videoElement.pause();
        videoElement.removeAttribute("src");
        videoElement.load();
      };
    }

    queueMicrotask(() => {
      setError({
        type: "UNSUPPORTED",
        fatal: true,
        message: "This browser does not support HLS playback.",
      });
    });

    return () => {
      disposed = true;
      removeVideoEventListeners();
    };
  }, [streamUrl, autoplay]);

  const hasCurrentState =
    streamUrl !== undefined && playerState?.streamUrl === streamUrl;

  const playbackStatus: PlaybackStatus = !streamUrl
    ? "INITIALIZING"
    : hasCurrentState
      ? playerState.status
      : "LOADING_STREAM";

  const playbackError: PlaybackError | null = hasCurrentState
    ? playerState.error
    : null;

  const playbackMode: PlaybackMode = !streamUrl
    ? "none"
    : hasCurrentState
      ? playerState.playbackMode
      : "none";

  return {
    videoRef,
    playbackStatus,
    playbackError,
    playbackMode,
    videoDiagnostics,
  };
}
