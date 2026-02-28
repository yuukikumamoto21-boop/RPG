param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5500
)

$ErrorActionPreference = "Stop"
$scriptRoot = $PSScriptRoot

Write-Host "[Deprecated] scripts/start-rpg.ps1 is kept only for compatibility." -ForegroundColor Yellow
Write-Host "Use .vscode/launch.json compound: RPGを起動（フロント+バックエンド）" -ForegroundColor Yellow

Start-Process powershell -ArgumentList @(
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $scriptRoot "start-frontend.ps1"),
    "-Port", $FrontendPort,
    "-BackendPort", $BackendPort
)

& powershell -ExecutionPolicy Bypass -File (Join-Path $scriptRoot "start-backend.ps1") -Port $BackendPort
exit $LASTEXITCODE
