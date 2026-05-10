param(
    [string]$DrawBaseUrl = "http://localhost:8081",
    [string]$PaymentBaseUrl = "http://localhost:8080",
    [string]$DrawMysqlContainer = "fcfsdraw-mysql",
    [string]$PaymentMysqlContainer = "fcfspayment-mysql",
    [string]$MysqlUser = "fcfs",
    [string]$DrawMysqlPassword = "fcfsdrawtest",
    [string]$PaymentMysqlPassword = "fcfspaymenttest",
    [string]$DrawMysqlDatabase = "fcfs_draw",
    [string]$PaymentMysqlDatabase = "fcfs_payment",
    [string]$RedisContainer = "fcfs-redis",
    [int[]]$RequestSteps = @(5000),
    [int]$PaymentUserCount = 5000,
    [int]$LowBalanceUserCount = 2500,
    [string]$ProductName = "load-test-ticket",
    [long]$TotalQuantity = 100,
    [long]$Price = 10000,
    [long]$LowWalletBalance = 5000,
    [long]$HighWalletBalance = 15000,
    [int]$DurationSeconds = 3,
    [int]$Vus = 1000,
    [int]$MaxVus = 3000,
    [int]$WorkerWaitSeconds = 30
)

$ErrorActionPreference = "Stop"

function Invoke-ContainerMySql {
    param(
        [string]$Container,
        [string]$Database,
        [string]$Password,
        [string]$Sql
    )

    docker exec $Container mysql `
        -u $MysqlUser `
        "-p$Password" `
        $Database `
        -e $Sql
}

function Invoke-DrawMySql {
    param([string]$Sql)
    Invoke-ContainerMySql -Container $DrawMysqlContainer -Database $DrawMysqlDatabase -Password $DrawMysqlPassword -Sql $Sql
}

function Invoke-PaymentMySql {
    param([string]$Sql)
    Invoke-ContainerMySql -Container $PaymentMysqlContainer -Database $PaymentMysqlDatabase -Password $PaymentMysqlPassword -Sql $Sql
}

function Invoke-Redis {
    param([string[]]$Arguments)
    docker exec $RedisContainer redis-cli @Arguments
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod -Method Get -Uri $Url | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 1
        }
    }

    throw "Service is not ready. url=$Url"
}

function Reset-DrawState {
    Write-Host "Resetting draw DB tables..."
    Invoke-DrawMySql "SET FOREIGN_KEY_CHECKS=0; TRUNCATE TABLE draw_entries; TRUNCATE TABLE products; SET FOREIGN_KEY_CHECKS=1;"

    Write-Host "Resetting Redis draw queues..."
    Invoke-Redis @("DEL", "draw:queue:products") | Out-Null
    Invoke-Redis @("DEL", "draw:queue:1") | Out-Null
}

function Reset-PaymentState {
    Write-Host "Resetting payment DB tables and seeding wallets..."
    Invoke-PaymentMySql "SET FOREIGN_KEY_CHECKS=0; TRUNCATE TABLE payment_transactions; TRUNCATE TABLE wallets; SET FOREIGN_KEY_CHECKS=1;"

    $values = New-Object System.Collections.Generic.List[string]
    $lowBalanceSeeded = 0
    $highBalanceSeeded = 0
    $highBalanceUserCount = $PaymentUserCount - $LowBalanceUserCount

    for ($userId = 1; $userId -le $PaymentUserCount; $userId++) {
        $useLowBalance = ($lowBalanceSeeded -lt $LowBalanceUserCount) -and (($userId % 2 -eq 1) -or ($highBalanceSeeded -ge $highBalanceUserCount))
        $balance = if ($useLowBalance) {
            $lowBalanceSeeded++
            $LowWalletBalance
        } else {
            $highBalanceSeeded++
            $HighWalletBalance
        }

        $values.Add("($userId, $balance, NOW(), NOW())")

        if ($values.Count -eq 500 -or $userId -eq $PaymentUserCount) {
            $sql = "INSERT INTO wallets (user_id, balance, created_at, updated_at) VALUES " + ($values -join ",") + ";"
            Invoke-PaymentMySql $sql
            $values.Clear()
        }
    }

    Write-Host "Seeded payment wallets. lowBalanceUsers=$LowBalanceUserCount lowBalance=$LowWalletBalance highBalanceUsers=$($PaymentUserCount - $LowBalanceUserCount) highBalance=$HighWalletBalance"
}

function New-Product {
    param([int]$TotalRequests)

    Write-Host "Creating single draw product..."
    $productBody = @{
        name = "$ProductName-$TotalRequests"
        totalQuantity = $TotalQuantity
        price = $Price
    } | ConvertTo-Json

    $productResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$DrawBaseUrl/api/v1/products" `
        -ContentType "application/json" `
        -Body $productBody

    $productId = $productResponse.data.productId
    Write-Host "Created product. productId=$productId quantity=$TotalQuantity price=$Price"
    return $productId
}

function Invoke-K6 {
    param(
        [long]$ProductId,
        [int]$TotalRequests
    )

    Write-Host "Running k6. totalRequests=$TotalRequests durationSeconds=$DurationSeconds"
    k6 run `
        -e "BASE_URL=$DrawBaseUrl" `
        -e "PRODUCT_ID=$ProductId" `
        -e "TOTAL_REQUESTS=$TotalRequests" `
        -e "DURATION_SECONDS=$DurationSeconds" `
        -e "VUS=$Vus" `
        -e "MAX_VUS=$MaxVus" `
        "draw-service/test/load/draw-entry.js"
}

function Show-Results {
    param([long]$ProductId)

    Write-Host "Redis queue status..."
    Invoke-Redis @("ZCARD", "draw:queue:$ProductId")

    Write-Host "Draw DB product result..."
    Invoke-DrawMySql "SELECT id, name, total_quantity, remaining_quantity, price, status FROM products WHERE id = $ProductId;"

    Write-Host "Draw DB entry result summary..."
    Invoke-DrawMySql "SELECT result, status, fail_reason, COUNT(*) AS count FROM draw_entries GROUP BY result, status, fail_reason ORDER BY result, status, fail_reason;"

    Write-Host "Payment DB summary..."
    Invoke-PaymentMySql "SELECT COUNT(*) AS wallet_count FROM wallets; SELECT COUNT(*) AS payment_count FROM payment_transactions;"
}

Write-Host "Checking draw-service health..."
Wait-HttpOk "$DrawBaseUrl/api/v1/health"

Write-Host "Checking payment-service health..."
Wait-HttpOk "$PaymentBaseUrl/api/v1/health"

foreach ($totalRequests in $RequestSteps) {
    Write-Host ""
    Write-Host "============================================================"
    Write-Host "Load test start. totalRequests=$totalRequests"
    Write-Host "============================================================"

    Reset-DrawState
    Reset-PaymentState
    $productId = New-Product -TotalRequests $totalRequests
    Invoke-K6 -ProductId $productId -TotalRequests $totalRequests

    Write-Host "Waiting for queue worker. seconds=$WorkerWaitSeconds"
    Start-Sleep -Seconds $WorkerWaitSeconds

    Show-Results -ProductId $productId
}

Write-Host "Done."
