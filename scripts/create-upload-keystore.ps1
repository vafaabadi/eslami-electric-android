# Creates a Play Console upload keystore (Play App Signing recommended).
# Output goes to release/ (gitignored). Never commit the keystore or passwords.
#
# Usage (from project root):
#   .\scripts\create-upload-keystore.ps1
#   .\scripts\create-upload-keystore.ps1 -Alias myalias -ValidityYears 25

param(
    [string]$KeystorePath = "release/upload-keystore.jks",
    [string]$Alias = "upload",
    [int]$ValidityYears = 25,
    [string]$Dname = "CN=Eslami Electric, OU=Mobile, O=Eslami Electric, L=Unknown, ST=Unknown, C=GB"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $projectRoot

$keystoreFull = Join-Path $projectRoot $KeystorePath
$keystoreDir = Split-Path -Parent $keystoreFull
if (-not (Test-Path $keystoreDir)) {
    New-Item -ItemType Directory -Path $keystoreDir | Out-Null
}

if (Test-Path $keystoreFull) {
    Write-Error "Keystore already exists: $KeystoreFull. Delete it first or choose another path."
}

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    Write-Error "keytool not found. Install JDK 17+ and ensure JAVA_HOME/bin is on PATH."
}

Write-Host "Creating upload keystore at: $KeystoreFull"
Write-Host "Alias: $Alias  Validity: $ValidityYears years"
Write-Host ""
Write-Host "You will be prompted for store and key passwords. Use strong values and save them offline."
Write-Host ""

& keytool -genkeypair `
    -v `
    -keystore $KeystoreFull `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity ($ValidityYears * 365) `
    -storetype PKCS12 `
    -dname $Dname

Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Copy keystore.properties.example -> keystore.properties"
Write-Host "  2. Set STORE_FILE=$KeystorePath, KEY_ALIAS=$Alias, and your passwords"
Write-Host "  3. Run: gradlew.bat bundleRelease"
Write-Host "  4. Upload app/build/outputs/bundle/release/app-release.aab to Play Console (Internal testing)"
