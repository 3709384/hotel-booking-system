$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$stateDir = Join-Path $projectRoot '.local'

function Stop-ProcessTree {
    param([int]$ProcessId)
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcessId" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-ProcessTree -ProcessId $child.ProcessId
    }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

function Stop-ProjectProcess {
    param([string]$Name)
    $pidFile = Join-Path $stateDir "$Name.pid"
    if (-not (Test-Path $pidFile)) {
        Write-Host "$Name PID file not found; skip."
        return
    }
    $processId = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "$Name process is not running."
        Remove-Item -LiteralPath $pidFile -Force
        return
    }
    Write-Host "Stopping $Name process tree, PID $processId..."
    Stop-ProcessTree -ProcessId $processId
    Remove-Item -LiteralPath $pidFile -Force
}

Stop-ProjectProcess -Name 'frontend'
Stop-ProjectProcess -Name 'backend'
Write-Host 'Local application processes stopped. MySQL service was left running.'
