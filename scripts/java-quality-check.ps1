$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$modules = @(
    "streaming-lab-orchestration-api",
    "test-framework/java"
)

foreach ($module in $modules) {
    $modulePath = Join-Path $repositoryRoot $module
    Write-Host "Running Java quality gate in $module"

    Push-Location $modulePath
    try {
        & .\gradlew.bat clean qualityCheck
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }
    finally {
        Pop-Location
    }
}
