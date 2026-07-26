<#
.SYNOPSIS
    Starts the Auto Accessories Store project (infrastructure + backend + frontend).

.DESCRIPTION
    Convenience launcher for local development on Windows.

    By default it:
      1. Starts PostgreSQL, Redis and Kafka via Docker Compose (--profile infra)
      2. Launches the Spring Boot backend in a new window   (http://localhost:8080/api/v1)
      3. Launches the Vite frontend dev server in a new window (http://localhost:3000)

.PARAMETER Only
    Run only selected parts. Any combination of: infra, backend, frontend.
    Example: -Only infra,backend

.PARAMETER SkipInstall
    Skip 'npm install' for the frontend (use when node_modules is already present).

.PARAMETER Stop
    Stop the Docker infrastructure containers and exit.

.EXAMPLE
    .\run.ps1
    Start everything.

.EXAMPLE
    .\run.ps1 -Only frontend -SkipInstall
    Start just the frontend without reinstalling dependencies.

.EXAMPLE
    .\run.ps1 -Stop
    Stop the Docker infrastructure.
#>

[CmdletBinding()]
param(
    [ValidateSet('infra', 'backend', 'frontend')]
    [string[]]$Only = @('infra', 'backend', 'frontend'),

    [switch]$SkipInstall,

    [switch]$Stop
)

$ErrorActionPreference = 'Stop'

# Resolve paths relative to this script so it works from any working directory.
$Root        = $PSScriptRoot
$BackendDir  = Join-Path $Root 'backend'
$FrontendDir = Join-Path $Root 'frontend'
$LogsDir     = Join-Path $Root 'logs'
$BackendLogs = Join-Path $LogsDir 'backend'
$FrontendLogs= Join-Path $LogsDir 'frontend'

# Ensure log directories exist.
New-Item -ItemType Directory -Force -Path $BackendLogs, $FrontendLogs | Out-Null

$RunStamp = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'

function Write-Step  { param($m) Write-Host "`n==> $m" -ForegroundColor Cyan }
function Write-Ok    { param($m) Write-Host "    $m" -ForegroundColor Green }
function Write-Warn2 { param($m) Write-Host "    $m" -ForegroundColor Yellow }

function Test-Command {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

# --------------------------------------------------------------------------
# Stop mode
# --------------------------------------------------------------------------
if ($Stop) {
    Write-Step 'Stopping Docker infrastructure...'
    Push-Location $BackendDir
    try {
        docker compose --profile infra down
        Write-Ok 'Infrastructure stopped.'
    }
    finally {
        Pop-Location
    }
    return
}

# --------------------------------------------------------------------------
# Prerequisite checks
# --------------------------------------------------------------------------
Write-Step 'Checking prerequisites...'

if (($Only -contains 'infra') -and -not (Test-Command 'docker')) {
    throw 'Docker is not installed or not on PATH. Install Docker Desktop, or run with -Only backend,frontend.'
}
if (($Only -contains 'frontend') -and -not (Test-Command 'node')) {
    throw 'Node.js is not installed or not on PATH. Install Node.js 18+ from https://nodejs.org'
}
if (($Only -contains 'backend') -and -not (Test-Path (Join-Path $BackendDir 'mvnw.cmd'))) {
    throw "Maven wrapper not found at $BackendDir\mvnw.cmd"
}
Write-Ok 'Prerequisites OK.'

# --------------------------------------------------------------------------
# 1. Infrastructure (PostgreSQL, Redis, Kafka)
# --------------------------------------------------------------------------
if ($Only -contains 'infra') {
    Write-Step 'Starting infrastructure (PostgreSQL, Redis, Kafka)...'

    $envFile = Join-Path $BackendDir '.env'
    if (-not (Test-Path $envFile)) {
        $example = Join-Path $BackendDir '.env.example'
        if (Test-Path $example) {
            Copy-Item $example $envFile
            Write-Warn2 'Created backend\.env from .env.example - review the passwords inside it.'
        }
        else {
            Write-Warn2 'No backend\.env found and no .env.example to copy. Docker Compose may fail.'
        }
    }

    Push-Location $BackendDir
    try {
        docker compose --profile infra up -d
        if ($LASTEXITCODE -ne 0) { throw 'docker compose failed to start infrastructure.' }
        Write-Ok 'Infrastructure containers are up: postgres:5432, redis:6379, kafka:9094.'
    }
    finally {
        Pop-Location
    }
}

# --------------------------------------------------------------------------
# 2. Backend (Spring Boot)
# --------------------------------------------------------------------------
if ($Only -contains 'backend') {
    Write-Step 'Launching backend (Spring Boot) in a new window...'
    Write-Warn2 'Ensure backend\src\main\resources\application.yaml has valid DB/Redis/JWT values.'

    $backendLog = Join-Path $BackendLogs "console-$RunStamp.log"
    Start-Process -FilePath 'powershell.exe' -WorkingDirectory $BackendDir -ArgumentList @(
        '-NoExit', '-Command',
        "Write-Host 'Backend: http://localhost:8080/api/v1' -ForegroundColor Cyan; Write-Host 'Logging console to $backendLog' -ForegroundColor DarkGray; .\mvnw.cmd spring-boot:run 2>&1 | Tee-Object -FilePath '$backendLog'"
    )
    Write-Ok 'Backend starting at http://localhost:8080/api/v1 (Swagger: /swagger-ui.html)'
    Write-Ok "Backend console log: $backendLog"
    Write-Ok "Backend app log:     $(Join-Path $BackendLogs 'backend.log')"
}

# --------------------------------------------------------------------------
# 3. Frontend (Vite + React)
# --------------------------------------------------------------------------
if ($Only -contains 'frontend') {
    Write-Step 'Launching frontend (Vite) in a new window...'

    if (-not (Test-Path (Join-Path $FrontendDir '.env'))) {
        $feExample = Join-Path $FrontendDir '.env.example'
        if (Test-Path $feExample) {
            Copy-Item $feExample (Join-Path $FrontendDir '.env')
            Write-Warn2 'Created frontend\.env from .env.example.'
        }
    }

    $installCmd = if ($SkipInstall) { '' } else { 'npm install; ' }

    $frontendLog = Join-Path $FrontendLogs "console-$RunStamp.log"
    Start-Process -FilePath 'powershell.exe' -WorkingDirectory $FrontendDir -ArgumentList @(
        '-NoExit', '-Command',
        "Write-Host 'Frontend: http://localhost:3000' -ForegroundColor Cyan; Write-Host 'Logging console to $frontendLog' -ForegroundColor DarkGray; $installCmd npm run dev 2>&1 | Tee-Object -FilePath '$frontendLog'"
    )
    Write-Ok 'Frontend starting at http://localhost:3000'
    Write-Ok "Frontend console log: $frontendLog"
}

Write-Step 'Done.'
Write-Host '    Backend and frontend run in their own windows. Close them to stop those services.' -ForegroundColor Gray
Write-Host '    Stop infrastructure with:  .\run.ps1 -Stop' -ForegroundColor Gray
