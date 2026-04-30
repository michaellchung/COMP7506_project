# NoteMind launcher - Docker + Flask backend + ADB reverse.

param(
    [switch]$SkipDocker,
    [switch]$SkipBackend,
    [switch]$SkipAdb
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host $msg -ForegroundColor Cyan }
function Ok($msg) { Write-Host $msg -ForegroundColor Green }
function Warn($msg) { Write-Host $msg -ForegroundColor Yellow }
function Err($msg) { Write-Host $msg -ForegroundColor Red }

Write-Host ""
Write-Host "========================================" -ForegroundColor Blue
Write-Host "   NoteMind Launcher" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# ============== 1. Docker ==============
if (-not $SkipDocker) {
    Info "[1/3] Starting Docker databases..."
    
    try {
        $null = docker info 2>&1
        Ok "   Docker is running"
    } catch {
        Err "   Docker is not running. Please start Docker Desktop first."
        exit 1
    }
    
    docker-compose up -d
    if ($LASTEXITCODE -ne 0) {
        Err "   Docker startup failed"
        exit 1
    }
    Ok "   Docker containers started"
    Warn "   Waiting for Milvus initialization..."
    
    # Simple wait for Milvus startup.
    # for ($i = 1; $i -le 15; $i++) {
    #     Write-Host "     Waiting... ($i/15)" -ForegroundColor Gray
    #     Start-Sleep -Seconds 2
    # }
    Ok "   Milvus should be ready. First startup may take longer."
} else {
    Warn "[1/3] Skipping Docker startup"
}

# ============== 2. ADB ==============
if (-not $SkipAdb) {
    Info "[2/3] Configuring ADB reverse port mapping..."
    
    # Locate adb.
    $adbPath = "adb"
    $paths = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    )
    foreach ($p in $paths) {
        if (Test-Path $p) {
            $adbPath = $p
            break
        }
    }
    
    $devices = & $adbPath devices 2>&1
    if ($devices -match "device$") {
        # Removing a non-existing reverse mapping returns an error; ignore it.
        try {
            & $adbPath reverse --remove tcp:5000 2>$null | Out-Null
        } catch {
            # No existing listener is fine.
        }
        & $adbPath reverse tcp:5000 tcp:5000
        if ($LASTEXITCODE -eq 0) {
            Ok "   ADB reverse ready: phone localhost:5000 -> PC 5000"
        } else {
            Err "   ADB reverse failed"
        }
    } else {
        Warn "   No USB device detected. Check USB debugging and authorization."
    }
} else {
    Warn "[2/3] Skipping ADB setup"
}

# ============== 3. Python backend ==============
if (-not $SkipBackend) {
    Info "[3/3] Starting Python backend..."
    
    $backendDir = Join-Path $PSScriptRoot "backend"
    if (-not (Test-Path $backendDir)) {
        Err "   Backend directory does not exist: $backendDir"
        exit 1
    }
    
    # Create virtual environment if needed.
    $activate = Join-Path $backendDir ".venv\Scripts\activate.ps1"
    if (-not (Test-Path $activate)) {
        Warn "   Creating virtual environment..."
        Set-Location $backendDir
        python -m venv .venv
    }
    
    # Start backend in a new PowerShell window.
    $cmd = "cd `"$backendDir`"; & `"$activate`"; python app.py; pause"
    Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $cmd
    
    Ok "   Python backend started in a separate window"
} else {
    Warn "[3/3] Skipping backend startup"
}

# ============== Done ==============
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   Startup finished" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Info "Next steps:"
Write-Host "   1. Run the app in Android Studio" -ForegroundColor White
Write-Host "   2. Set Profile Backend URL to: http://127.0.0.1:5000" -ForegroundColor White
Write-Host "   3. Open Chat and test" -ForegroundColor White
Write-Host ""
