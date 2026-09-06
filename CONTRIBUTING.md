# Contributing

Create a branch from `main`, make focused changes, and open a pull request.
All changes to `main` go through a PR with passing desktop build checks.
Resolve review conversations and update the branch when required before merging.

Run `./gradlew :composeApp:desktopTest` locally (Windows:
`.\gradlew.bat :composeApp:desktopTest`). The live manga-source test is opt-in
and is excluded from CI.

PR checks run tests and build native installers on Windows x64, macOS Apple
Silicon, and macOS Intel. Each merge to `main` repeats these checks and uploads
installers to the **Desktop** workflow run's **Artifacts** section. Installer
artifacts are retained for 30 days; test reports for 14 days. Builds include a
Java runtime, so users do not need to install Java separately.

Installers are currently unsigned and macOS builds are not notarized. CI does
not publish GitHub Releases. To retain a build beyond artifact expiration,
download it before it expires.
