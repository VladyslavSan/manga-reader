param([switch]$LiveSource)
$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "JDK 21 is required." }
if ($LiveSource) { $env:PANEL_RELAY_LIVE_TESTS = "1" }
$arguments = @("-p", $projectRoot, "--no-daemon", ":composeApp:desktopTest")
if ($LiveSource) { $arguments += "--rerun-tasks" }
& (Join-Path $projectRoot "gradlew.bat") @arguments
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
