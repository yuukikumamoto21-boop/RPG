param(
    [int]$Port = 8080,
    [string]$CorsAllowOrigins = "*",
    [string]$CorsAllowMethods = "GET,POST,OPTIONS",
    [string]$CorsAllowHeaders = "Content-Type",
    [bool]$CorsAllowCredentials = $false
)

$ErrorActionPreference = "Stop"
$workspaceRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $workspaceRoot "backend"

function Refresh-ProcessPath {
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $env:Path = "$machinePath;$userPath"

    $wingetLinks = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links"
    if ((Test-Path $wingetLinks) -and ($env:Path -notlike "*$wingetLinks*")) {
        $env:Path = "$env:Path;$wingetLinks"
    }
}

function Resolve-JavaCommands {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    $javacCmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($javaCmd -and $javacCmd) {
        return @{ java = $javaCmd; javac = $javacCmd }
    }

    Refresh-ProcessPath
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    $javacCmd = Get-Command javac -ErrorAction SilentlyContinue
    if ($javaCmd -and $javacCmd) {
        return @{ java = $javaCmd; javac = $javacCmd }
    }

    return @{ java = $null; javac = $null }
}

function Test-WingetPackageInstalled {
    param([string]$PackageId)

    $wingetCmd = Get-Command winget -ErrorAction SilentlyContinue
    if (-not $wingetCmd) {
        return $false
    }

    $output = winget list --exact --id $PackageId --accept-source-agreements 2>$null
    if ($LASTEXITCODE -ne 0) {
        return $false
    }

    return [bool]($output | Select-String -SimpleMatch $PackageId)
}

if (-not (Test-Path $backendDir)) {
    Write-Error "backend directory not found: $backendDir"
}

Push-Location $backendDir
try {
    $env:APP_PORT = [string]$Port
    $env:CORS_ALLOW_ORIGINS = $CorsAllowOrigins
    $env:CORS_ALLOW_METHODS = $CorsAllowMethods
    $env:CORS_ALLOW_HEADERS = $CorsAllowHeaders
    $env:CORS_ALLOW_CREDENTIALS = if ($CorsAllowCredentials) { "true" } else { "false" }

    $javaTools = Resolve-JavaCommands
    $javaCmd = $javaTools.java
    $javacCmd = $javaTools.javac

    if ((-not $javaCmd -or -not $javacCmd) -and (Get-Command winget -ErrorAction SilentlyContinue)) {
        $jdkId = "Microsoft.OpenJDK.17"
        $jdkInstalled = Test-WingetPackageInstalled -PackageId $jdkId
        if (-not $jdkInstalled) {
            Write-Host "Java not found. Installing OpenJDK 17 via winget..." -ForegroundColor Yellow
            winget install -e --id $jdkId --scope user --accept-package-agreements --accept-source-agreements
        }

        $javaTools = Resolve-JavaCommands
        $javaCmd = $javaTools.java
        $javacCmd = $javaTools.javac
    }

    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        Write-Host "[Backend] Starting with Maven on http://localhost:$Port" -ForegroundColor Cyan
        mvn compile exec:java
        exit $LASTEXITCODE
    }

    if ($javaCmd -and $javacCmd) {
        Write-Host "[Backend] Starting with javac/java on http://localhost:$Port" -ForegroundColor Cyan
        $outDir = Join-Path $backendDir "out"
        if (Test-Path $outDir) {
            Remove-Item -Recurse -Force $outDir
        }
        New-Item -ItemType Directory -Path $outDir | Out-Null

        $sources = Get-ChildItem -Recurse -Path "src/main/java" -Filter "*.java" | ForEach-Object { $_.FullName }
        if (-not $sources -or $sources.Count -eq 0) {
            Write-Error "No Java source files found under src/main/java"
        }

        & javac -encoding UTF-8 -d $outDir $sources
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }

        & java -cp $outDir com.example.rpg.Application
        exit $LASTEXITCODE
    }

    Write-Host "Maven or Java (javac/java) is required." -ForegroundColor Red
    Write-Host "Install one of the following:" -ForegroundColor Red
    Write-Host "- Maven + JDK 17+" -ForegroundColor Red
    Write-Host "- JDK 17+ (javac/java)" -ForegroundColor Red
    exit 1
}
finally {
    Pop-Location
}
