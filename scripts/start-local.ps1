param([switch]$NoFrontend)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$backendDir = Join-Path $projectRoot 'backend'
$frontendDir = Join-Path $projectRoot 'frontend'
$stateDir = Join-Path $projectRoot '.local'
$logDir = Join-Path $stateDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Test-PortListening {
    param([int]$Port)
    $connections = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    return $null -ne $connections
}

function Resolve-JavaHome {
    foreach ($candidate in @('C:\Program Files\Java\jdk-17', 'C:\Program Files\Java\jdk-21')) {
        if (Test-Path (Join-Path $candidate 'bin\java.exe')) { return $candidate }
    }
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { return $env:JAVA_HOME }
    throw 'No usable JDK found. Install JDK 17 or 21, or set JAVA_HOME.'
}

$mysqlService = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
if ($mysqlService -and $mysqlService.Status -ne 'Running') {
    Write-Host 'Starting MySQL80...'
    Start-Service -Name 'MySQL80'
}

$javaHome = Resolve-JavaHome
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8'
Write-Host "Using Java: $javaHome"

if (-not (Test-PortListening -Port 8080)) {
    $mvnw = Join-Path $backendDir 'mvnw.cmd'
    if (-not (Test-Path $mvnw)) { throw "Maven wrapper not found: $mvnw" }
    Write-Host 'Preparing backend build cache...'
    Push-Location $backendDir
    try {
        & $mvnw -q clean
        if ($LASTEXITCODE -ne 0) { throw 'Backend clean failed before startup.' }
    } finally {
        Pop-Location
    }
    $backendProcess = Start-Process -FilePath $mvnw -ArgumentList 'spring-boot:run' -WorkingDirectory $backendDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'backend.out.log') -RedirectStandardError (Join-Path $logDir 'backend.err.log') -PassThru
    Set-Content -LiteralPath (Join-Path $stateDir 'backend.pid') -Value $backendProcess.Id -Encoding ascii
    Write-Host "Starting backend, PID $($backendProcess.Id)..."
} else {
    Write-Host 'Backend port 8080 is already in use; reuse the running service.'
}

$backendReady = $false
for ($i = 0; $i -lt 90; $i++) {
    try {
        $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 2
        if ($health.code -eq 0) { $backendReady = $true; break }
    } catch { }
    Start-Sleep -Milliseconds 500
}
if (-not $backendReady) { throw "Backend did not become ready. Check $logDir\backend.out.log and backend.err.log" }
Write-Host 'Backend ready: http://127.0.0.1:8080/api'

if (-not $NoFrontend) {
    if (-not (Test-PortListening -Port 5173)) {
        $npm = (Get-Command npm.cmd -ErrorAction Stop).Source
        $frontendProcess = Start-Process -FilePath $npm -ArgumentList @('run', 'dev', '--', '--host', '127.0.0.1') -WorkingDirectory $frontendDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'frontend.out.log') -RedirectStandardError (Join-Path $logDir 'frontend.err.log') -PassThru
        Set-Content -LiteralPath (Join-Path $stateDir 'frontend.pid') -Value $frontendProcess.Id -Encoding ascii
        Write-Host "Starting frontend, PID $($frontendProcess.Id)..."
    } else {
        Write-Host 'Frontend port 5173 is already in use; reuse the running service.'
    }

    $frontendReady = $false
    for ($i = 0; $i -lt 40; $i++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:5173/' -TimeoutSec 2
            if ($response.StatusCode -eq 200) { $frontendReady = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    if (-not $frontendReady) { throw "Frontend did not become ready. Check $logDir\frontend.out.log and frontend.err.log" }
    Write-Host 'Frontend ready: http://127.0.0.1:5173'
}

Write-Host ''
Write-Host 'Local environment is ready.'
Write-Host 'Member:       13900000001 / 123456'
Write-Host 'Receptionist: 13700000001 / front123'
Write-Host 'Administrator:13800000000 / admin123'
