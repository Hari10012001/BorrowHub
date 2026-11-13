# BorrowHub Backend Daemon Launcher
$envFile = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -and -not $_.StartsWith('#') -and $_.Contains('=') } | ForEach-Object {
        $parts = $_.Split('=', 2)
        $k = $parts[0].Trim()
        $v = $parts[1].Trim()
        [Environment]::SetEnvironmentVariable($k, $v, 'Process')
        Set-Item "env:$k" $v
    }
}

$jarPath = Join-Path $PSScriptRoot "..\runtime\borrowhub-backend.jar"
$pidFile = Join-Path $PSScriptRoot "..\runtime\backend.pid"

$proc = Start-Process -FilePath "java" -ArgumentList "-Duser.timezone=Asia/Kolkata", "-jar", "`"$jarPath`"" -PassThru
$proc.Id | Out-File -FilePath $pidFile -Encoding ascii
Write-Host "Backend started with PID $($proc.Id)"
$proc.WaitForExit()
