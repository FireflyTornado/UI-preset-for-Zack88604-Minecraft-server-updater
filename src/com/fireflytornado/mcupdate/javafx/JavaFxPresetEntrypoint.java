package com.fireflytornado.mcupdate.javafx;

import com.zack88604.autoupdater.gui.api.JavaHelperCommand;
import com.zack88604.autoupdater.gui.api.JavaHelperEntrypoint;
import com.zack88604.autoupdater.gui.api.JavaHelperSession;
import javafx.application.Platform;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Runs the FireflyTornado JavaFX preset inside the updater's helper JVM. */
public final class JavaFxPresetEntrypoint implements JavaHelperEntrypoint {

    private static final long FX_SHUTDOWN_TIMEOUT_SECONDS = 5;

    /** Public no-argument constructor required by {@code JavaHelperLauncher}. */
    public JavaFxPresetEntrypoint() {
    }

    @Override
    public void run(JavaHelperSession session) throws Exception {
        Objects.requireNonNull(session, "session");
        AtomicReference<JavaFxUpdateView> viewRef = new AtomicReference<>();
        AtomicReference<Throwable> startupFailure = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);

        Platform.startup(() -> {
            try {
                JavaFxViewListener listener = new SessionViewListener(session);
                JavaFxUpdateView view = new JavaFxUpdateView(listener,
                        session.getContext().isDebug(),
                        session.getContext().getGameDirectory());
                viewRef.set(view);
            } catch (Throwable failure) {
                startupFailure.set(failure);
            } finally {
                started.countDown();
            }
        });

        started.await();
        Throwable failure = startupFailure.get();
        if (failure != null) {
            throw new IllegalStateException("Unable to initialize JavaFX preset", failure);
        }
        JavaFxUpdateView view = Objects.requireNonNull(viewRef.get(), "JavaFX view");
        LatestFxStateRenderer stateRenderer = new LatestFxStateRenderer(view);
        session.signalReady();

        JavaHelperCommand command;
        while ((command = session.nextCommand()) != null) {
            switch (command.getType()) {
                case OPEN:
                    Platform.runLater(view::open);
                    break;
                case RENDER:
                    stateRenderer.submit(command.getState());
                    break;
                case CLOSE:
                    closeAndExit(view, stateRenderer);
                    return;
                default:
                    break;
            }
        }
        closeAndExit(view, stateRenderer);
    }

    /**
     * JavaFX queues tasks in submission order. Waiting for this final close
     * therefore also guarantees that all preceding terminal renders ran first.
     */
    private static void closeAndExit(JavaFxUpdateView view,
                                     LatestFxStateRenderer stateRenderer) {
        stateRenderer.stopAccepting();
        CountDownLatch closed = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                // A render task may have been executing while CLOSE arrived and
                // scheduled a newer snapshot behind this task. Pull that latest
                // snapshot forward so the terminal UI state is never skipped.
                stateRenderer.renderLatestNow();
                view.close();
            } finally {
                closed.countDown();
            }
        });
        try {
            closed.await(FX_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            Platform.exit();
        }
    }

    /**
     * Keeps at most one JavaFX render task queued while always applying the most
     * recent complete state. The updater already rate-limits snapshots, but this
     * second boundary prevents a temporarily slow JavaFX pulse/layout pass from
     * accumulating stale progress frames in the helper process.
     */
    private static final class LatestFxStateRenderer {
        private final JavaFxUpdateView view;
        private final AtomicReference<com.zack88604.autoupdater.gui.api.UpdateUiState>
                latest = new AtomicReference<>();
        private final AtomicBoolean scheduled = new AtomicBoolean();
        private final AtomicBoolean accepting = new AtomicBoolean(true);

        private LatestFxStateRenderer(JavaFxUpdateView view) {
            this.view = Objects.requireNonNull(view, "view");
        }

        private void submit(com.zack88604.autoupdater.gui.api.UpdateUiState state) {
            if (!accepting.get()) {
                return;
            }
            latest.set(Objects.requireNonNull(state, "state"));
            scheduleIfNeeded();
        }

        private void scheduleIfNeeded() {
            if (scheduled.compareAndSet(false, true)) {
                Platform.runLater(this::drainLatest);
            }
        }

        private void drainLatest() {
            try {
                renderLatestNow();
            } finally {
                scheduled.set(false);
                if (latest.get() != null && accepting.get()) {
                    scheduleIfNeeded();
                }
            }
        }

        private void renderLatestNow() {
            com.zack88604.autoupdater.gui.api.UpdateUiState state =
                    latest.getAndSet(null);
            if (state != null) {
                view.render(state);
            }
        }

        private void stopAccepting() {
            accepting.set(false);
        }
    }

    /** Maps view-only user intent onto the updater-owned helper protocol. */
    private static final class SessionViewListener implements JavaFxViewListener {
        private final JavaHelperSession session;

        private SessionViewListener(JavaHelperSession session) {
            this.session = session;
        }

        @Override
        public void userRequestedClose() {
            session.requestClose();
        }

        @Override
        public void userRequestedSkipUpdate() {
            session.requestSkipUpdate();
        }

        @Override
        public void windowClosed() {
            session.notifyWindowClosed();
        }

        @Override
        public void beginCloseConfirmation() {
            session.beginCloseConfirmation();
        }

        @Override
        public void cancelCloseConfirmation() {
            session.cancelCloseConfirmation();
        }
    }
}
