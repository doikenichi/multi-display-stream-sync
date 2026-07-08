package com.streaminglab.testframework.model;

/** Immutable browser playback observation captured by DisplayClientDriver. */
public record PlaybackObservation(
    boolean videoLoaded,
    int videoWidth,
    int videoHeight,
    double initialCurrentTime,
    double finalCurrentTime,
    boolean progressed) {}
