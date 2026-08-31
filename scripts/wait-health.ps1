param([int]$TimeoutSeconds = 180)
$ErrorActionPreference = 'Stop'
$targets = @(
    @{ Name = 'Gateway'; Url = 'http://localhost:8080/actuator/health' },
    @{ Name = 'Order'; Url = 'http://localhost:8082/actuator/health' },
    @{ Name = 'Inventory'; Url = 'http://localhost:8083/actuator/health' },
    @{ Name = 'Fulfillment'; Url = 'http://localhost:8084/actuator/health' },
    @{ Name = 'Web'; Url = 'http://localhost:5173/' }
)
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
foreach ($target in $targets) {
    $ready = $false
    while ((Get-Date) -lt $deadline -and -not $ready) {
        & curl.exe --fail --silent --show-error --max-time 4 --output NUL $target.Url 2>$null
        $ready = $LASTEXITCODE -eq 0
        if (-not $ready) { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw "$($target.Name) did not become ready: $($target.Url)" }
    Write-Host "READY $($target.Name)" -ForegroundColor Green
}
