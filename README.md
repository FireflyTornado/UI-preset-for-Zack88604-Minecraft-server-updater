# FireflyTornado JavaFX UI Preset

English | [简体中文](README_CN.md)

This is a V2 `java-helper` GUI preset loaded by the Minecraft auto-update agent.
The JavaFX window runs in a separate helper JVM, preventing the Minecraft JVM
from loading `javafx.*`.

This project is based on Zack88604's
[minecraft-server-updater-for656](https://github.com/Zack88604/minecraft-server-updater-for656)
and requires that project to load and run.

## Features

- Runs JavaFX in a separate helper JVM without affecting the Minecraft process.
- With the updated core, shows network waiting, retry, and verification
  feedback above the progress bars; the speed indicator always shows a numeric speed.
  Without new bytes for two seconds it shows 0 KB/s, even if no new state arrives.
  Errors retain the failed file's actual progress and show zero speed.
  New bytes restore the ordinary file counter subtitle; resume/restart details stay in the log.
- Retains progress, speed, logs, status illustrations, animations, error guidance,
  and close-confirmation UI.
- Failed updates offer “Use trusted version”, “Retry update”, and “Exit”.
  Retry requires an updated core supporting `requestRetryUpdate()`; it fetches a
  fresh manifest while keeping installed changes and the original rollback backups.
- Embeds a pinned OpenJFX runtime verified with SHA-256 hashes.
- Retains hover and pressed feedback on the focused default
  trusted-version button in both maintenance and ordinary recovery dialogs.
- Version 1.2.3 pairs with the updated me-main core for maintenance mode. When
  all update sources report maintenance, the main window shows a short instruction
  without opening a dialog automatically. The status area's top-right “View details”
  button shows the administrator's verbatim message and opens the two-choice dialog: “Launch trusted
  version” and “Exit”. Ordinary failures retain “Get help” in that position.
  Trusted launch delegates rollback and cache verification to the core; exit
  retains installed changes without starting Minecraft, as does closing the main
  maintenance window directly. Active rounds and their
  automatic retries continue; manual retries check maintenance again.
- Maintenance uses a fixed two-line main-window instruction. The notice dialog
  fits the owner's screen and scrolls long administrator messages in full while
  keeping its illustration and both decision buttons outside the scroll area.
- Falls back to the upstream main project's Swing UI through its V2 adapter if
  the helper fails to start or exits unexpectedly.

## Building

JDK 17 or later is required. On Windows, run:

```bat
build.bat
```

The updater API contract required by the build is stored in this project's
`provided-api/` directory. It is used only for compilation and is not included
in the preset JAR.

OpenJFX dependencies are cached in this project's `lib/javafx/` directory and
downloaded automatically from Maven Central when missing. The pinned OpenJFX
21.0.4 Windows dependencies are checked against the SHA-256 hashes embedded in
the build script before they are compiled or packaged.

The final release artifact is written to:

```text
dist/fireflytornado-javafx-preset-1.2.3-win.jar
```

## Installation

Copy the artifact to the following directory under the game directory:

```text
<game-dir>/.mc-update/gui-presets/
```

Delete `<game-dir>/.mc-update/gui-selection.properties` to make the upstream
main project display the GUI selector again on the next launch. When an external
preset is selected for the first time, the main project displays a warning about
the risks of running external code.

## Platform and Runtime

- The current release target is Windows, using the OpenJFX `win` classifier.
- JavaFX is pinned to version 21.0.4, and the helper requires Java 17 or later.
- `javafx-base`, `javafx-graphics`, and `javafx-controls` are embedded.
- The updater API is a provided dependency and is not duplicated in the preset.
- Linux and macOS runtimes are not yet included in the current artifact.

## License and Attribution

This project's code and content derived from the upstream project are released
under the MIT License. See [LICENSE](LICENSE) and [NOTICE](NOTICE). Upstream
copyright notices are retained in both the source code and build artifacts.

The final preset JAR embeds OpenJFX binaries. OpenJFX is licensed under GPLv2
with the Classpath Exception; the relevant license texts are packaged under
`META-INF/licenses/openjfx/`. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for dependency and source URLs
and trademark notices.
