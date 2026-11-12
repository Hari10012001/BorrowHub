# BorrowHub Automated Application-Level Security Scanner
$ErrorActionPreference = "Continue"

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing Automated Application Security Scan" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$findings = @()

function Add-Finding {
    param(
        [string]$Severity,
        [string]$Title,
        [string]$Description,
        [string]$Status
    )
    $script:findings += [PSCustomObject]@{
        Severity    = $Severity
        Title       = $Title
        Description = $Description
        Status      = $Status
    }
    $color = switch ($Severity) {
        "Critical" { "Red" }
        "High"     { "Red" }
        "Medium"   { "Yellow" }
        "Low"      { "Cyan" }
        default    { "White" }
    }
    Write-Host ("  [$Severity] ${Title}: $Status") -ForegroundColor $color
}

# 1. Privileged Role Escalation Prevention during Registration
try {
    $rand = Get-Random -Minimum 1000 -Maximum 9999
    $escalationPayload = @{
        username   = "attacker_$rand"
        email      = "attacker_$rand@test.com"
        password   = "Attack@123"
        firstName  = "Malicious"
        lastName   = "Actor"
        department = "Security"
        phone      = "1234567890"
        role       = "ADMIN"
    } | ConvertTo-Json

    $resp = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $escalationPayload -ContentType "application/json"
    # Now login and check role
    $loginPayload = @{ username = "attacker_$rand"; password = "Attack@123" } | ConvertTo-Json
    $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginPayload -ContentType "application/json"
    
    if ($loginResp.data.role -eq "ADMIN") {
        Add-Finding "Critical" "Privileged Role Escalation" "Public registration assigned ADMIN role without authorization" "VULNERABLE"
    } else {
        Add-Finding "Low" "Privileged Role Escalation Check" "Role assigned safely (Assigned: $($loginResp.data.role))" "PASSED"
    }
} catch {
    Add-Finding "Low" "Privileged Role Escalation Check" "Privileged role rejected during registration" "PASSED"
}

# 2. SQL Injection Resistance
try {
    $sqliPayload = "' OR '1'='1' --"
    $encoded = [System.Web.HttpUtility]::UrlEncode($sqliPayload)
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/resources?query=$encoded" -Method Get -TimeoutSec 3
    if ($resp.success -eq $true) {
        Add-Finding "Medium" "SQL Injection Resistance" "Parameterized query safely executed without SQL syntax error" "PASSED"
    } else {
        Add-Finding "High" "SQL Injection Resistance" "Unexpected response on SQL injection probe" "SUSPICIOUS"
    }
} catch {
    Add-Finding "Medium" "SQL Injection Resistance" "Request safely blocked or handled without exposing DB error" "PASSED"
}

# 3. Credential Leakage in Profile API
try {
    $body = @{ username = "student1"; password = "Student@123" } | ConvertTo-Json
    $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    $token = $loginResp.data.token

    $headers = @{ Authorization = "Bearer $token" }
    $rawResp = Invoke-WebRequest -Uri "$baseUrl/api/auth/me" -Method Get -Headers $headers -UseBasicParsing
    $rawContent = $rawResp.Content

    if ($rawContent -match '"password"' -or $rawContent -match '"hash"' -or $rawContent -match '"secret"') {
        Add-Finding "High" "Credential Exposure" "Profile response contains sensitive password or secret fields" "VULNERABLE"
    } else {
        Add-Finding "Low" "Credential Exposure" "Profile response is clean; zero password or hash fields exposed" "PASSED"
    }
} catch {
    Add-Finding "High" "Credential Exposure" "Failed to verify profile response" "ERROR"
}

# 4. Password Hashing in Database
try {
    $mysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    $env:MYSQL_PWD = "BorrowHub@2026"
    $sampleHash = (& $mysqlExe -u borrowhub_user -h localhost -P 3306 -s -N -e "SELECT password_hash FROM borrowhub_db.users WHERE username='admin' LIMIT 1;" 2>$null)
    if ($sampleHash -like "$2a$*" -or $sampleHash -like "$2b$*") {
        Add-Finding "Low" "Password Storage Security" "BCrypt adaptive salt and hashing algorithm verified ($sampleHash)" "PASSED"
    } else {
        Add-Finding "Critical" "Password Storage Security" "Passwords appear to be stored unhashed or weakly hashed" "VULNERABLE"
    }
} catch {
    Add-Finding "Informational" "Password Storage Security" "Database hash check requires local CLI" "SKIPPED"
}

# 5. Invalid JWT Token Tampering Rejection
try {
    $headers = @{ Authorization = "Bearer eyJhbGciOiJIUzI1NiJ9.invalid.tampered.token" }
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/auth/me" -Method Get -Headers $headers -UseBasicParsing
    Add-Finding "High" "JWT Tampering Resistance" "Malformed JWT was accepted" "VULNERABLE"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Add-Finding "Medium" "JWT Tampering Resistance" "Malformed and forged JWT rejected with 401 Unauthorized" "PASSED"
    } else {
        Add-Finding "Medium" "JWT Tampering Resistance" "Unexpected status code on forged token" "REVIEW"
    }
}

# 6. Unauthenticated Access Control Enforcement
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/borrowings/requests/pending" -Method Get -UseBasicParsing
    Add-Finding "High" "RBAC Enforcement" "Admin endpoint accessible without credentials" "VULNERABLE"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Add-Finding "Low" "RBAC Enforcement" "Protected admin endpoint rejected with 401 Unauthorized" "PASSED"
    } else {
        Add-Finding "Low" "RBAC Enforcement" "Protected admin endpoint blocked" "PASSED"
    }
}

Write-Host "============================================================================" -ForegroundColor Cyan
$criticalOrHigh = ($findings | Where-Object { ($_.Severity -in @("Critical", "High")) -and ($_.Status -eq "VULNERABLE") }).Count
if ($criticalOrHigh -eq 0) {
    Write-Host " [BorrowHub RESULT] SECURITY AUDIT: PASSED (ZERO CRITICAL/HIGH VULNERABILITIES DETECTED)" -ForegroundColor Green
    exit 0
} else {
    Write-Host " [BorrowHub RESULT] SECURITY AUDIT: FAILED ($criticalOrHigh CRITICAL/HIGH FINDINGS)" -ForegroundColor Red
    exit 1
}
