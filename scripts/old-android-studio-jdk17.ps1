#Requires -Version 5.1
<#
.SYNOPSIS
  Prints steps to fix Android Studio Gradle sync when status bar shows Android Studio\jre (Java 8).
.NOTES
  Does not modify system environment variables (requires admin / GUI). Updates user ~/.gradle/gradle.properties.
#>
$ErrorActionPreference = "Stop"
$Jdk17 = "C:\Program Files\Java\jdk-17"
$UserGradleProps = Join-Path $env:USERPROFILE ".gradle\gradle.properties"
$jdkLine = "org.gradle.java.home=C\:\\Program Files\\Java\\jdk-17"

Write-Host "=== Eslami Electric Android: Studio JDK 17 helper ===" -ForegroundColor Cyan
Write-Host ""
Write-Host ("Detected JAVA_HOME (process): " + $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "(not set)" }))
if ($env:JAVA_HOME -match '1\.8|jdk1\.8|jdk-8') {
  Write-Host "WARNING: JAVA_HOME points at Java 8. Android Studio often uses this for Gradle sync." -ForegroundColor Yellow
}
Write-Host ""
Write-Host "1) Windows System Environment Variables (recommended for old Android Studio):"
Write-Host "   - JAVA_HOME = $Jdk17"
Write-Host "   - Path: prepend %JAVA_HOME%\bin (above any Java 8 paths)"
Write-Host "   - Fully quit Android Studio, reopen"
Write-Host "   - File -> Invalidate Caches / Restart"
Write-Host ""
Write-Host "2) Android Studio: File -> Project Structure -> SDK Location -> JDK location = $Jdk17"
Write-Host "   (Newer Studio: Settings -> Build Tools -> Gradle -> Gradle JDK = 17 / jbr-17)"
Write-Host ""

$dir = Split-Path $UserGradleProps
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$lines = @()
if (Test-Path $UserGradleProps) {
  $lines = Get-Content $UserGradleProps -Encoding UTF8 | Where-Object { $_ -notmatch '^\s*org\.gradle\.java\.home\s*=' }
}
if ($lines.Count -gt 0 -and $lines[-1] -ne "") { $lines += "" }
$lines += "# Android Studio / AGP 8.x: force Gradle daemon JDK 17"
$lines += $jdkLine
$lines | Set-Content -Path $UserGradleProps -Encoding UTF8
Write-Host "Updated: $UserGradleProps" -ForegroundColor Green
Write-Host ""
Write-Host "3) Best long-term fix: update Android Studio to Ladybug (2024.2+) so Gradle uses embedded JBR 17."
Write-Host ""
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
$env:JAVA_HOME = $Jdk17
$env:Path = "$Jdk17\bin;" + $env:Path
& .\gradlew.bat --version
Pop-Location


