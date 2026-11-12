# BorrowHub Full QA Automation Pipeline Orchestrator
param(
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Continue"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportDate = Get-Date -Format "yyyy-MM-dd HH:mm:ss K"

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $projectRoot "qa\reports"
}
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$reportPath = Join-Path $OutputDir "full-qa-report-$timestamp.md"

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing Master Quality Assurance Pipeline" -ForegroundColor Cyan
Write-Host " Start Time: $reportDate" -ForegroundColor Cyan
Write-Host " Report Destination: $reportPath" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$testResults = @()
$anyFailure = $false

function Execute-QAStep {
    param(
        [string]$StepId,
        [string]$Name,
        [scriptblock]$ExecutionBlock
    )
    Write-Host "`n>>> [$StepId] Executing: $Name ..." -ForegroundColor Yellow
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $status = "FAIL"
    $errorMsg = ""
    $output = ""

    try {
        $output = & $ExecutionBlock 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0 -or $LASTEXITCODE -eq $null) {
            $status = "PASS"
        } else {
            $status = "FAIL"
            $errorMsg = "Non-zero exit code ($LASTEXITCODE)"
            $script:anyFailure = $true
        }
    } catch {
        $status = "FAIL"
        $errorMsg = $_.Exception.Message
        $script:anyFailure = $true
    }
    $sw.Stop()
    $elapsedSec = [Math]::Round($sw.Elapsed.TotalSeconds, 2)

    $color = if ($status -eq "PASS") { "Green" } else { "Red" }
    Write-Host "[$status] $Name (${elapsedSec}s)" -ForegroundColor $color

    $script:testResults += [PSCustomObject]@{
        StepId    = $StepId
        Name      = $Name
        Status    = $status
        Duration  = "${elapsedSec}s"
        Error     = $errorMsg
        LogOutput = $output.Trim()
    }
}

# 1. Environment & Toolchains
Execute-QAStep "QA-01" "Environment & Dependencies Verification" {
    $j = & java -version 2>&1
    $m = & mvn -v 2>&1
    $n = & node -v 2>&1
    Write-Host $j
    Write-Host $m
    Write-Host $n
}

# 2. Database Migrations
Execute-QAStep "QA-02" "Database Migration & Schema Validation" {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $projectRoot "scripts\db-status.ps1")
}

# 3. Backend Test Suite (JUnit 5 / Spring Boot Test / AssertJ)
Execute-QAStep "QA-03" "Backend Test Suite (24 Architecture & Unit Tests)" {
    Push-Location (Join-Path $projectRoot "borrowhub-backend")
    try {
        & mvn test
    } finally {
        Pop-Location
    }
}

# 4. Backend Package / Fat JAR Build
Execute-QAStep "QA-04" "Backend Packaging & Executable JAR Compilation" {
    Push-Location (Join-Path $projectRoot "borrowhub-backend")
    try {
        & mvn package -DskipTests
    } finally {
        Pop-Location
    }
}

# 5. Frontend Unit & Component Tests (Vitest)
Execute-QAStep "QA-05" "Frontend Vitest & React Testing Library Suite" {
    Push-Location (Join-Path $projectRoot "borrowhub-frontend")
    try {
        & npm test
    } finally {
        Pop-Location
    }
}

# 6. Frontend Production Build (Vite)
Execute-QAStep "QA-06" "Frontend Production Build & Bundle Generation" {
    Push-Location (Join-Path $projectRoot "borrowhub-frontend")
    try {
        & npm run build
    } finally {
        Pop-Location
    }
}

# Ensure Application Services are UP before live testing stages (QA-07 through QA-12)
Write-Host "`n>>> Ensuring Application Services are Active for Live QA Stages..." -ForegroundColor Yellow
$beActive = $false
try {
    $h = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($h.status -eq "UP") { $beActive = $true }
} catch {}

$feActive = $false
try {
    $f = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($f.StatusCode -eq 200) { $feActive = $true }
} catch {}

if (-not $beActive -or -not $feActive) {
    Write-Host "  Starting BorrowHub services via scripts\start-borrowhub.bat..." -ForegroundColor Cyan
    & cmd.exe /c (Join-Path $projectRoot "scripts\start-borrowhub.bat")
    Start-Sleep -Seconds 3
}

# 7. Live Smoke Tests (REST API & Auth)
Execute-QAStep "QA-07" "Automated API & Operational Smoke Tests" {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $projectRoot "qa\smoke\smoke-test.ps1")
}

# 8. Playwright E2E Tests (Headless Chromium)
Execute-QAStep "QA-08" "Playwright End-to-End User Journeys (6 Journeys)" {
    Push-Location (Join-Path $projectRoot "borrowhub-frontend")
    try {
        & npm run test:e2e
    } finally {
        Pop-Location
    }
}

# 9. Application Security Audit
Execute-QAStep "QA-09" "Application Security & Vulnerability Audit" {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $projectRoot "qa\security\security-check.ps1")
}

# 10. Performance & Latency Benchmark
Execute-QAStep "QA-10" "API Performance & Throughput Benchmark" {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $projectRoot "qa\performance\perf-test.ps1")
}

# 11. Accessibility & Responsive Audit
Execute-QAStep "QA-11" "Accessibility (a11y) & Responsive Architecture Audit" {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $projectRoot "qa\accessibility\a11y-check.ps1")
}

# 12. Final System Health Check
Execute-QAStep "QA-12" "Final Multi-Tier System Health Check" {
    & cmd.exe /c (Join-Path $projectRoot "scripts\health-check.bat")
}

# Generate Markdown Quality Report
$md = @"
# BorrowHub (APPJFS12) — Automated Master Quality Assurance Report

**Execution Timestamp:** $reportDate  
**Overall Verdict:** $(if ($anyFailure) { "FAIL" } else { "PASS — VERIFIED" })  
**Architecture Baseline:** v2.4.8 (Issues C1–C50)  

---

## 1. Quality Pipeline Execution Summary

| Step ID | Test Group / Phase | Result | Duration | Notes |
|---|---|---|---|---|
"@

foreach ($res in $testResults) {
    $icon = if ($res.Status -eq "PASS") { "✅ PASS" } else { "❌ FAIL" }
    $md += "`n| **$($res.StepId)** | $($res.Name) | $icon | $($res.Duration) | $($res.Error) |"
}

$md += @"


---

## 2. Test Execution Details by Group

"@

foreach ($res in $testResults) {
    $md += @"
### $($res.StepId): $($res.Name)
- **Status:** $($res.Status)
- **Duration:** $($res.Duration)
$(if ($res.Error) { "- **Error Message:** " + $res.Error })

````text
$($res.LogOutput.Substring(0, [Math]::Min($res.LogOutput.Length, 1500)))
````

"@
}

$md += @"

---

## 3. Final Readiness Statement

This report documents real, empirical test executions conducted against the running BorrowHub platform.
$(if (-not $anyFailure) { "**VERDICT: PASS — VERIFIED.** All 12 quality assurance stages completed successfully with zero failures." } else { "**VERDICT: FAIL.** One or more quality gates encountered errors." })
"@

Set-Content -Path $reportPath -Value $md -Encoding utf8
Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub RESULT] FULL QA PIPELINE COMPLETE" -ForegroundColor Cyan
Write-Host " Report written to: $reportPath" -ForegroundColor Cyan
if ($anyFailure) {
    Write-Host " Final Verdict: FAIL (Errors Detected)" -ForegroundColor Red
    exit 1
} else {
    Write-Host " Final Verdict: PASS — VERIFIED (100% QA SUCCESS)" -ForegroundColor Green
    exit 0
}
