# BorrowHub Safe Performance & Latency Benchmark
$ErrorActionPreference = "Continue"

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing Performance & API Latency Benchmark" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$benchmarks = @()

function Measure-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [hashtable]$Headers = @{},
        [int]$Iterations = 25
    )
    Write-Host ("Benchmarking: $Name ($Iterations iterations)...")
    $times = @()
    $errors = 0

    # Warmup
    try { Invoke-RestMethod -Uri $Url -Headers $Headers -TimeoutSec 3 | Out-Null } catch {}

    $swTotal = [System.Diagnostics.Stopwatch]::StartNew()
    for ($i = 0; $i -lt $Iterations; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $resp = Invoke-RestMethod -Uri $Url -Headers $Headers -TimeoutSec 5
            $sw.Stop()
            $times += $sw.ElapsedMilliseconds
        } catch {
            $sw.Stop()
            $errors++
        }
    }
    $swTotal.Stop()

    $avg = if ($times.Count -gt 0) { [Math]::Round(($times | Measure-Object -Average).Average, 2) } else { 0 }
    $min = if ($times.Count -gt 0) { ($times | Measure-Object -Minimum).Minimum } else { 0 }
    $max = if ($times.Count -gt 0) { ($times | Measure-Object -Maximum).Maximum } else { 0 }
    $throughput = [Math]::Round($Iterations / ($swTotal.ElapsedMilliseconds / 1000), 2)
    $errorRate = [Math]::Round(($errors / $Iterations) * 100, 2)

    $obj = [PSCustomObject]@{
        Endpoint   = $Name
        Samples    = $Iterations
        AvgMs      = $avg
        MinMs      = $min
        MaxMs      = $max
        Throughput = "$throughput req/s"
        ErrorRate  = "$errorRate%"
    }
    $script:benchmarks += $obj
    Write-Host ("  Avg: ${avg}ms | Min: ${min}ms | Max: ${max}ms | Throughput: $throughput req/s | Errors: $errorRate%")
}

# 1. Health check latency
Measure-Endpoint "Actuator Health Check" "$baseUrl/actuator/health" @{} 30

# 2. Public Catalog listing latency
Measure-Endpoint "Public Resource Catalog" "$baseUrl/api/resources" @{} 30

# 3. Category tree latency
Measure-Endpoint "Category Hierarchy" "$baseUrl/api/categories" @{} 25

# 4. Authenticated profile latency
$loginBody = @{ username = "student1"; password = "Student@123" } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResp.data.token
$authHeaders = @{ Authorization = "Bearer $token" }
Measure-Endpoint "Authenticated User Profile" "$baseUrl/api/auth/me" $authHeaders 25

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub RESULT] PERFORMANCE BENCHMARK SUMMARY:" -ForegroundColor Cyan
$benchmarks | Format-Table -AutoSize | Out-String | Write-Host

# Save to reports
$reportDir = Join-Path $PSScriptRoot "..\reports"
if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir -Force | Out-Null }
$reportFile = Join-Path $reportDir "performance-benchmark-results.txt"
$benchmarks | Format-Table -AutoSize | Out-File -FilePath $reportFile -Encoding utf8
Write-Host "Benchmark metrics recorded in: $reportFile"
Write-Host "============================================================================" -ForegroundColor Cyan
