@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM BorrowHub - Master Automated Quality Assurance Suite Runner
REM ============================================================================

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

echo ============================================================================
echo [BorrowHub] Launching Master Full Quality Assurance Pipeline...
echo ============================================================================

powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\qa\run-full-qa.ps1"
set "QA_EXIT_CODE=%ERRORLEVEL%"

exit /b %QA_EXIT_CODE%
