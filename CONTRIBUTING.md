# Contributing

Create a branch from `main`, make focused changes, and open a pull request.
All changes to `main` go through a PR with passing desktop build checks.
Resolve review conversations and update the branch when required before merging.

Run `./gradlew :composeApp:desktopTest` locally (Windows:
`.\gradlew.bat :composeApp:desktopTest`). The live manga-source test is opt-in
and is excluded from CI.

PR checks run tests and build installers on Windows x64 and macOS Apple Silicon.
Merges to `main` run the same checks. Neither publishes a release, but every run
attaches its installers to the **Desktop** workflow run's **Artifacts** section,
so a PR can be downloaded and run before merging. Installer artifacts are kept
for 30 days, test reports for 14. Builds include a Java runtime, so users do not
need to install Java separately.

Releases are cut by tag, not by merge. Push a `v`-prefixed tag and the **Desktop**
workflow builds it and publishes a GitHub Release with both installers
attached:

```sh
git tag v1.2.0
git push origin v1.2.0
```

The tag is the version: `v1.2.0` packages as `1.2.0`. Start at `v1.0.0` or later
— jpackage rejects a major version of `0` on macOS, and CI fails fast with that
message rather than partway through packaging. Branch and PR builds package as
`1.0.<workflow run number>`, which is never published. Local packaging defaults
to `1.0.0` and can be overridden with `-PappVersion=1.2.0`.

Installers are unsigned and macOS builds are not notarized, so the OS warns on
first launch.
