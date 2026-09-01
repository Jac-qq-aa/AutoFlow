param([int]$Requests = 20)
$ErrorActionPreference = 'Stop'
$loginBody = @{ username = 'admin'; password = 'demo123' } | ConvertTo-Json
$token = (Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/login' -ContentType 'application/json' -Body $loginBody).token
$headers = @{ Authorization = "Bearer $token" }
$candidates = @(
    @{ storeId = 'STORE-SH-001'; modelCode = 'AF-SUV-PRO' },
    @{ storeId = 'STORE-SH-001'; modelCode = 'AF-SEDAN-X' },
    @{ storeId = 'STORE-BJ-001'; modelCode = 'AF-SUV-PRO' },
    @{ storeId = 'STORE-BJ-001'; modelCode = 'AF-CITY-EV' },
    @{ storeId = 'STORE-SZ-001'; modelCode = 'AF-SEDAN-X' },
    @{ storeId = 'STORE-SZ-001'; modelCode = 'AF-CITY-EV' }
)
$selected = $candidates | ForEach-Object {
    $quota = (Invoke-RestMethod "http://localhost:8080/api/inventory/quota?storeId=$($_.storeId)&modelCode=$($_.modelCode)" -Headers $headers).data
    [pscustomobject]@{ storeId = $_.storeId; modelCode = $_.modelCode; available = $quota.available }
} | Sort-Object available -Descending | Select-Object -First 1
if (-not $selected -or $selected.available -le 0) { throw 'No positive inventory quota is available for a meaningful concurrency test' }
$storeId = $selected.storeId
$modelCode = $selected.modelCode
$before = (Invoke-RestMethod "http://localhost:8080/api/inventory/quota?storeId=$storeId&modelCode=$modelCode" -Headers $headers).data
$jobs = 1..$Requests | ForEach-Object {
    $id = [guid]::NewGuid().ToString()
    Start-Job -ScriptBlock {
        param($OrderId, $Store, $Model, $Token)
        try {
            $body = @{ orderId = $OrderId; storeId = $Store; modelCode = $Model } | ConvertTo-Json
            Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/inventory/reservations' -Headers @{ Authorization = "Bearer $Token" } -ContentType 'application/json' -Body $body | Out-Null
            [pscustomobject]@{ Success = $true; OrderId = $OrderId }
        } catch { [pscustomobject]@{ Success = $false; OrderId = $OrderId } }
    } -ArgumentList $id, $storeId, $modelCode, $token
}
$results = $jobs | Receive-Job -Wait -AutoRemoveJob
$after = (Invoke-RestMethod "http://localhost:8080/api/inventory/quota?storeId=$storeId&modelCode=$modelCode" -Headers $headers).data
$successful = @($results | Where-Object { $_.Success })
$successes = $successful.Count
if ($after.available -lt 0) { throw "Oversell detected: available=$($after.available)" }
if ($successes -gt $before.available) { throw "More reservations succeeded ($successes) than initial quota ($($before.available))" }
foreach ($result in $successful) {
    Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/inventory/reservations/$($result.OrderId)/release" -Headers $headers | Out-Null
}
$restored = (Invoke-RestMethod "http://localhost:8080/api/inventory/quota?storeId=$storeId&modelCode=$modelCode" -Headers $headers).data
if ($restored.available -ne $before.available) { throw "Cleanup failed: expected available=$($before.available), actual=$($restored.available)" }
Write-Host "CONCURRENCY TEST PASSED requests=$Requests success=$successes before=$($before.available) exhausted=$($after.available) restored=$($restored.available)" -ForegroundColor Green
