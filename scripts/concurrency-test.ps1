param(
    [string]$BaseUrl = 'http://127.0.0.1:8080/api',
    [string]$Mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$User = 'root',
    [string]$Password = 'root',
    [int]$RoomTypeId = 1,
    [int]$DaysAhead = 60,
    [int]$Requests = 10
)

$ErrorActionPreference = 'Stop'
$date = (Get-Date).Date.AddDays($DaysAhead).ToString('yyyy-MM-dd')
$nextDate = (Get-Date).Date.AddDays($DaysAhead + 1).ToString('yyyy-MM-dd')

$inventoryExists = & $Mysql "--user=$User" "--password=$Password" -N -e "USE hotel_booking; SELECT COUNT(*) FROM room_inventory WHERE room_type_id=$RoomTypeId AND inventory_date='$date';"
if ([int]$inventoryExists -ne 1) { throw "No inventory row for room type $RoomTypeId on $date." }

& $Mysql "--user=$User" "--password=$Password" -e "USE hotel_booking; UPDATE room_inventory SET total_count=1, booked_count=0 WHERE room_type_id=$RoomTypeId AND inventory_date='$date';" | Out-Null

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType 'application/json' -Body '{"phone":"13900000001","password":"123456"}'
$headers = @{ Authorization = "Bearer $($login.data.token)" }
$body = @{
    contactName = '并发测试'
    contactPhone = '13900000001'
    checkInDate = $date
    checkOutDate = $nextDate
    remark = 'concurrency verification'
    items = @(@{ roomTypeId = $RoomTypeId; quantity = 1 })
} | ConvertTo-Json -Depth 8 -Compress

$results = 1..$Requests | ForEach-Object -Parallel {
    try {
        $response = Invoke-RestMethod -Method Post -Uri $using:BaseUrl/reservations -Headers $using:headers -ContentType 'application/json; charset=utf-8' -Body $using:body
        [pscustomobject]@{ code = $response.code; orderNo = $response.data.orderNo }
    } catch {
        [pscustomobject]@{ code = -1; orderNo = $null }
    }
} -ThrottleLimit $Requests

$successes = @($results | Where-Object { $_.code -eq 0 })
$outOfStock = @($results | Where-Object { $_.code -eq 4002 })
if ($successes.Count -ne 1) { throw "Expected exactly 1 successful order, got $($successes.Count)." }
if ($outOfStock.Count -ne ($Requests - 1)) { throw "Expected $($Requests - 1) out-of-stock responses, got $($outOfStock.Count)." }

$orderNo = $successes[0].orderNo
Invoke-RestMethod -Method Post -Uri "$BaseUrl/reservations/$orderNo/cancel" -Headers $headers | Out-Null

$remaining = & $Mysql "--user=$User" "--password=$Password" -N -e "USE hotel_booking; SELECT total_count - booked_count FROM room_inventory WHERE room_type_id=$RoomTypeId AND inventory_date='$date';"
if ([int]$remaining -ne 1) { throw "Inventory was not restored after cancellation. Remaining: $remaining" }

Write-Host "Concurrency test passed: $($successes.Count) success, $($outOfStock.Count) rejected, remaining inventory $remaining."
