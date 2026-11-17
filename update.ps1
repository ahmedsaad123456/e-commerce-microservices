
# update.ps1
#
# Usage:
#   .\update.ps1 customer-service
#       -> Build config-server + customer-service, then recreate both.
#
#   .\update.ps1 customer-service order-service
#       -> Build config-server + both selected projects.
#
#   .\update.ps1 gateway-service
#       -> Build gateway-service once, then deploy gateway-1 and gateway-2.
#
#   .\update.ps1 customer-service -NoConfig
#       -> Skip config-server.
#
#   .\update.ps1 customer-service -RestartOnly
#       -> Rebuild config-server, but only recreate customer-service.
#          No Maven build and no Docker image rebuild for customer-service.
#
#   .\update.ps1 customer-service -NoConfig -RestartOnly
#       -> Only recreate customer-service without building anything.
#
#   .\update.ps1 customer-service -Tests
#       -> Run Maven tests during the build.
#
#   .\update.ps1 notification-service customer-service order-service payment-service product-service gateway-service
#       -> Build all selected projects and deploy all their mapped Compose services.

param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Services = @(),

    [switch]$NoConfig,
    [switch]$RestartOnly,
    [switch]$Tests
)

$ErrorActionPreference = 'Stop'

# Always execute from the directory containing this script.
Set-Location $PSScriptRoot


# ---------------------------------------------------------------
# 1) Maven project folder -> Docker Compose service names.
#
# A single Maven project may be deployed as multiple containers.
# ---------------------------------------------------------------

$composeNames = @{
    'discovery-service' = @('eureka-server')

    # One Gateway image, two running instances.
    'gateway-service' = @(
        'gateway-1'
        'gateway-2'
    )

    # One Order Service image, two running instances.
    'order-service' = @(
        'order-service'
        'order-service-2'
    )
}


# ---------------------------------------------------------------
# 2) Build a Maven project JAR.
# ---------------------------------------------------------------

function Build-Jar([string]$folder) {

    $pomPath = Join-Path $PSScriptRoot "$folder/pom.xml"

    if (-not (Test-Path $pomPath)) {
        throw "Folder '$folder' not found or has no pom.xml"
    }

    Write-Host "==> mvn package: $folder" -ForegroundColor Cyan

    Push-Location $folder

    try {

        $mvnArgs = @(
            '-B'
            'clean'
            'package'
        )

        # Skip tests unless the -Tests switch was provided.
        if (-not $Tests) {
            $mvnArgs += '-DskipTests'
        }

        & mvn @mvnArgs

        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed for '$folder'"
        }

    }
    finally {
        Pop-Location
    }
}


# ---------------------------------------------------------------
# 3) Resolve a Maven folder to one or more Compose services.
# ---------------------------------------------------------------

function Resolve-ComposeServices([string]$folder) {

    if ($composeNames.ContainsKey($folder)) {

        # Return all Compose services mapped to this project.
        foreach ($composeService in $composeNames[$folder]) {
            Write-Output $composeService
        }

        return
    }

    # By default, Maven folder and Compose service have the same name.
    Write-Output $folder
}


# ---------------------------------------------------------------
# 4) Collect the Docker Compose targets.
# ---------------------------------------------------------------

$targets = [System.Collections.Generic.List[string]]::new()

# Include Config Server unless -NoConfig was provided.
if (-not $NoConfig) {
    $targets.Add('config-server')
}

# Map every requested Maven project to its Compose services.
foreach ($service in $Services) {

    $composeServices = @(
        Resolve-ComposeServices $service
    )

    foreach ($composeService in $composeServices) {

        # Avoid adding duplicate targets.
        if (-not $targets.Contains([string]$composeService)) {
            $targets.Add([string]$composeService)
        }
    }
}

if ($targets.Count -eq 0) {
    throw 'Nothing to do: pass a service name or include config-server.'
}


# ---------------------------------------------------------------
# 5) Validate Compose service names BEFORE building anything.
# ---------------------------------------------------------------

Write-Host '==> Validating Docker Compose services...' -ForegroundColor Cyan

$availableComposeServices = @(
    & docker compose config --services
)

if ($LASTEXITCODE -ne 0) {
    throw 'Could not read Docker Compose services. Check docker-compose.yaml.'
}

$missingServices = @(
    $targets | Where-Object {
        $_ -notin $availableComposeServices
    }
)

if ($missingServices.Count -gt 0) {

    Write-Host 'Available Compose services:' -ForegroundColor Yellow
    $availableComposeServices | ForEach-Object {
        Write-Host "  - $_"
    }

    throw "Unknown Docker Compose service(s): $($missingServices -join ', ')"
}

Write-Host "Compose targets: $($targets -join ', ')" -ForegroundColor Green


# ---------------------------------------------------------------
# 6) Build Maven JARs.
#
# Config Server is built first because it contains configuration
# files used by the other services.
#
# -RestartOnly skips Maven builds for the requested services.
# ---------------------------------------------------------------

$toBuild = @()

if (-not $NoConfig) {
    $toBuild += 'config-server'
}

if (-not $RestartOnly) {
    $toBuild += $Services
}

$toBuild = @(
    $toBuild | Select-Object -Unique
)

foreach ($folder in $toBuild) {
    Build-Jar $folder
}


# ---------------------------------------------------------------
# 7) Build and deploy Docker services.
# ---------------------------------------------------------------

if ($RestartOnly) {

    # Config Server is rebuilt unless -NoConfig was provided.
    if (-not $NoConfig) {

        Write-Host '==> Rebuilding Config Server Docker image...' -ForegroundColor Cyan

        & docker compose build config-server

        if ($LASTEXITCODE -ne 0) {
            throw 'Docker image build failed for config-server'
        }
    }

    # Recreate selected containers without rebuilding their images.
    $composeArgs = @(
        'up'
        '-d'
        '--no-build'
        '--force-recreate'
    ) + $targets

}
else {

    # Normal deployment:
    # Build images and recreate all selected containers.
    $composeArgs = @(
        'up'
        '-d'
        '--build'
        '--force-recreate'
    ) + $targets
}


Write-Host "==> docker compose $($composeArgs -join ' ')" -ForegroundColor Cyan

& docker compose @composeArgs

if ($LASTEXITCODE -ne 0) {
    throw 'docker compose up failed'
}


# ---------------------------------------------------------------
# 8) Show final container status.
# ---------------------------------------------------------------

Write-Host ''
Write-Host '==> Final Docker Compose status:' -ForegroundColor Cyan

& docker compose ps

if ($LASTEXITCODE -ne 0) {
    throw 'Could not read Docker Compose container status.'
}

Write-Host ''
Write-Host 'Deployment completed successfully.' -ForegroundColor Green
