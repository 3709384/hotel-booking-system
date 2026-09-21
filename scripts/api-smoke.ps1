param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [int]$DaysAhead = 3
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding

function Invoke-Json {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [hashtable]$Headers = @{},
        $Body = $null
    )
    $params = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }
    Invoke-RestMethod @params
}

function Assert-Success {
    param($Response, [string]$Step)
    if ($null -eq $Response -or $Response.code -ne 0) {
        throw "$Step failed: $($Response | ConvertTo-Json -Depth 8 -Compress)"
    }
}

$checkIn = (Get-Date).Date.AddDays($DaysAhead).ToString("yyyy-MM-dd")
$checkOut = (Get-Date).Date.AddDays($DaysAhead + 2).ToString("yyyy-MM-dd")

Write-Host "[1/8] Public room types and availability"
$types = Invoke-Json -Method Get -Uri "$BaseUrl/room-types?page=1&size=10"
Assert-Success $types "room types"
$availability = Invoke-Json -Method Get -Uri "$BaseUrl/availability?checkIn=$checkIn&checkOut=$checkOut"
Assert-Success $availability "availability"
$selected = $availability.data | Sort-Object minAvailable -Descending | Select-Object -First 1
if ($null -eq $selected -or $selected.minAvailable -lt 1) {
    throw "No room inventory available for $checkIn to $checkOut"
}
if (-not $selected.coverImage -or -not $selected.description) {
    throw "Room type image or description is missing"
}
$reviews = Invoke-Json -Method Get -Uri "$BaseUrl/room-types/$($selected.roomTypeId)/reviews?page=1&size=10"
Assert-Success $reviews "room type reviews"
if ($reviews.data.total -lt 1) {
    throw "Room type reviews were not seeded"
}

Write-Host "[2/8] Member login"
$memberLogin = Invoke-Json -Method Post -Uri "$BaseUrl/auth/login" -Body @{
    phone = "13900000001"
    password = "123456"
}
Assert-Success $memberLogin "member login"
$memberHeaders = @{ Authorization = "Bearer $($memberLogin.data.token)" }

Write-Host "[3/8] Create reservation"
$create = Invoke-Json -Method Post -Uri "$BaseUrl/reservations" -Headers $memberHeaders -Body @{
    contactName = "王女士"
    contactPhone = "13900000001"
    checkInDate = $checkIn
    checkOutDate = $checkOut
    remark = "api smoke test"
    items = @(@{ roomTypeId = $selected.roomTypeId; quantity = 1 })
}
Assert-Success $create "create reservation"
$orderNo = $create.data.orderNo
if ($create.data.status -ne "PENDING_PAYMENT") {
    throw "Expected PENDING_PAYMENT, got $($create.data.status)"
}

Write-Host "[4/8] Start mock payment and callback"
$payment = Invoke-Json -Method Post -Uri "$BaseUrl/payments" -Headers $memberHeaders -Body @{
    orderNo = $orderNo
    channel = "WECHAT"
}
Assert-Success $payment "start payment"
$paymentNo = $payment.data.paymentNo
$transactionId = "SMOKE-$orderNo"
$callback = Invoke-Json -Method Post -Uri "$BaseUrl/payments/callback/WECHAT?paymentNo=$paymentNo&transactionId=$transactionId"
Assert-Success $callback "payment callback"

Write-Host "[5/8] Verify reservation and item id"
$detail = Invoke-Json -Method Get -Uri "$BaseUrl/reservations/$orderNo" -Headers $memberHeaders
Assert-Success $detail "reservation detail"
if ($detail.data.status -ne "CONFIRMED") {
    throw "Expected CONFIRMED, got $($detail.data.status)"
}
$itemId = $detail.data.items[0].itemId
if (-not $itemId) {
    throw "Reservation itemId is missing"
}

Write-Host "[6/8] Receptionist opens room board"
$frontLogin = Invoke-Json -Method Post -Uri "$BaseUrl/auth/login" -Body @{
    phone = "13700000001"
    password = "front123"
}
Assert-Success $frontLogin "receptionist login"
$frontHeaders = @{ Authorization = "Bearer $($frontLogin.data.token)" }
$board = Invoke-Json -Method Get -Uri "$BaseUrl/pms/rooms/board?floor=8" -Headers $frontHeaders
Assert-Success $board "room board"
$staffSearch = Invoke-Json -Method Get -Uri "$BaseUrl/reservations/search?status=CONFIRMED&page=1&size=20" -Headers $frontHeaders
Assert-Success $staffSearch "staff reservation search"
if (-not ($staffSearch.data.records | Where-Object { $_.orderNo -eq $orderNo })) {
    throw "Created order was not returned by staff reservation search"
}
$room = $board.data | Where-Object {
    $_.roomTypeId -eq $selected.roomTypeId -and $_.status -eq "VC"
} | Select-Object -First 1
if ($null -eq $room) {
    throw "No vacant room found for room type $($selected.roomTypeId)"
}

Write-Host "[7/8] Check in"
$checkInResult = Invoke-Json -Method Post -Uri "$BaseUrl/pms/reservations/$orderNo/check-in" -Headers $frontHeaders -Body @{
    assignments = @(@{ itemId = $itemId; roomId = $room.roomId })
}
Assert-Success $checkInResult "check in"
if ($checkInResult.data.status -ne "CHECKED_IN") {
    throw "Expected CHECKED_IN, got $($checkInResult.data.status)"
}

Write-Host "[8/8] Check out"
$checkOutResult = Invoke-Json -Method Post -Uri "$BaseUrl/pms/reservations/$orderNo/check-out" -Headers $frontHeaders
Assert-Success $checkOutResult "check out"
if ($checkOutResult.data.status -ne "COMPLETED") {
    throw "Expected COMPLETED, got $($checkOutResult.data.status)"
}

Write-Host "API smoke test passed."
Write-Host "Order: $orderNo"
Write-Host "Room type: $($selected.name), stay: $checkIn -> $checkOut"
Write-Host "Room: $($room.roomNo)"
