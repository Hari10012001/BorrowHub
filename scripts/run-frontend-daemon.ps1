# BorrowHub Frontend Daemon Launcher
$feDir = Join-Path $PSScriptRoot "..\borrowhub-frontend"
$pidFile = Join-Path $PSScriptRoot "..\runtime\frontend.pid"

$proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm run dev" -WorkingDirectory $feDir -PassThru
$proc.Id | Out-File -FilePath $pidFile -Encoding ascii
Write-Host "Frontend started with PID $($proc.Id)"
$proc.WaitForExit()
