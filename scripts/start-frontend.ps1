param(
    [int]$Port = 5500,
    [int]$BackendPort = 8080
)

$ErrorActionPreference = "Stop"
$workspaceRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $workspaceRoot "frontend/public"

if (-not (Test-Path $frontendRoot)) {
    Write-Error "frontend/public directory not found: $frontendRoot"
}

Add-Type -AssemblyName System.Web

function Get-ContentType {
    param([string]$Path)

    switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
        ".html" { return "text/html; charset=utf-8" }
        ".css" { return "text/css; charset=utf-8" }
        ".js" { return "application/javascript; charset=utf-8" }
        ".json" { return "application/json; charset=utf-8" }
        ".svg" { return "image/svg+xml" }
        ".png" { return "image/png" }
        ".jpg" { return "image/jpeg" }
        ".jpeg" { return "image/jpeg" }
        ".ico" { return "image/x-icon" }
        default { return "application/octet-stream" }
    }
}

$listener = [System.Net.HttpListener]::new()
$prefix = "http://localhost:$Port/"
$listener.Prefixes.Add($prefix)
$listener.Start()

Write-Host "Frontend started: $prefix" -ForegroundColor Cyan
Write-Host "API endpoint: http://localhost:$BackendPort/api/game" -ForegroundColor DarkCyan

try {
    Start-Process $prefix | Out-Null
} catch {
    # Browser launch is best-effort; server should keep running regardless.
}

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $requestPath = [System.Web.HttpUtility]::UrlDecode($context.Request.Url.AbsolutePath)

        if ([string]::IsNullOrWhiteSpace($requestPath) -or $requestPath -eq "/") {
            $requestPath = "/index.html"
        }

        $relativePath = $requestPath.TrimStart('/').Replace('/', [System.IO.Path]::DirectorySeparatorChar)
        $target = [System.IO.Path]::GetFullPath((Join-Path $frontendRoot $relativePath))
        $rootFull = [System.IO.Path]::GetFullPath($frontendRoot)

        if (-not $target.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path $target) -or (Test-Path $target -PathType Container)) {
            $bytes = [System.Text.Encoding]::UTF8.GetBytes("404 Not Found")
            $context.Response.StatusCode = 404
            $context.Response.ContentType = "text/plain; charset=utf-8"
            $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
            $context.Response.Close()
            continue
        }

        $bytes = [System.IO.File]::ReadAllBytes($target)
        $context.Response.StatusCode = 200
        $context.Response.ContentType = Get-ContentType -Path $target
        $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
        $context.Response.Close()
    }
}
finally {
    if ($listener.IsListening) {
        $listener.Stop()
    }
    $listener.Close()
}
