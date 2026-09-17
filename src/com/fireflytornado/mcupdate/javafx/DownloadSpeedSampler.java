package com.fireflytornado.mcupdate.javafx;

import com.zack88604.autoupdater.gui.api.DownloadProgress;

import java.util.Objects;

/**
 * Computes a UI-side, session-wide download speed from cumulative byte counts.
 *
 * <p>The updater's speed field is intentionally ignored: it is an instantaneous
 * value measured around individual input-buffer reads. This sampler instead
 * accumulates bytes across render snapshots and across consecutive files, then
 * exposes an average over a useful wall-clock interval.</p>
 */
final class DownloadSpeedSampler {

    static final long SAMPLE_INTERVAL_NANOS = 500_000_000L;
    static final long INITIAL_SAMPLE_INTERVAL_NANOS = 100_000_000L;

    private String path;
    private DownloadProgress.Kind kind;
    private long downloadedBytes;
    private long totalBytes;
    private long windowStartedAtNanos = Long.MIN_VALUE;
    private long bytesInWindow;
    private int observationsInWindow;
    private double displayedBytesPerSecond;
    private boolean hasDisplayedSample;

    double update(DownloadProgress progress, long nowNanos) {
        Objects.requireNonNull(progress, "progress");
        if (!progress.isActive()) {
            return displayedBytesPerSecond;
        }

        String nextPath = progress.getPath();
        DownloadProgress.Kind nextKind = progress.getKind();
        long nextDownloaded = progress.getDownloadedBytes();
        long nextTotal = progress.getTotalBytes();

        if (windowStartedAtNanos == Long.MIN_VALUE || kind != nextKind) {
            beginSession(nextPath, nextKind, nextDownloaded, nextTotal, nowNanos);
            return displayedBytesPerSecond;
        }

        long delta;
        if (Objects.equals(path, nextPath)) {
            // A decreasing counter means the producer restarted this resource.
            // Establish a new baseline instead of manufacturing a huge delta.
            delta = nextDownloaded >= downloadedBytes
                    ? nextDownloaded - downloadedBytes : 0L;
        } else {
            // The UI may never render the final snapshot of a small file because
            // upstream coalesces states. Account for that unobserved tail when a
            // following file proves that the previous one completed.
            long previousTail = totalBytes > 0L && downloadedBytes <= totalBytes
                    ? totalBytes - downloadedBytes : 0L;
            delta = saturatedAdd(previousTail, nextDownloaded);
        }

        bytesInWindow = saturatedAdd(bytesInWindow, Math.max(0L, delta));
        observationsInWindow++;
        path = nextPath;
        kind = nextKind;
        downloadedBytes = nextDownloaded;
        totalBytes = nextTotal;

        long elapsed = nowNanos - windowStartedAtNanos;
        long requiredInterval = hasDisplayedSample
                ? SAMPLE_INTERVAL_NANOS : INITIAL_SAMPLE_INTERVAL_NANOS;
        if (elapsed >= requiredInterval && observationsInWindow >= 2) {
            displayedBytesPerSecond = bytesInWindow * 1_000_000_000.0 / elapsed;
            hasDisplayedSample = true;
            windowStartedAtNanos = nowNanos;
            bytesInWindow = 0L;
            // The snapshot that closed this window is also the byte baseline
            // for the next one, so only one later snapshot is required.
            observationsInWindow = 1;
        }
        return displayedBytesPerSecond;
    }

    double getDisplayedBytesPerSecond() {
        return displayedBytesPerSecond;
    }

    void reset() {
        path = null;
        kind = null;
        downloadedBytes = 0L;
        totalBytes = 0L;
        windowStartedAtNanos = Long.MIN_VALUE;
        bytesInWindow = 0L;
        observationsInWindow = 0;
        displayedBytesPerSecond = 0.0;
        hasDisplayedSample = false;
    }

    private void beginSession(String nextPath, DownloadProgress.Kind nextKind,
                              long nextDownloaded, long nextTotal, long nowNanos) {
        reset();
        path = nextPath;
        kind = nextKind;
        downloadedBytes = nextDownloaded;
        totalBytes = nextTotal;
        windowStartedAtNanos = nowNanos;
        observationsInWindow = 1;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
