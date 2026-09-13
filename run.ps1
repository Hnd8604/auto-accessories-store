<#
.SYNOPSIS
    Starts the Auto Accessories Store project (infrastructure + backend + frontend).

.DESCRIPTION
    Convenience launcher for local development on Windows.

    By default it:
      1. Starts PostgreSQL and Redis via Docker Compose (--profile infra)
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

# File env của DEV. Nằm ở thư mục gốc cạnh docker-compose.yml, song song với
# .env.prod của production (dùng bởi docker-compose.prod.yml trên VPS).
# Cả hai cùng chỗ nên luôn phải chỉ đích danh, không để Compose tự đoán.
$DevEnvFile     = Join-Path $Root '.env.dev'
$DevEnvExample  = Join-Path $Root '.env.dev.example'

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

# Mọi lệnh docker compose của dev đều đi qua đây: chạy từ $Root với
# --env-file trỏ thẳng vào .env.dev ở thư mục gốc.
#
# Trước đây script Push-Location vào backend\ rồi mới gọi `docker compose`;
# Compose đi ngược lên tìm thấy docker-compose.yml ở gốc nhưng lại lấy .env
# từ thư mục đang đứng, nên vô tình đúng. Chạy y hệt lệnh đó từ thư mục gốc
# là các biến POSTGRES_* rỗng và container dựng lên với mật khẩu trống.
function Invoke-DevCompose {
    param([Parameter(Mandatory)][string[]]$ComposeArgs)

    # Chưa có .env.dev thì bỏ qua --env-file để Compose báo lỗi thiếu biến
    # một cách bình thường, thay vì chết vì "env file not found".
    $envArgs = @()
    if (Test-Path $DevEnvFile) { $envArgs = @('--env-file', $DevEnvFile) }

    Push-Location $Root
    try {
        docker compose @envArgs @ComposeArgs
    }
    finally {
        Pop-Location
    }
}

# Spring Boot KHÔNG tự đọc .env.dev — chỉ Docker Compose làm được điều đó.
# Chạy `mvnw spring-boot:run` mà không nạp file này thì ${POSTGRES_USER},
# ${JWT_SIGNER_KEY}... trong application.yaml không phân giải được và app chết
# ngay lúc khởi động. Nạp vào tiến trình hiện tại rồi Start-Process bên dưới
# cho cửa sổ backend kế thừa.
function Import-DevEnv {
    if (-not (Test-Path $DevEnvFile)) {
        Write-Warn2 "Khong tim thay $DevEnvFile - backend se thieu bien moi truong."
        return
    }
    Get-Content $DevEnvFile |
        Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } |
        ForEach-Object {
            $parts = $_ -split '=', 2
            $name  = $parts[0].Trim()
            # Cắt comment cuối dòng (đứng sau >= 2 khoảng trắng) nhưng giữ
            # nguyên giá trị có khoảng trắng, vd MAIL_PASSWORD=abcd efgh ijkl mnop
            $value = ($parts[1] -replace '\s{2,}#.*$', '').Trim()
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    # Backend chạy ngoài docker nên trỏ về cổng mà compose publish ra localhost.
    [Environment]::SetEnvironmentVariable('DB_HOST', 'localhost', 'Process')
    [Environment]::SetEnvironmentVariable('REDIS_HOST', 'localhost', 'Process')
}

# --------------------------------------------------------------------------
# Stop mode
# --------------------------------------------------------------------------
if ($Stop) {
    Write-Step 'Stopping Docker infrastructure...'
    Invoke-DevCompose @('--profile', 'infra', 'down')
    Write-Ok 'Infrastructure stopped.'
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
# 1. Infrastructure (PostgreSQL, Redis)
# --------------------------------------------------------------------------
if ($Only -contains 'infra') {
    Write-Step 'Starting infrastructure (PostgreSQL, Redis)...'

    if (-not (Test-Path $DevEnvFile)) {
        if (Test-Path $DevEnvExample) {
            Copy-Item $DevEnvExample $DevEnvFile
            Write-Warn2 'Created .env.dev from .env.dev.example - review the passwords inside it.'
        }
        else {
            Write-Warn2 'No .env.dev found and no .env.dev.example to copy. Docker Compose may fail.'
        }
    }

    Invoke-DevCompose @('--profile', 'infra', 'up', '-d')
    if ($LASTEXITCODE -ne 0) { throw 'docker compose failed to start infrastructure.' }
    Write-Ok 'Infrastructure containers are up: postgres:5432, redis:6379.'
}

# --------------------------------------------------------------------------
# 2. Backend (Spring Boot)
# --------------------------------------------------------------------------
if ($Only -contains 'backend') {
    Write-Step 'Launching backend (Spring Boot) in a new window...'
    Import-DevEnv
    Write-Ok "Loaded environment variables from $DevEnvFile"

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

    # Không còn frontend\.env. Vite đọc thẳng .env.dev ở thư mục gốc — xem
    # phần loadEnv trong frontend\vite.config.ts.

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
