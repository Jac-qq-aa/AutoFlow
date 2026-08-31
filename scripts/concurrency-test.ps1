param([int]$Requests = 20)
$ErrorActionPreference = 'Stop'
$storeId = 'STORE-BJ-001'
$modelCode = 'AF-CITY-EV'
$before = (Invoke-RestMethod "http://localhost:8083/api/inventory/quota?storeId=$storeId&modelCode=$modelCode").data
$jobs = 1..$Requests | ForEach-Object {
    $id = [guid]::NewGuid().ToString()
    Start-Job -ScriptBlock {
        param($OrderId, $Store, $Model)
        try {
            $body = @{ orderId = $OrderId; storeId = $Store; modelCode = $Model } | ConvertTo-Json
            Invoke-RestMethod -Method Post -Uri 'http://localhost:8083/api/inventory/reservations' -ContentType 'application/json' -Body $body | Out-Null
            $true
        } catch { $false }
    } -ArgumentList $id, $storeId, $modelCode
}
$results = $jobs | Receive-Job -Wait -AutoRemoveJob
$after = (Invoke-RestMethod "http://localhost:8083/api/inventory/quota?storeId=$storeId&modelCode=$modelCode").data
$successes = @($results | Where-Object { $_ }).Count
if ($after.available -lt 0) { throw "Oversell detected: available=$($after.available)" }
if ($successes -gt $before.available) { throw "More reservations succeeded ($successes) than initial quota ($($before.available))" }
Write-Host "CONCURRENCY TEST PASSED requests=$Requests success=$successes before=$($before.available) after=$($after.available)" -ForegroundColor Green
