param([switch]$SkipSmoke)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$backendDir = Join-Path $projectRoot 'backend'
$frontendDir = Join-Path $projectRoot 'frontend'

$javaHome = $null
foreach ($candidate in @('C:\Program Files\Java\jdk-17', 'C:\Program Files\Java\jdk-21')) {
    if (Test-Path (Join-Path $candidate 'bin\java.exe')) { $javaHome = $candidate; break }
}
if (-not $javaHome) { throw 'No JDK 17/21 found.' }
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8'

Write-Host '[1/4] Backend test and compile'
Push-Location $backendDir
try {
    & .\mvnw.cmd clean test
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
} finally {
    Pop-Location
}

Write-Host '[2/4] Frontend production build'
Push-Location $frontendDir
try {
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally {
    Pop-Location
}

Write-Host '[3/4] Restart local services'
& (Join-Path $PSScriptRoot 'stop-local.ps1')
& (Join-Path $PSScriptRoot 'start-local.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Local service startup failed.' }

if (-not $SkipSmoke) {
    Write-Host '[4/4] API smoke test'
    & (Join-Path $PSScriptRoot 'api-smoke.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'API smoke test failed.' }
} else {
    Write-Host '[4/4] Smoke test skipped'
}

Write-Host 'Local verification passed.'
