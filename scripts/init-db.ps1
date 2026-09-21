param(
    [string]$Mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$User = 'root',
    [string]$Password = 'root',
    [switch]$Reset
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Test-Path $Mysql)) { throw "mysql.exe not found: $Mysql" }

$exists = & $Mysql "--user=$User" "--password=$Password" -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='hotel_booking';"
if ($LASTEXITCODE -ne 0) { throw 'Cannot connect to MySQL. Check the user and password.' }

if ([int]$exists -gt 0 -and -not $Reset) {
    Write-Host 'Database hotel_booking already exists. Use -Reset only when you intentionally want to recreate development data.'
    exit 0
}

Push-Location $projectRoot
try {
    Write-Host 'Creating database schema...'
    & $Mysql "--user=$User" "--password=$Password" --default-character-set=utf8mb4 -e "source database/schema.sql"
    if ($LASTEXITCODE -ne 0) { throw 'schema.sql failed.' }
    Write-Host 'Importing seed data...'
    & $Mysql "--user=$User" "--password=$Password" --default-character-set=utf8mb4 -e "source database/seed.sql"
    if ($LASTEXITCODE -ne 0) { throw 'seed.sql failed.' }
} finally {
    Pop-Location
}
Write-Host 'Database hotel_booking is ready.'
