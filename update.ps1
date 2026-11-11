# update.ps1
# Usage:
#   .\update.ps1 customer-service             -> rebuild config-server + customer-service
#   .\update.ps1 customer-service order-service
#   .\update.ps1 customer-service -NoConfig   -> skip config-server
#   .\update.ps1 customer-service -RestartOnly -> config-server is rebuilt, the service is only recreated (no mvn)
#   .\update.ps1 customer-service -Tests      -> run the tests during the maven build
param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Services = @(),
    [switch]$NoConfig,
    [switch]$RestartOnly,
    [switch]$Tests
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# folder name -> docker compose service name (only where they differ)
$composeNames = @{ 'discovery-service' = 'eureka-server' }

function Build-Jar([string]$folder) {
    if (-not (Test-Path "$folder/pom.xml")) {
        throw "Folder '$folder' not found or has no pom.xml"
    }
    Write-Host "==> mvn package: $folder" -ForegroundColor Cyan
    Push-Location $folder
    try {
        $mvnArgs = @('-B', 'clean', 'package')
        if (-not $Tests) { $mvnArgs += '-DskipTests' }
        & mvn @mvnArgs
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed for $folder" }
    }
    finally { Pop-Location }
}

function To-Compose([string]$folder) {
    if ($composeNames.ContainsKey($folder)) { return $composeNames[$folder] }
    return $folder
}

# 1) build the jars (config-server first, because it carries the config files)
$toBuild = @()
if (-not $NoConfig) { $toBuild += 'config-server' }
if (-not $RestartOnly) { $toBuild += $Services }
foreach ($f in ($toBuild | Select-Object -Unique)) { Build-Jar $f }

$targets = [System.Collections.Generic.List[string]]::new()

if (-not $NoConfig) {
    $targets.Add('config-server')
}

foreach ($service in $Services) {
    $targets.Add((To-Compose $service))
}

$targets = @($targets | Select-Object -Unique)

if ($targets.Count -eq 0) {
    throw 'Nothing to do: pass a service name or remove -NoConfig'
}

Write-Host "==> docker compose up: $($targets -join ', ')" -ForegroundColor Cyan

$composeArgs = @(
    'up'
    '-d'
    '--build'
    '--force-recreate'
) + $targets

& docker compose @composeArgs

if ($LASTEXITCODE -ne 0) {
    throw 'docker compose up failed'
}

docker compose ps
Write-Host 'Done.' -ForegroundColor Green