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
- Retains progress, speed, logs, status illustrations, animations, error guidance,
  and close-confirmation UI.
- Supports upstream GUI protocol actions such as closing, skipping an update,
  and using the last trusted version.
- Embeds a pinned OpenJFX runtime verified with SHA-256 hashes.
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
dist/fireflytornado-javafx-preset-1.1.2-win.jar
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
