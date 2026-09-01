$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080'
function Post-Json([string]$Url, $Body, [string]$Token) {
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    Invoke-RestMethod -Method Post -Uri $Url -Headers $headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json)
}
function Wait-Order([string]$OrderId, [string]$Expected, [string]$Token, [int]$Seconds = 45) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        $response = Invoke-RestMethod -Uri "$baseUrl/api/orders/$OrderId" -Headers @{ Authorization = "Bearer $Token" }
        if ($response.data.status -eq $Expected) { return $response.data }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    throw "Order $OrderId did not reach $Expected; current status is $($response.data.status)"
}

$login = Post-Json "$baseUrl/api/auth/login" @{ username = 'manager'; password = 'demo123' } ''
$token = $login.token
$fixtureId = [Guid]::NewGuid().ToString('N')
$modelCode = 'AF-SMOKE-' + $fixtureId.Substring(0, 8).ToUpperInvariant()
$vin = 'LSMOKE' + $fixtureId.Substring(8, 17).ToUpperInvariant()
$fixtureSql = @"
INSERT INTO autoflow_inventory.inventory_quota(store_id, model_code, available, reserved, version, updated_at)
VALUES ('STORE-SH-001', '$modelCode', 1, 0, 0, CURRENT_TIMESTAMP);
INSERT INTO autoflow_inventory.vehicle(vin, store_id, model_code, color, status, updated_at)
VALUES ('$vin', 'STORE-SH-001', '$modelCode', '冒烟测试', 'AVAILABLE', CURRENT_TIMESTAMP);
"@
$fixtureSql | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -N -uroot'
if ($LASTEXITCODE -ne 0) { throw 'Failed to create isolated smoke-test inventory fixture' }

$channelOrderNo = 'SMOKE-' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$created = Post-Json "$baseUrl/api/orders" @{
    channel = 'STORE'; channelOrderNo = $channelOrderNo; storeId = 'STORE-SH-001'
    customerName = '演示客户'; customerPhone = '13800000000'; modelCode = $modelCode; amount = 219800
} $token
$orderId = $created.data.orderId
Post-Json "$baseUrl/api/orders/$orderId/approve" @{} $token | Out-Null
Wait-Order $orderId 'PENDING_PAYMENT' $token | Out-Null
Post-Json "$baseUrl/api/orders/$orderId/pay" @{ scenario = 'SUCCESS' } $token | Out-Null
Wait-Order $orderId 'PENDING_DELIVERY' $token | Out-Null
$deliveryLogin = Post-Json "$baseUrl/api/auth/login" @{ username = 'delivery'; password = 'demo123' } ''
Post-Json "$baseUrl/api/fulfillment/deliveries/$orderId/complete" @{} $deliveryLogin.token | Out-Null
$completed = Wait-Order $orderId 'COMPLETED' $token
Write-Host "SMOKE TEST PASSED orderNo=$($completed.orderNo) vin=$($completed.vin)" -ForegroundColor Green
