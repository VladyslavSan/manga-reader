# Panel Relay

Panel Relay is a Windows-first, local-first manga reader built with Kotlin
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

## Run on Windows

Requirements: JDK 21 available through `JAVA_HOME` or `PATH`.

The Gradle wrapper is included and pins Gradle 9.0.0. The first run downloads
Gradle and project dependencies, so it requires internet access. A separate
Gradle installation is not needed. Run commands from the repository root.

```powershell
.\run-windows.ps1
```

Application data is stored outside the repository:

```text
%USERPROFILE%\.panel-relay\
  library.json
  covers\
  pages\
```

For development on macOS or Linux with JDK 21:

```sh
./gradlew :composeApp:run
```

Native installers are currently configured for Windows only.

## Tests

The default suite is deterministic and never contacts the manga website:

```powershell
.\test.ps1
```

On macOS or Linux:

```sh
./gradlew :composeApp:desktopTest
```

The live source test is opt-in, throttled, fetches metadata only, and downloads
no manga images:

```powershell
.\test.ps1 -LiveSource
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
├── run-windows.ps1              Windows run helper
├── test.ps1                     Windows test helper
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
