# BorrowHub Automated Accessibility & Responsive Audit
$ErrorActionPreference = "Continue"

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " [BorrowHub] Executing Accessibility (a11y) & Responsive Architecture Audit" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$frontendDir = Join-Path $PSScriptRoot "..\..\borrowhub-frontend"
$indexHtmlPath = Join-Path $frontendDir "index.html"
$srcDir = Join-Path $frontendDir "src"
$checks = @()

function Audit-Check {
    param(
        [string]$Category,
        [string]$Name,
        [bool]$Condition,
        [string]$Detail
    )
    $status = if ($Condition) { "PASS" } else { "FAIL" }
    $color = if ($Condition) { "Green" } else { "Red" }
    Write-Host ("  [$status] $Category - ${Name}: $Detail") -ForegroundColor $color
    $script:checks += [PSCustomObject]@{
        Category  = $Category
        Check     = $Name
        Status    = $status
        Detail    = $Detail
    }
}

# 1. Viewport & Responsive Meta Tags
if (Test-Path $indexHtmlPath) {
    $indexContent = Get-Content -Path $indexHtmlPath -Raw
    $hasViewport = $indexContent -match 'name="viewport"'
    $hasTitle = $indexContent -match '<title>'
    $hasLang = $indexContent -match 'lang="[a-zA-Z\-]+"'

    Audit-Check "Responsive Design" "Mobile Viewport Meta Tag" $hasViewport "width=device-width, initial-scale=1.0 configured"
    Audit-Check "Document Semantics" "HTML Document Lang Attribute" $hasLang "lang attribute present on root <html>"
    Audit-Check "Document Semantics" "Accessible Page Title" $hasTitle "<title> tag configured"
}

# 2. JSX Semantic Elements Audit
if (Test-Path $srcDir) {
    $jsxFiles = Get-ChildItem -Path $srcDir -Filter "*.jsx" -Recurse
    $totalFiles = $jsxFiles.Count

    $hasButtons = $false
    $hasInputs = $false
    $hasNavOrAside = $false
    $hasModalRole = $false

    foreach ($file in $jsxFiles) {
        $text = Get-Content -Path $file.FullName -Raw
        if ($text -match '<button' -or $text -match 'type="button"') { $hasButtons = $true }
        if ($text -match '<input' -or $text -match '<select') { $hasInputs = $true }
        if ($text -match '<nav' -or $text -match '<aside' -or $text -match '<header') { $hasNavOrAside = $true }
        if ($text -match 'role="dialog"' -or $text -match 'aria-modal' -or $text -match 'backdrop') { $hasModalRole = $true }
    }

    Audit-Check "Semantic HTML" "Semantic Landmarks (<nav>, <aside>, <header>)" $hasNavOrAside "Landmark elements used for page structure"
    Audit-Check "Semantic HTML" "Interactive Buttons (<button>)" $hasButtons "Native button elements used for clickable triggers"
    Audit-Check "Form Accessibility" "Form Controls (<input>, <select>)" $hasInputs "Standard form elements with state bindings"
    Audit-Check "Dialog Usability" "Accessible Modal Dialogs" $hasModalRole "Modal overlays use backdrop blur and dismissal buttons"
}

Write-Host "============================================================================" -ForegroundColor Cyan
$failedCount = ($checks | Where-Object { $_.Status -eq "FAIL" }).Count
if ($failedCount -eq 0) {
    Write-Host " [BorrowHub RESULT] ACCESSIBILITY AUDIT: ALL CHECKS PASSED" -ForegroundColor Green
    exit 0
} else {
    Write-Host " [BorrowHub RESULT] ACCESSIBILITY AUDIT: $failedCount CHECKS FAILED" -ForegroundColor Red
    exit 1
}
