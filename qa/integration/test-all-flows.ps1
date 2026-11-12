# BorrowHub Comprehensive Functional Flow & State Machine Verification
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing End-to-End Application Functional Verification" -ForegroundColor Cyan
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

# 1. Student Login
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

# 2. Faculty Login
$facultyToken = Step "2. Faculty Authentication" {
    $body = @{ username = "faculty1"; password = "Faculty@123" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if (-not $r.success -or $r.data.role -ne "FACULTY") { throw "Faculty login failed" }
    return $r.data.token
}

# 3. Admin Login
$adminToken = Step "3. Admin Authentication" {
    $body = @{ username = "admin"; password = "Admin@123" } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if (-not $r.success -or $r.data.role -ne "ADMIN") { throw "Admin login failed" }
    return $r.data.token
}

# 4. Resource Browsing
$catalog = Step "4. Resource Catalog Browsing" {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/resources" -Method Get
    if (-not $r.success -or $r.data.content.Count -eq 0) { throw "Catalog empty" }
    return $r.data.content
}

# 5. Search and Filters
Step "5. Catalog Search and Category Filtering" {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/resources?search=Arduino" -Method Get
    if (-not $r.success -or $r.data.content.Count -eq 0) { throw "Search failed" }
    if ($r.data.content[0].name -notmatch "Arduino") { throw "Search result mismatch" }
    return $true
}

# 6. Direct Reservation Creation (Quantity Resource 6: Raspberry Pi, Quantity: 2)
$startDate = (Get-Date).ToString("yyyy-MM-dd")
$endDate = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
$dueDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")

$reservationId = Step "6. Reservation Creation (Resource 6 Quantity Mode)" {
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

# 7. Borrowing Request Submission and Approval (Resource 4: Sony Camera)
$requestId = Step "7. Borrowing Request Submission (Student on Resource 4)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{
        resourceId = 4
        quantity = 1
        requestedStartDate = $startDate
        requestedEndDate = $endDate
        purpose = "Department Seminar Recording"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/borrowings/requests" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or -not $r.data.id) { throw "Request submission failed" }
    if ($r.data.status -ne "PENDING_REVIEW") { throw "Expected status PENDING_REVIEW, got $($r.data.status)" }
    return $r.data.id
}

Step "8. Request Review & Approval (Faculty on Resource 4)" {
    $headers = @{ Authorization = "Bearer $facultyToken" }
    $body = @{
        approved = $true
        reviewNotes = "Approved for seminar coverage"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/borrowings/requests/$requestId/review" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or $r.data.status -ne "APPROVED") { throw "Review failed: $($r.message)" }
    return $true
}

# 9. Checkout with Idempotency Key (Resource 6 Reservation Checkout)
$checkoutRecord = Step "9. Circulation Checkout (Admin with Idempotency)" {
    $headers = @{
        Authorization = "Bearer $adminToken"
        "Idempotency-Key" = [System.Guid]::NewGuid().ToString()
    }
    $body = @{
        resourceId = 6
        userId = $studentId # student1
        reservationId = $reservationId
        quantity = 2
        dueDate = $dueDate
        notes = "Issued at Central Lab circulation desk"
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/circulation/checkout" -Method Post -Headers $headers -Body $body -ContentType "application/json"
    if (-not $r.success -or -not $r.data.borrowingRecordId) { throw "Checkout failed: $($r.message)" }
    if ($r.data.status -ne "ACTIVE") { throw "Expected status ACTIVE, got $($r.data.status)" }
    return $r.data
}

# 10. Partial Return (Return 1 of 2 units of Resource 6)
$partialReturn = Step "10. Partial Return Processing (1 of 2 Units)" {
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

# 11. Final Full Return (Return remaining 1 unit of Resource 6)
Step "11. Full Return Processing (Remaining 1 Unit)" {
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

# 12. Waitlist Operations (Resource 2: Dell laptop)
$waitlistEntry = Step "12. Waitlist Queue Joining & Verification" {
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

Step "13. Waitlist Cancellation" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/waitlist/$($waitlistEntry.id)/cancel" -Method Post -Headers $headers
    if (-not $r.success) { throw "Waitlist cancellation failed" }
    return $true
}

# 14. Authorization Guards
Step "14. Authorization Restrictions (Student Denied Circulation Desk)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $body = @{ resourceId = 5; userId = $studentId; quantity = 1 } | ConvertTo-Json
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

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub RESULT] ALL 14 FUNCTIONAL JOURNEYS PASSED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "============================================================================" -ForegroundColor Cyan
