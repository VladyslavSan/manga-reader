# Contributing

Create a branch from `main`, make focused changes, and open a pull request.
All changes to `main` go through a PR with passing desktop build checks.
Resolve review conversations and update the branch when required before merging.

Run `./gradlew :composeApp:desktopTest` locally (Windows:
`.\gradlew.bat :composeApp:desktopTest`). The live manga-source test is opt-in
and is excluded from CI.

PR checks run tests and build native installers on Windows x64 and macOS Apple
Silicon. Each merge to `main` repeats these checks and then publishes a GitHub
Release tagged `v1.0.<workflow run number>` with all three installers attached.
The same installers also land in the **Desktop** workflow run's **Artifacts**
section, retained for 30 days; test reports for 14 days. Builds include a Java
runtime, so users do not need to install Java separately.

Versions come from the workflow run number, so every merge to `main` releases a
strictly higher version with no manual bump. Local packaging defaults to `1.0.0`
and can be overridden with `-PappVersion=1.0.123`.

Installers are unsigned and macOS builds are not notarized, so the OS warns on
first launch.
