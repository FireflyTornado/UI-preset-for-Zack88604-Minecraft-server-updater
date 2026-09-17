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
        List<String> hints = new ArrayList<>(3);
        UpdateErrorCode errorCode = state.getErrorCode();

        if (errorCode == UpdateErrorCode.NETWORK) {
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
        } else if (containsAny(context, "timeout", "timed out", "connection", "connect",
                "network", "socket", "reset by peer", "unreachable", "dns",
                "unknown host", "http", "ssl", "certificate")) {
            hints.add(Lang.text("help.network.check"));
            hints.add(Lang.text("help.network.access"));
        } else if (containsAny(context, "access denied", "permission denied", "not permitted",
                "unauthorized", "read-only", "readonly", "being used", "in use",
                "locked", "another process")) {
            hints.add(Lang.text("help.filesystem.close"));
            hints.add(Lang.text("help.filesystem.writable"));
        } else if (containsAny(context, "no space", "disk full", "insufficient space",
                "not enough space", "out of space")) {
            hints.add(Lang.text("help.disk.free"));
            hints.add(Lang.text("help.disk.retry"));
        } else if (containsAny(context, "checksum", "hash mismatch", "digest", "corrupt",
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

