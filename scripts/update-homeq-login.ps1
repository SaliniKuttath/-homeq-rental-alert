# Renews your saved HomeQ login (needed about once a week) and uploads it to GitHub,
# so alert emails can show your queue position ("Du är nummer N i kön").
#
# Run from the project folder:
#   powershell -ExecutionPolicy Bypass -File scripts\update-homeq-login.ps1
#
# A browser opens: log in to HomeQ yourself, then come back here and press Enter.
# Your password is never stored; only HomeQ's login token is saved to data\homeq-session.json.

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$repo = 'SaliniKuttath/-homeq-rental-alert'

Write-Host "Building..." -ForegroundColor Cyan
mvn -q compile
if ($LASTEXITCODE -ne 0) { throw "Build failed." }

Write-Host "Opening HomeQ. Log in there, then come back to THIS window and press Enter." -ForegroundColor Cyan
mvn -q exec:java "-Dexec.mainClass=com.homeq.alert.tools.HomeQLogin"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path data\homeq-session.json)) { throw "Login was not saved." }

$gh = 'C:\Program Files\GitHub CLI\gh.exe'
if (-not (Test-Path $gh)) { $gh = 'gh' }
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
Get-Content data\homeq-session.json -Raw | & $gh secret set HOMEQ_SESSION --repo $repo
if ($LASTEXITCODE -ne 0) { throw "Uploading to GitHub failed. Try: gh auth login" }

Write-Host "Done! HomeQ login updated on GitHub. Queue positions will show in alerts for the next 7 days." -ForegroundColor Green
