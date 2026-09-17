package com.fireflytornado.mcupdate.javafx;

import com.zack88604.autoupdater.gui.api.UpdateErrorCode;
import com.zack88604.autoupdater.gui.api.UpdateUiState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns display-safe error context into a short list of actionable hints. */
final class ErrorHintResolver {

    private ErrorHintResolver() {
    }

    static List<String> resolve(UpdateUiState state) {
        String context = buildContext(state).toLowerCase(Locale.ROOT);
        boolean hasExplicitCause = state.getErrorMessage() != null
                && !state.getErrorMessage().isBlank();
        String causeContext = hasExplicitCause
                ? state.getErrorMessage().toLowerCase(Locale.ROOT) : context;
        List<String> hints = new ArrayList<>(3);
        UpdateErrorCode errorCode = state.getErrorCode();

        // Prefer the concrete upstream cause over the coarse error code. This is
        // especially important after a failed trusted-version recovery: the
        // controller currently classifies every such failure as FILESYSTEM even
        // when the real cause is a missing cache or an authentication failure.
        if (containsAny(causeContext, "expired or has invalid timestamps")) {
            add(hints, "help.authentication.clock", "help.authentication.renew");
        } else if (containsAny(causeContext,
                "key id does not match", "signature is invalid",
                "content hash is invalid")) {
            add(hints, "help.authentication.warning", "help.authentication.admin");
        } else if (containsAny(causeContext, "no ed25519 manifest public key is configured")) {
            add(hints, "help.key.check", "help.key.admin");
        } else if (containsAny(causeContext, "ed25519 verification requires java")) {
            add(hints, "help.java.upgrade", "help.key.admin");
        } else if (containsAny(causeContext, "configured ed25519 manifest public key is invalid")) {
            add(hints, "help.java.key", "help.key.admin");
        } else if (containsAny(causeContext,
                "cannot parse manifest", "unsupported signed manifest",
                "incomplete signed manifest", "payload has an invalid size",
                "unsupported claims format", "claims are incomplete",
                "invalid signed manifest payload", "invalid signed manifest signature",
                "cannot hash signed manifest")) {
            add(hints, "help.manifest.format", "help.manifest.regenerate");
        } else if (containsAny(causeContext, "public key was not accepted")) {
            add(hints, "help.trust.verifyFingerprint", "help.trust.retry");
        } else if (containsAny(causeContext,
                "invalid ed25519 public-key descriptor", "public-key fingerprint",
                "first-use public key", "server-key confirmation",
                "manifest public key already exists", "game configuration directory")) {
            add(hints, "help.trust.environment", "help.trust.configuration");
        } else if (containsAny(causeContext,
                "no verified signed manifest cache is available")) {
            add(hints, "help.cache.unavailable", "help.cache.rebuild");
        } else if (containsAny(causeContext,
                "cache belongs to different update servers")) {
            add(hints, "help.cache.serverMismatch", "help.cache.restoreServer");
        } else if (containsAny(causeContext,
                "cache is too large", "cache has an unsupported format",
                "cache is missing", "invalid cached",
                "cached signed manifest does not contain a file list")) {
            add(hints, "help.cache.invalid", "help.cache.noEdit");
        } else if (containsAny(causeContext,
                "cached manifest resource is missing or has an invalid size")) {
            add(hints, "help.cache.localMissing", "help.cache.rebuild");
        } else if (containsAny(causeContext,
                "cached manifest resource hash does not match")) {
            add(hints, "help.cache.localMismatch", "help.cache.rebuild");
        } else if (containsAny(causeContext,
                "cannot roll back non-file target", "unable to recreate directory")) {
            add(hints, "help.rollback.close", "help.rollback.permissions");
        } else if (containsAny(causeContext, "no space", "disk full", "insufficient space",
                "not enough space", "out of space")) {
            add(hints, "help.disk.free", "help.disk.retry");
        } else if (!hasExplicitCause
                && containsAny(context, "unsafe manifest path", "rejected unsafe path")) {
            add(hints, "help.manifest.pathRejected", "help.manifest.pathAdmin");
        } else if (!hasExplicitCause && containsAny(context, "hash mismatch after download")) {
            add(hints, "help.integrity.retry", "help.integrity.cache");
        } else if (!hasExplicitCause
                && containsAny(context, ": download failed", "download failed from:")) {
            add(hints, "help.network.check", "help.download.server");
        } else if (!hasExplicitCause && containsAny(context, "cannot replace file")) {
            add(hints, "help.filesystem.close", "help.filesystem.writable");
        } else if (errorCode == UpdateErrorCode.NETWORK) {
            hints.add(Lang.text("help.network.check"));
            hints.add(Lang.text("help.network.access"));
        } else if (errorCode == UpdateErrorCode.MANIFEST_AUTHENTICATION) {
            hints.add(Lang.text("help.authentication.warning"));
            hints.add(Lang.text("help.authentication.admin"));
        } else if (errorCode == UpdateErrorCode.CONFIGURATION) {
            hints.add(Lang.text("help.configuration.check"));
            hints.add(Lang.text("help.configuration.admin"));
        } else if (errorCode == UpdateErrorCode.FILESYSTEM) {
            hints.add(Lang.text("help.filesystem.close"));
            hints.add(Lang.text("help.filesystem.writableSpace"));
        } else if (containsAny(causeContext, "timeout", "timed out", "connection", "connect",
                "network", "socket", "reset by peer", "unreachable", "dns",
                "unknown host", "http", "ssl", "certificate")) {
            hints.add(Lang.text("help.network.check"));
            hints.add(Lang.text("help.network.access"));
        } else if (containsAny(causeContext, "access denied", "permission denied", "not permitted",
                "unauthorized", "read-only", "readonly", "being used", "in use",
                "locked", "another process")) {
            hints.add(Lang.text("help.filesystem.close"));
            hints.add(Lang.text("help.filesystem.writable"));
        } else if (containsAny(causeContext, "checksum", "hash mismatch", "digest", "corrupt",
                "integrity", "unexpected size")) {
            hints.add(Lang.text("help.integrity.retry"));
            hints.add(Lang.text("help.integrity.cache"));
        } else {
            hints.add(Lang.text("help.generic.retry"));
            hints.add(Lang.text("help.generic.details"));
        }

        hints.add(Lang.text("help.support"));
        return List.copyOf(hints.subList(0, Math.min(3, hints.size())));
    }

    private static void add(List<String> hints, String firstKey, String secondKey) {
        hints.add(Lang.text(firstKey));
        hints.add(Lang.text(secondKey));
    }

    private static String buildContext(UpdateUiState state) {
        StringBuilder text = new StringBuilder();
        append(text, state.getErrorMessage());
        append(text, state.getStatus());
        append(text, state.getDescription());
        for (String line : state.getLogLines()) {
            append(text, line);
        }
        return text.toString();
    }

    private static void append(StringBuilder target, String value) {
        if (value != null && !value.isBlank()) {
            target.append(' ').append(value);
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
