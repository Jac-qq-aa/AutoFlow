param([int]$TimeoutSeconds = 180)
$ErrorActionPreference = 'Stop'
$targets = @(
    @{ Name = 'Gateway'; Url = 'http://localhost:8080/actuator/health' },
    @{ Name = 'Web'; Url = 'http://localhost:5173/' }
)
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
foreach ($target in $targets) {
    $ready = $false
    while ((Get-Date) -lt $deadline -and -not $ready) {
        $previousErrorAction = $ErrorActionPreference
        $ErrorActionPreference = 'SilentlyContinue'
        & curl.exe --fail --silent --max-time 4 --output NUL $target.Url 2>$null
        $exitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorAction
        $ready = $exitCode -eq 0
        if (-not $ready) { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw "$($target.Name) did not become ready: $($target.Url)" }
    Write-Host "READY $($target.Name)" -ForegroundColor Green
}

$internalTargets = @(
    @{ Name = 'Order'; Url = 'http://order-service:8082/actuator/health' },
    @{ Name = 'Inventory'; Url = 'http://inventory-service:8083/actuator/health' },
    @{ Name = 'Fulfillment'; Url = 'http://fulfillment-service:8084/actuator/health' }
)
foreach ($target in $internalTargets) {
    $ready = $false
    while ((Get-Date) -lt $deadline -and -not $ready) {
        $exitCode = 1
        try {
            $response = & docker compose exec -T web-admin wget -q -O - $target.Url 2>$null
            $exitCode = $LASTEXITCODE
        }
        catch {
            $response = ''
        }
        $ready = $exitCode -eq 0 -and $response -match '"status":"UP"'
        if (-not $ready) { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw "$($target.Name) did not become ready inside the Compose network: $($target.Url)" }
    Write-Host "READY $($target.Name) (internal only)" -ForegroundColor Green
}
