# BorrowHub Rigorous Performance & Latency Benchmark with Statistical Percentiles
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

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing Performance & Latency Statistical Benchmark" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$benchmarks = @()

function Get-Percentile {
    param([double[]]$Values, [double]$Percentile)
    if ($Values.Count -eq 0) { return 0 }
    $sorted = $Values | Sort-Object
    $index = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $sorted.Count) { $index = $sorted.Count - 1 }
    return [Math]::Round($sorted[$index], 2)
}

function Measure-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [hashtable]$Headers = @{},
        [int]$Iterations = 50
    )
    Write-Host ("Benchmarking: $Name ($Iterations iterations)...")
    [double[]]$times = @()
    $errors = 0

    # Warmup
    try { Invoke-RestMethod -Uri $Url -Headers $Headers -TimeoutSec 3 | Out-Null } catch {}

    $swTotal = [System.Diagnostics.Stopwatch]::StartNew()
    for ($i = 0; $i -lt $Iterations; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $resp = Invoke-RestMethod -Uri $Url -Headers $Headers -TimeoutSec 5
            $sw.Stop()
            $times += [double]$sw.ElapsedMilliseconds
        } catch {
            $sw.Stop()
            $errors++
        }
    }
    $swTotal.Stop()

    $avg = if ($times.Count -gt 0) { [Math]::Round(($times | Measure-Object -Average).Average, 2) } else { 0 }
    $min = if ($times.Count -gt 0) { [Math]::Round(($times | Measure-Object -Minimum).Minimum, 2) } else { 0 }
    $max = if ($times.Count -gt 0) { [Math]::Round(($times | Measure-Object -Maximum).Maximum, 2) } else { 0 }
    $median = Get-Percentile -Values $times -Percentile 50
    $p95 = Get-Percentile -Values $times -Percentile 95
    $p99 = Get-Percentile -Values $times -Percentile 99
    $throughput = [Math]::Round($Iterations / ($swTotal.ElapsedMilliseconds / 1000), 2)
    $errorRate = [Math]::Round(($errors / $Iterations) * 100, 2)

    $obj = [PSCustomObject]@{
        Endpoint   = $Name
        Samples    = $Iterations
        AvgMs      = $avg
        MedianMs   = $median
        p95Ms      = $p95
        p99Ms      = $p99
        MaxMs      = $max
        MinMs      = $min
        Throughput = "$throughput req/s"
        ErrorRate  = "$errorRate%"
    }
    $script:benchmarks += $obj
    Write-Host ("  Avg: ${avg}ms | Median: ${median}ms | p95: ${p95}ms | p99: ${p99}ms | Max: ${max}ms | Errors: $errorRate%")
}

# 1. Health check latency
Measure-Endpoint "Actuator Health Check" "$baseUrl/actuator/health" @{} 50

# 2. Public Catalog listing latency
Measure-Endpoint "Public Resource Catalog" "$baseUrl/api/resources" @{} 50

# 3. Category tree latency
Measure-Endpoint "Category Hierarchy" "$baseUrl/api/categories" @{} 50

# 4. Authenticated profile latency
$loginBody = @{ username = "student1"; password = $studentPassword } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResp.data.token
$authHeaders = @{ Authorization = "Bearer $token" }
Measure-Endpoint "Authenticated User Profile" "$baseUrl/api/auth/me" $authHeaders 50

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub RESULT] STATISTICAL PERFORMANCE BENCHMARK SUMMARY:" -ForegroundColor Cyan
$benchmarks | Format-Table -AutoSize | Out-String | Write-Host

# Save to reports
$reportDir = Join-Path $PSScriptRoot "..\reports"
if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir -Force | Out-Null }
$reportFile = Join-Path $reportDir "performance-benchmark-results.txt"
$benchmarks | Format-Table -AutoSize | Out-File -FilePath $reportFile -Encoding utf8
Write-Host "Benchmark metrics recorded in: $reportFile"
Write-Host "============================================================================" -ForegroundColor Cyan
