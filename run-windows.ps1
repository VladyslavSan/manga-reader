$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "JDK 21 is required." }
& (Join-Path $projectRoot "gradlew.bat") -p $projectRoot --no-daemon :composeApp:run
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
