# Contributing

Create a branch from `main`, make focused changes, and open a pull request.
All changes to `main` go through a PR with passing desktop build checks.
Resolve review conversations and update the branch when required before merging.

Run `./gradlew :composeApp:desktopTest` locally (Windows:
`.\gradlew.bat :composeApp:desktopTest`). The live manga-source test is opt-in
and is excluded from CI.

PR checks run tests and build the app for Windows x64 and macOS Apple Silicon.
Merges to `main` run the same checks. Neither publishes a release, but every run
attaches its builds to the **Desktop** workflow run's **Artifacts** section, so a
PR can be downloaded and run before merging. Build artifacts are kept for 30
days, test reports for 14.

CI ships a portable app image rather than an installer: `createDistributable`
produces an app directory that is zipped and published as-is, so users unzip and
run it. A Java runtime is bundled, so Java need not be installed. The zipping is
done on the build runner, because artifact upload drops the executable bit and
follows symlinks, either of which breaks a macOS `.app` bundle. The `packageDmg`
and `packageExe` tasks still work locally if an installer is ever wanted.

Releases are cut by tag, not by merge. Push a `v`-prefixed tag and the **Desktop**
workflow builds it and publishes a GitHub Release with a portable zip per
platform attached:

```sh
git tag v1.2.0
git push origin v1.2.0
```

The tag is the version: `v1.2.0` packages as `1.2.0`. Start at `v1.0.0` or later
— jpackage rejects a major version of `0` on macOS, and CI fails fast with that
message rather than partway through packaging. Branch and PR builds package as
`1.0.<workflow run number>`, which is never published. Local packaging defaults
to `1.0.0` and can be overridden with `-PappVersion=1.2.0`.

Builds are unsigned and macOS builds are not notarized. Windows SmartScreen
warns on first launch, and macOS reports the app as damaged until the quarantine
flag is cleared with `xattr -dr com.apple.quarantine "Manga Reader.app"`. The
release notes repeat both workarounds for users.
