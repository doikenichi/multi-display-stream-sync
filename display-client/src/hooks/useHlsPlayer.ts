import {useEffect, useRef, useState} from "react";
import Hls from "hls.js";
import type {PlaybackError, PlaybackStatus} from "../types/display";

interface UseHlsPlayerOptions {
    streamUrl?: string;
    autoplay: boolean;
}

interface PlayerState {
    streamUrl: string;
    status: PlaybackStatus;
    error: PlaybackError | null;
}

export function useHlsPlayer({streamUrl, autoplay}: UseHlsPlayerOptions) {
    // This gives React a way to access the real browser <video> element.
    // later we connect it like <video ref={videoRef} />
    const videoRef = useRef<HTMLVideoElement | null>(null);
    // These state variables track the current playback status and any errors that occur.
    const [playerState, setPlayerState] = useState<PlayerState | null>(null);

    // When the stream URL changes, run the player setup logic.
    useEffect(() => {
        const videoElement = videoRef.current;

        if (!videoElement) {
            return;
        }

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
            });
        };

        const setError = (error: PlaybackError) => {
            setPlayerState({
                streamUrl: currentStreamUrl,
                status: "ERROR",
                error,
            });
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

        videoElement.addEventListener("playing", handlePlaying);
        videoElement.addEventListener("waiting", handleWaiting);
        videoElement.addEventListener("pause", handlePause);


        if (Hls.isSupported()) {
            const hls = new Hls();

            hls.attachMedia(videoElement);
            hls.loadSource(currentStreamUrl);

            hls.on(Hls.Events.MANIFEST_PARSED, () => {
                setStatus("MANIFEST_LOADED");

                if (autoplay) {
                    videoElement.play().catch((error: unknown) => {
                        setError({
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
                setError({
                    type: data.type,
                    details: data.details,
                    fatal: data.fatal,
                    message: "HLS playback error.",
                });
            });

            return () => {
                hls.destroy();

                videoElement.removeEventListener("playing", handlePlaying);
                videoElement.removeEventListener("waiting", handleWaiting);
                videoElement.removeEventListener("pause", handlePause);
            };
        }

        if (videoElement.canPlayType("application/vnd.apple.mpegurl")) {
            videoElement.src = currentStreamUrl;

            const handleLoadedMetadata = () => {
                setStatus("MANIFEST_LOADED");

                if (autoplay) {
                    videoElement.play().catch((error: unknown) => {
                        setError({
                            type: "PLAYBACK_ERROR",
                            fatal: false,
                            message:
                                error instanceof Error
                                    ? error.message
                                    : "Browser blocked or failed playback.",
                        });
                    });
                }
            };

            videoElement.addEventListener("loadedmetadata", handleLoadedMetadata);

            return () => {
                videoElement.removeEventListener("loadedmetadata", handleLoadedMetadata);
                videoElement.removeEventListener("playing", handlePlaying);
                videoElement.removeEventListener("waiting", handleWaiting);
                videoElement.removeEventListener("pause", handlePause);
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
            videoElement.removeEventListener("playing", handlePlaying);
            videoElement.removeEventListener("waiting", handleWaiting);
            videoElement.removeEventListener("pause", handlePause);
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

    return {
        videoRef,
        playbackStatus,
        playbackError,
    };
}
