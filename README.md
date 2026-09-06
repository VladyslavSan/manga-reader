# Manga Reader

Manga Reader is a local-first manga reader built with Kotlin
Multiplatform and Compose Multiplatform. It currently supports `manga.in.ua`
and keeps the library, chapter registry, reading progress, covers, and manga
pages on the device.

The application talks to the manga source directly. There is no application
backend and no proxy server.

## Features

- Persistent cover gallery and multi-title library
- Remembered last chapter for each manga
- Green read markers and blue fully-offline markers
- Per-chapter, range, and "mark through here" progress controls
- Resumable background download with source-friendly throttling
- Chapter selection remains usable during background downloads
- Vertical continuous reader and horizontal pager
- Arrow, Page Up, and Page Down keyboard navigation
- Adjustable image width
- ZIP export of downloaded pages
- Atomic cache writes and restart-safe JSON state

## Run

Requirements: JDK 21 available through `JAVA_HOME` or `PATH`.

The Gradle wrapper is included and pins Gradle 9.0.0. The first run downloads
Gradle and project dependencies, so it requires internet access. A separate
Gradle installation is not needed. Run commands from the repository root.

On Windows (PowerShell):

```powershell
.\gradlew.bat :composeApp:run
```

Application data is stored outside the repository:

```text
%USERPROFILE%\.panel-relay\
  library.json
  settings.json
  covers\
  pages\
```

For development on macOS or Linux with JDK 21:

```sh
./gradlew :composeApp:run
```

Portable builds for Windows x64 and macOS Apple Silicon are published on the
[Releases page](https://github.com/VladyslavSan/manga-reader/releases/latest).
Unzip and run - there is no installer, and Java is bundled. The builds are
unsigned and not notarized, so Windows SmartScreen warns on first launch and
macOS needs the quarantine flag cleared:

```sh
xattr -dr com.apple.quarantine "Manga Reader.app"
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the PR workflow and CI details.

## Keyboard and layout

Use the **sidebar icon** or **Ctrl+B** (Windows/Linux) / **Cmd+B** (macOS) to
show or hide the sidebar. The **Settings** icon opens reading options and
keyboard help. Page width ranges from **25% to 100%** of the reading area.

Enable **Auto-hide top bars while reading** in Settings or use the toolbar icon.
Move the pointer into the top 12 dp to reveal them; moving below the bars plus
a 16 dp buffer hides them again. **Escape** or **Ctrl/Cmd+T** also reveals them
temporarily. Revealing the bars keeps auto-hide enabled, and they overlay the
reader without shifting the pages. Disable auto-hide in Settings to pin them.
Settings also opens with **Ctrl/Cmd+comma**. Reading mode, page width, sidebar visibility, and auto-hide preferences are
saved automatically in `~/.panel-relay/settings.json` and restored on startup.

When the reader has focus:

- **Page Up / Page Down** and **Shift+Space / Space** scroll by exactly one visible screen height, stopping at the content boundaries.
- In vertical mode, **Up / Down** scroll in small steps and **Left / Right** scroll by one screen.
- In horizontal mode, **arrow keys** switch images; tall images can be scrolled with the mouse or Page Up / Page Down.
- **Home / End** jump to the first / last image in the chapter.
- **Tab / Shift+Tab** move between controls; **Enter / Space** activate buttons.

The interface uses a dark palette, including the native macOS title bar.
The existing `.panel-relay` data folder is retained so existing libraries remain available.

## Tests

The default suite is deterministic and never contacts the manga website:

On Windows (PowerShell):

```powershell
.\gradlew.bat :composeApp:desktopTest
```

On macOS or Linux:

```sh
./gradlew :composeApp:desktopTest
```

The live source test is opt-in, throttled, fetches metadata only, and downloads
no manga images:

On Windows (PowerShell):

```powershell
$env:PANEL_RELAY_LIVE_TESTS = "1"
try {
    .\gradlew.bat :composeApp:desktopTest --rerun-tasks
} finally {
    Remove-Item Env:PANEL_RELAY_LIVE_TESTS
}
```

On macOS or Linux:

```sh
PANEL_RELAY_LIVE_TESTS=1 ./gradlew :composeApp:desktopTest --rerun-tasks
```

## Project layout

```text
.
├── README.md
├── build.gradle.kts             Shared plugin versions
├── settings.gradle.kts          Repositories and module declarations
├── gradle.properties            Gradle and Kotlin settings
├── gradle/wrapper/              Pinned Gradle wrapper
├── gradlew / gradlew.bat        Cross-platform build entry points
└── composeApp/
    ├── build.gradle.kts         Desktop target, dependencies and packaging
    └── src/
        ├── commonMain/kotlin/   Shared models, provider and repository
        ├── desktopMain/kotlin/  Desktop UI, HTTP, storage and ZIP export
        ├── commonTest/kotlin/   Provider and repository tests
        └── desktopTest/kotlin/  Desktop transport, persistence and UI logic tests
```

Open the repository root as the Gradle project in IntelliJ IDEA. Source sets
follow Kotlin Multiplatform conventions; add platform-specific code to its
corresponding source set. Generated build outputs, caches, and local IDE
settings are ignored by Git. The wrapper JAR belongs in version control.

The shared core is structured for future iOS support. Producing an iOS build
still requires Xcode on macOS; AltStore can be used for personal installation.
