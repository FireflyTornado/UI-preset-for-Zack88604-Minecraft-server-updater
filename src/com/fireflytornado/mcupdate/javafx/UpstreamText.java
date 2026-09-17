package com.fireflytornado.mcupdate.javafx;

import com.zack88604.autoupdater.gui.api.UpdateErrorCode;

import java.util.Map;

/** Localizes controller-owned text shown outside the verbatim Details log. */
final class UpstreamText {

    private static final String UPDATE_ERROR_PREFIX = "Update error: ";
    // Controller prefixes are parsing tokens only. They are stripped before
    // presentation so outcome text is not repeated around the localized cause.
    private static final String LEGACY_SKIP_ERROR_PREFIX =
            "Unable to skip the update safely; Minecraft will not start: ";
    private static final String CURRENT_SKIP_ERROR_PREFIX =
            "Unable to skip the update safely: ";

    private static final Map<String, String> EXACT_STATUS_KEYS = Map.ofEntries(
            Map.entry("Preparing update...", "status.preparing"),
            Map.entry("Checking for updates...", "status.preparing"),
            Map.entry("Cleaning up…", "status.cleaning"),
            Map.entry("Verifying updated resources before caching...",
                    "upstream.status.verifyingUpdated"),
            Map.entry("Verifying cached resources...", "upstream.status.verifyingCached"),
            Map.entry("Verifying local files against cached manifest...",
                    "upstream.status.verifyingLocal"),
            Map.entry("Safely skipping update…", "upstream.status.stopping"),
            Map.entry("Restoring changed files…", "upstream.status.restoring"),
            Map.entry("Downloading agent update...", "status.updater.updating"),
            Map.entry("Update failed", "status.error.failed"),
            Map.entry("Already up to date, launching Minecraft...", "status.success.current")
    );

    private static final Map<String, String> EXACT_DESCRIPTION_KEYS = Map.of(
            "Removing files that are no longer needed", "status.cleaning.description",
            "Waiting for the current operation to stop",
                    "upstream.description.waitingForStop",
            "Preparing the last trusted version",
                    "upstream.description.preparingTrusted",
            "Minecraft starts only when the signed cache matches local files",
                    "upstream.description.signedCacheRequired"
    );

    private static final Map<String, String> EXACT_ERROR_KEYS = Map.ofEntries(
            Map.entry("Cannot parse manifest", "upstream.error.manifest.parse"),
            Map.entry("No Ed25519 manifest public key is configured",
                    "upstream.error.manifest.noKey"),
            Map.entry("Update server returned an unsupported signed manifest",
                    "upstream.error.manifest.unsupported"),
            Map.entry("Update server returned an incomplete signed manifest",
                    "upstream.error.manifest.incomplete"),
            Map.entry("Signed manifest key id does not match the configured key id",
                    "upstream.error.manifest.keyMismatch"),
            Map.entry("Signed manifest payload has an invalid size",
                    "upstream.error.manifest.invalidSize"),
            Map.entry("Signed manifest signature is invalid",
                    "upstream.error.manifest.invalidSignature"),
            Map.entry("Ed25519 verification requires Java 15 or later",
                    "upstream.error.manifest.javaTooOld"),
            Map.entry("Configured Ed25519 manifest public key is invalid; Java 15 or later is required",
                    "upstream.error.manifest.invalidKey"),
            Map.entry("Signed manifest has an unsupported claims format",
                    "upstream.error.manifest.unsupportedClaims"),
            Map.entry("Signed manifest is expired or has invalid timestamps",
                    "upstream.error.manifest.expired"),
            Map.entry("Signed manifest claims are incomplete",
                    "upstream.error.manifest.incompleteClaims"),
            Map.entry("Signed manifest content hash is invalid",
                    "upstream.error.manifest.invalidContentHash"),
            Map.entry("Invalid signed manifest payload",
                    "upstream.error.manifest.invalidPayloadEncoding"),
            Map.entry("Invalid signed manifest signature",
                    "upstream.error.manifest.invalidSignatureEncoding"),
            Map.entry("Cannot hash signed manifest", "upstream.error.manifest.hash"),
            Map.entry("Update server returned an invalid Ed25519 public-key descriptor",
                    "upstream.error.trust.invalidDescriptor"),
            Map.entry("The server Ed25519 public key was not accepted; Minecraft will not start",
                    "upstream.error.trust.rejected"),
            Map.entry("Cannot calculate server public-key fingerprint",
                    "upstream.error.trust.fingerprint"),
            Map.entry("Cannot confirm a first-use public key in a headless environment",
                    "upstream.error.trust.noPrompt"),
            Map.entry("Unable to show server-key confirmation",
                    "upstream.error.trust.noPrompt"),
            Map.entry("A manifest public key already exists; refusing to replace it",
                    "upstream.error.trust.existingKey"),
            Map.entry("Unable to create game configuration directory",
                    "upstream.error.trust.save"),
            Map.entry("Cached signed manifest does not contain a file list",
                    "upstream.error.cache.noFileList"),
            Map.entry("Cannot cache an incomplete signed manifest",
                    "upstream.error.cache.incomplete"),
            Map.entry("Unable to create updater cache directory",
                    "upstream.error.cache.createDirectory"),
            Map.entry("No verified signed manifest cache is available; complete an update first",
                    "upstream.error.cache.unavailable"),
            Map.entry("Signed manifest cache is too large", "upstream.error.cache.tooLarge"),
            Map.entry("Signed manifest cache has an unsupported format",
                    "upstream.error.cache.unsupported"),
            Map.entry("Signed manifest cache belongs to different update servers",
                    "upstream.error.cache.wrongServer"),
            Map.entry("All servers unreachable", "upstream.error.network.allUnreachable")
    );

    private UpstreamText() {
    }

    static String status(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String exactKey = EXACT_STATUS_KEYS.get(value);
        if (exactKey != null) {
            return Lang.text(exactKey);
        }
        if (value.startsWith("Downloading: ")) {
            return Lang.text("upstream.status.downloading", value.substring("Downloading: ".length()));
        }
        if (value.startsWith("Updated ") && value.endsWith(" file(s), launching Minecraft...")) {
            String count = between(value, "Updated ", " file(s), launching Minecraft...");
            return Lang.text("upstream.status.updated", count);
        }
        return null;
    }

    static String description(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String exactKey = EXACT_DESCRIPTION_KEYS.get(value);
        return exactKey == null ? null : Lang.text(exactKey);
    }

    static String error(String value, UpdateErrorCode errorCode) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.startsWith(UPDATE_ERROR_PREFIX)) {
            return cause(value.substring(UPDATE_ERROR_PREFIX.length()), errorCode);
        }
        if (value.startsWith(LEGACY_SKIP_ERROR_PREFIX)) {
            return cause(value.substring(LEGACY_SKIP_ERROR_PREFIX.length()), errorCode);
        }
        if (value.startsWith(CURRENT_SKIP_ERROR_PREFIX)) {
            return cause(value.substring(CURRENT_SKIP_ERROR_PREFIX.length()), errorCode);
        }
        return cause(value, errorCode);
    }

    private static String cause(String value, UpdateErrorCode errorCode) {
        String exactKey = EXACT_ERROR_KEYS.get(value);
        if (exactKey != null) {
            return Lang.text(exactKey);
        }
        String localized = prefix(value,
                "Cached manifest resource is missing or has an invalid size: ",
                "upstream.error.cache.resourceMissing");
        if (localized != null) return localized;
        localized = prefix(value, "Cached manifest resource hash does not match: ",
                "upstream.error.cache.resourceHash");
        if (localized != null) return localized;
        localized = prefix(value, "Signed manifest cache is missing ",
                "upstream.error.cache.missingField");
        if (localized != null) return localized;
        localized = prefix(value, "Invalid cached ", "upstream.error.cache.invalidField");
        if (localized != null) return localized;
        localized = prefix(value, "Cannot roll back non-file target: ",
                "upstream.error.filesystem.rollbackTarget");
        if (localized != null) return localized;
        localized = prefix(value, "Unable to recreate directory: ",
                "upstream.error.filesystem.recreateDirectory");
        if (localized != null) return localized;

        if (errorCode == UpdateErrorCode.NETWORK) {
            return Lang.text("upstream.error.network.generic");
        }
        if (errorCode == UpdateErrorCode.FILESYSTEM) {
            return Lang.text("upstream.error.filesystem.generic");
        }
        if (errorCode == UpdateErrorCode.MANIFEST_AUTHENTICATION) {
            return Lang.text("upstream.error.manifest.generic");
        }
        if (errorCode == UpdateErrorCode.CONFIGURATION) {
            return Lang.text("upstream.error.configuration.generic");
        }
        return Lang.text("upstream.error.unknown");
    }

    private static String prefix(String value, String prefix, String key) {
        return value.startsWith(prefix) ? Lang.text(key, value.substring(prefix.length())) : null;
    }

    private static String between(String value, String prefix, String suffix) {
        return value.substring(prefix.length(), value.length() - suffix.length());
    }
}
