# BorrowHub Automated Smoke Test Suite
$ErrorActionPreference = "Continue"

# Load environment configuration from .env if present
$envFile = Join-Path $PSScriptRoot "..\..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -and -not $_.StartsWith('#') -and $_.Contains('=') } | ForEach-Object {
        $parts = $_.Split('=', 2)
        $k = $parts[0].Trim()
        $v = $parts[1].Trim()
        [Environment]::SetEnvironmentVariable($k, $v, 'Process')
        Set-Item "env:$k" $v
    }
}
$studentPassword = if ($env:BORROWHUB_SEED_STUDENT_PASSWORD) { $env:BORROWHUB_SEED_STUDENT_PASSWORD } else { "Student@123" }
$adminPassword = if ($env:BORROWHUB_SEED_ADMIN_PASSWORD) { $env:BORROWHUB_SEED_ADMIN_PASSWORD } else { "Admin@123" }

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing Live Smoke Test Suite against Running Services" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$frontendUrl = "http://localhost:5173"
$failures = 0
$totalTests = 0

function Run-SmokeTest {
    param(
        [string]$Name,
        [scriptblock]$TestBlock
    )
    $script:totalTests++
    Write-Host -NoNewline ("  [TEST $script:totalTests] $Name ... ")
    try {
        $result = & $TestBlock
        if ($result -eq $true -or $result -eq $null) {
            Write-Host "PASS" -ForegroundColor Green
        } else {
            Write-Host "FAIL" -ForegroundColor Red
            $script:failures++
        }
    } catch {
        Write-Host ("FAIL ($($_.Exception.Message))") -ForegroundColor Red
        $script:failures++
    }
}

# 1. Database & Actuator Health
Run-SmokeTest "Backend Actuator Health (/actuator/health)" {
    $resp = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 3
    return ($resp.status -eq "UP")
}

# 2. Frontend Web Server
Run-SmokeTest "Frontend Server Reachability ($frontendUrl)" {
    $resp = Invoke-WebRequest -Uri $frontendUrl -UseBasicParsing -TimeoutSec 3
    return ($resp.StatusCode -eq 200)
}

# 3. Public Catalog API
Run-SmokeTest "Public Catalog API (/api/resources)" {
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/resources" -Method Get -TimeoutSec 3
    return ($resp.success -eq $true -and $resp.data.content -ne $null)
}

# 4. Public Categories API
Run-SmokeTest "Categories API (/api/categories)" {
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/categories" -Method Get -TimeoutSec 3
    return ($resp.success -eq $true -and $resp.data.Count -gt 0)
}

# 5. Student Authentication
$global:studentToken = $null
Run-SmokeTest "Student Authentication (/api/auth/login)" {
    $body = @{ username = "student1"; password = $studentPassword } | ConvertTo-Json
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if ($resp.success -eq $true -and $resp.data.token) {
        $global:studentToken = $resp.data.token
        return $true
    }
    return $false
}

# 6. Authenticated User Profile
Run-SmokeTest "Authenticated User Profile (/api/auth/me)" {
    $headers = @{ Authorization = "Bearer $global:studentToken" }
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -Headers $headers
    return ($resp.success -eq $true -and $resp.data.username -eq "student1")
}

# 7. Admin Authentication
$global:adminToken = $null
Run-SmokeTest "Admin Authentication (/api/auth/login)" {
    $body = @{ username = "admin"; password = $adminPassword } | ConvertTo-Json
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if ($resp.success -eq $true -and $resp.data.token) {
        $global:adminToken = $resp.data.token
        return $true
    }
    return $false
}

# 8. Admin Endpoint Access Enforcement (Admin Allowed)
Run-SmokeTest "Admin Access to Pending Requests (/api/borrowings/requests/pending)" {
    $headers = @{ Authorization = "Bearer $global:adminToken" }
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/borrowings/requests/pending" -Method Get -Headers $headers
    return ($resp.success -eq $true)
}

# 9. Role-Based Authorization Guard (Student Denied Admin Access)
Run-SmokeTest "Role Guard: Student Denied Admin Access (/api/borrowings/requests/pending)" {
    $headers = @{ Authorization = "Bearer $global:studentToken" }
    try {
        $resp = Invoke-WebRequest -Uri "$baseUrl/api/borrowings/requests/pending" -Method Get -Headers $headers -UseBasicParsing
        return $false # Should not succeed
    } catch {
        return ($_.Exception.Response.StatusCode.value__ -eq 403)
    }
}

# 10. Security Guard: Unauthenticated Rejection
Run-SmokeTest "Security Guard: Unauthenticated Rejection (/api/borrowings/requests/my)" {
    try {
        $resp = Invoke-WebRequest -Uri "$baseUrl/api/borrowings/requests/my" -Method Get -UseBasicParsing
        return $false # Should not succeed
    } catch {
        return ($_.Exception.Response.StatusCode.value__ -eq 401)
    }
}

Write-Host "============================================================================" -ForegroundColor Cyan
if ($failures -eq 0) {
    Write-Host " [BorrowHub RESULT] SMOKE TESTS: ALL $totalTests CHECKS PASSED (100% SUCCESS)" -ForegroundColor Green
    exit 0
} else {
    Write-Host " [BorrowHub RESULT] SMOKE TESTS: $failures OF $totalTests FAILED" -ForegroundColor Red
    exit 1
}
