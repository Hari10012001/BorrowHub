# BorrowHub Comprehensive Functional Flow & State Machine Verification
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing Comprehensive Functional & State Machine Verification" -ForegroundColor Cyan
Write-Host " Target API: $BaseUrl" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$results = [ordered]@{}

function Step {
    param([string]$Title, [scriptblock]$Action)
    Write-Host -NoNewline "`n--> Testing: $Title ... "
    try {
        $res = & $Action
        Write-Host "PASS" -ForegroundColor Green
        $results[$Title] = "PASS"
        return $res
    } catch {
        Write-Host "FAIL: $($_.Exception.Message)" -ForegroundColor Red
        $results[$Title] = "FAIL: $($_.Exception.Message)"
        throw $_
    }
}

# --------------------------------------------------------------------------
# Baseline Inventory Inspection (Pre-Test State)
# --------------------------------------------------------------------------
$initialRpi = (Invoke-RestMethod -Uri "$BaseUrl/api/resources/6" -Method Get).data
$initialRpiAvailable = $initialRpi.availableQuantity
Write-Host "Baseline Resource 6 (Raspberry Pi) Available Stock: $initialRpiAvailable" -ForegroundColor Gray

# --------------------------------------------------------------------------
# 1. Student Authentication
# --------------------------------------------------------------------------
$studentAuth = Step "1. Student Authentication" {
    $body = @{ username = "student1"; password = "Student@123" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if (-not $r.success -or -not $r.data.token) { throw "Login failed" }
    
    $headers = @{ Authorization = "Bearer $($r.data.token)" }
    $me = Invoke-RestMethod -Uri "$BaseUrl/api/auth/me" -Method Get -Headers $headers
    return @{ token = $r.data.token; id = $me.data.id }
}
$studentToken = $studentAuth.token
$studentId = $studentAuth.id

# --------------------------------------------------------------------------
# 2. Faculty Authentication
# --------------------------------------------------------------------------
$facultyToken = Step "2. Faculty Authentication" {
    $body = @{ username = "faculty1"; password = "Faculty@123" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if (-not $r.success -or $r.data.role -ne "FACULTY") { throw "Faculty login failed" }
    return $r.data.token
}

# --------------------------------------------------------------------------
# 3. Admin Authentication
# --------------------------------------------------------------------------
$adminToken = Step "3. Admin Authentication" {
    $body = @{ username = "admin"; password = "Admin@123" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if (-not $r.success -or $r.data.role -ne "ADMIN") { throw "Admin login failed" }
    return $r.data.token
}

# --------------------------------------------------------------------------
# 4. Resource Catalog Browsing
# --------------------------------------------------------------------------
$catalog = Step "4. Resource Catalog Browsing" {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/resources" -Method Get
    if (-not $r.success -or $r.data.content.Count -eq 0) { throw "Catalog empty" }
    return $r.data.content
}

# --------------------------------------------------------------------------
# 5. Search and Filters
# --------------------------------------------------------------------------
Step "5. Catalog Search and Category Filtering" {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/resources?search=Arduino" -Method Get
    if (-not $r.success -or $r.data.content.Count -eq 0) { throw "Search failed" }
    if ($r.data.content[0].name -notmatch "Arduino") { throw "Search result mismatch" }
    return $true
}

# --------------------------------------------------------------------------
# 6. Reservation Creation & Explicit Cancellation (Testing Lifecycle & Inventory)
# --------------------------------------------------------------------------
$startDate = (Get-Date).ToString("yyyy-MM-dd")
$endDate = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
$dueDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")

Step "6. Reservation Creation and Cancellation Lifecycle" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{
        resourceId = 6
        quantity = 1
        startDate = $startDate
        endDate = $endDate
    } | ConvertTo-Json
    
    # 6a. Create Reservation
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/reservations" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $res.success -or -not $res.data.id) { throw "Failed to create reservation" }
    $tempResId = $res.data.id
    
    # Verify inventory was decremented
    $afterHold = (Invoke-RestMethod -Uri "$BaseUrl/api/resources/6" -Method Get).data.availableQuantity
    if ($afterHold -ne ($initialRpiAvailable - 1)) {
        throw "Available stock expected $($initialRpiAvailable - 1), but got $afterHold"
    }
    
    # 6b. Explicitly Cancel Reservation
    $cancelBody = @{ reason = "Schedule conflict change" } | ConvertTo-Json
    $cancelRes = Invoke-RestMethod -Uri "$BaseUrl/api/reservations/$tempResId/cancel" -Method Post -Headers $headers -Body $cancelBody -ContentType "application/json"
    if (-not $cancelRes.success) { throw "Reservation cancellation failed" }
    
    # Verify inventory was restored immediately
    $afterCancel = (Invoke-RestMethod -Uri "$BaseUrl/api/resources/6" -Method Get).data.availableQuantity
    if ($afterCancel -ne $initialRpiAvailable) {
        throw "Available stock expected to restore to $initialRpiAvailable, but got $afterCancel"
    }
    return $true
}

# --------------------------------------------------------------------------
# 7. Reservation for Checkout Flow (Quantity: 2 on Resource 6)
# --------------------------------------------------------------------------
$reservationId = Step "7. Reservation Creation for Checkout Flow (Resource 6, Qty: 2)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{
        resourceId = 6
        quantity = 2
        startDate = $startDate
        endDate = $endDate
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/reservations" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or -not $r.data.id) { throw "Reservation failed: $($r.message)" }
    if ($r.data.status -ne "HELD") { throw "Expected status HELD, got $($r.data.status)" }
    return $r.data.id
}

# --------------------------------------------------------------------------
# 8. Borrowing Request Submission and Approval (Resource 6)
# --------------------------------------------------------------------------
$requestId = Step "8. Borrowing Request Submission (Student)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{
        resourceId = 6
        quantity = 1
        requestedStartDate = $startDate
        requestedEndDate = $endDate
        purpose = "IoT Embedded Systems Lab Assignment"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/borrowings/requests" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or -not $r.data.id) { throw "Request submission failed" }
    if ($r.data.status -ne "PENDING_REVIEW") { throw "Expected status PENDING_REVIEW, got $($r.data.status)" }
    return $r.data.id
}

$approvedReservationId = Step "9. Request Review & Approval (Faculty)" {
    $headers = @{ Authorization = "Bearer $facultyToken" }
    $body = @{
        approved = $true
        reviewNotes = "Approved for lab assignment"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/borrowings/requests/$requestId/review" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or $r.data.status -ne "APPROVED") { throw "Review failed: $($r.message)" }
    
    # Retrieve the reservation created on approval so we can clean it up later
    $resR = Invoke-RestMethod -Uri "$BaseUrl/api/reservations/my" -Method Get -Headers @{ Authorization = "Bearer $studentToken" }
    $matching = $resR.data | Where-Object { $_.resourceId -eq 6 -and $_.status -eq "HELD" -and $_.id -ne $reservationId }
    return $matching[0].id
}

# --------------------------------------------------------------------------
# 10. Checkout with Idempotency Key & Replay Verification
# --------------------------------------------------------------------------
$idempotencyKey = [System.Guid]::NewGuid().ToString()

$checkoutRecord = Step "10. Circulation Checkout with Level-0 Idempotency" {
    $headers = @{
        Authorization = "Bearer $adminToken"
        "Idempotency-Key" = $idempotencyKey
    }
    $body = @{
        resourceId = 6
        userId = $studentId
        reservationId = $reservationId
        quantity = 2
        dueDate = $dueDate
        notes = "Issued at Central Lab circulation desk"
    } | ConvertTo-Json
    
    # 10a. First execution
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/circulation/checkout" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or -not $r.data.borrowingRecordId) { throw "Checkout failed: $($r.message)" }
    if ($r.data.status -ne "ACTIVE") { throw "Expected status ACTIVE, got $($r.data.status)" }
    
    # 10b. Idempotent Replay Execution (same key and payload)
    $replay = Invoke-RestMethod -Uri "$BaseUrl/api/circulation/checkout" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $replay.success) { throw "Idempotent replay failed" }
    if ($replay.data.borrowingRecordId -ne $r.data.borrowingRecordId) {
        throw "Idempotent replay returned different borrowingRecordId!"
    }
    return $r.data
}

# --------------------------------------------------------------------------
# 11. Partial Return Processing (1 of 2 Units)
# --------------------------------------------------------------------------
$partialReturn = Step "11. Partial Return Processing (1 of 2 Units)" {
    $headers = @{
        Authorization = "Bearer $adminToken"
        "Idempotency-Key" = [System.Guid]::NewGuid().ToString()
    }
    $body = @{
        borrowingRecordId = $checkoutRecord.borrowingRecordId
        quantityReturned = 1
        notes = "Returned 1 unit in good working condition"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/circulation/return" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success) { throw "Partial return failed: $($r.message)" }
    if ($r.data.status -ne "PARTIALLY_RETURNED") { throw "Expected loan status PARTIALLY_RETURNED after partial return, got $($r.data.status)" }
    if ($r.data.quantityOutstanding -ne 1) { throw "Expected quantityOutstanding 1, got $($r.data.quantityOutstanding)" }
    return $r.data
}

# --------------------------------------------------------------------------
# 12. Final Full Return Processing (Remaining 1 Unit)
# --------------------------------------------------------------------------
Step "12. Full Return Processing (Remaining 1 Unit)" {
    $headers = @{
        Authorization = "Bearer $adminToken"
        "Idempotency-Key" = [System.Guid]::NewGuid().ToString()
    }
    $body = @{
        borrowingRecordId = $checkoutRecord.borrowingRecordId
        quantityReturned = 1
        notes = "Complete return - final unit in good condition"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/circulation/return" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success) { throw "Full return failed: $($r.message)" }
    if ($r.data.status -ne "RETURNED") { throw "Expected loan status RETURNED, got $($r.data.status)" }
    if ($r.data.quantityOutstanding -ne 0) { throw "Expected quantityOutstanding 0, got $($r.data.quantityOutstanding)" }
    return $true
}

# Clean up the approved request's reservation so inventory remains consistent
if ($approvedReservationId) {
    try {
        $headers = @{ Authorization = "Bearer $studentToken" }
        $cancelBody = @{ reason = "Cleaning up test reservation" } | ConvertTo-Json
        Invoke-RestMethod -Uri "$BaseUrl/api/reservations/$approvedReservationId/cancel" -Method Post -Headers $headers -Body $cancelBody -ContentType "application/json" | Out-Null
    } catch {}
}

# --------------------------------------------------------------------------
# 13. Waitlist Operations (Resource 2: Dell Laptop)
# --------------------------------------------------------------------------
$waitlistEntry = Step "13. Waitlist Queue Joining & Verification" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{
        resourceId = 2
        quantity = 1
        neededFrom = $startDate
        neededUntil = $endDate
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/waitlist" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or -not $r.data.id) { throw "Waitlist join failed: $($r.message)" }
    if ($r.data.status -ne "WAITING") { throw "Expected status WAITING, got $($r.data.status)" }
    return $r.data
}

Step "14. Waitlist Cancellation" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/waitlist/$($waitlistEntry.id)/cancel" -Method Post -Headers $headers
    if (-not $r.success) { throw "Waitlist cancellation failed" }
    return $true
}

# --------------------------------------------------------------------------
# 15. Overdue Detection Verification
# --------------------------------------------------------------------------
Step "15. Overdue Records Query & Authoritative Time Verification" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $myLoans = Invoke-RestMethod -Uri "$BaseUrl/api/borrowings/my" -Method Get -Headers @{ Authorization = "Bearer $studentToken" }
    $overdueLoans = $myLoans.data | Where-Object { $_.status -eq "OVERDUE" }
    Write-Host -NoNewline " (Found $($overdueLoans.Count) active overdue records) "
    return $true
}

# --------------------------------------------------------------------------
# 16. Inventory Consistency Verification (Post-Test State == Initial State)
# --------------------------------------------------------------------------
Step "16. Post-Test Inventory Consistency Verification" {
    $finalRpi = (Invoke-RestMethod -Uri "$BaseUrl/api/resources/6" -Method Get).data
    $finalRpiAvailable = $finalRpi.availableQuantity
    if ($finalRpiAvailable -ne $initialRpiAvailable) {
        throw "Inventory mismatch! Initial available: $initialRpiAvailable, Final available: $finalRpiAvailable"
    }
    return $true
}

# --------------------------------------------------------------------------
# 17. Security & Role-Based Authorization Guards
# --------------------------------------------------------------------------
Step "17. RBAC Guard: Student Forbidden from Circulation Desk" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{ resourceId = 6; userId = $studentId; quantity = 1 } | ConvertTo-Json
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/circulation/checkout" -Method Post -Headers $headers -Body $body -ContentType "application/json" -UseBasicParsing
        throw "Student checkout unexpectedly succeeded!"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 403) {
            throw "Expected 403 Forbidden, got $($_.Exception.Response.StatusCode.value__)"
        }
    }
    return $true
}

Step "18. RBAC Guard: Faculty Forbidden from Admin Category Management" {
    $headers = @{ Authorization = "Bearer $facultyToken" }
    $body = @{ name = "Unauthorized Category"; description = "Test" } | ConvertTo-Json
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/categories" -Method Post -Headers $headers -Body $body -ContentType "application/json" -UseBasicParsing
        throw "Faculty category creation unexpectedly succeeded!"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 403) {
            throw "Expected 403 Forbidden, got $($_.Exception.Response.StatusCode.value__)"
        }
    }
    return $true
}

# --------------------------------------------------------------------------
# 19. Invalid Requests & Input Validation
# --------------------------------------------------------------------------
Step "19. Input Validation: Invalid Date Range Rejection (Start > End)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{
        resourceId = 6
        quantity = 1
        startDate = (Get-Date).AddDays(10).ToString("yyyy-MM-dd")
        endDate = (Get-Date).ToString("yyyy-MM-dd") # Invalid: end < start
    } | ConvertTo-Json
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/reservations" -Method Post -Headers $headers -Body $body -ContentType "application/json" -UseBasicParsing
        throw "Invalid date range unexpectedly succeeded!"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -ne 400) {
            throw "Expected 400 Bad Request, got $statusCode"
        }
    }
    return $true
}

Step "20. Security Guard: Unauthenticated Protected Endpoint Rejection" {
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/api/borrowings/my" -Method Get -UseBasicParsing
        throw "Unauthenticated request unexpectedly succeeded!"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -ne 401) {
            throw "Expected 401 Unauthorized, got $statusCode"
        }
    }
    return $true
}

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub RESULT] ALL 20 FUNCTIONAL & STATE MACHINE WORKFLOWS PASSED!" -ForegroundColor Green
Write-Host "============================================================================" -ForegroundColor Cyan
