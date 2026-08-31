param(
    [switch]$SkipBuild,
    [switch]$InfrastructureOnly
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    docker info *> $null
    if (-not $SkipBuild -and -not $InfrastructureOnly) {
        & .\mvnw.cmd clean package
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' }
        Push-Location web-admin
        try {
            npm ci
            if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed' }
            npm run build
            if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
        } finally { Pop-Location }
    }
    if ($InfrastructureOnly) {
        docker compose up -d mysql redis nacos namesrv broker rocketmq-init
    } else {
        docker compose up -d --build
    }
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose startup failed' }
    Write-Host 'AutoFlow startup requested. Run scripts/wait-health.ps1 to verify readiness.' -ForegroundColor Green
} finally { Pop-Location }
