@echo off
setlocal
REM One-shot: Gradle CLI with JDK 17 (does not change Windows system env permanently).
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0.."
echo JAVA_HOME=%JAVA_HOME%
call gradlew.bat --stop
call gradlew.bat --version
echo.
echo If JVM is 17 above, command-line builds are OK.
echo For Android Studio sync, set SYSTEM JAVA_HOME to JDK 17 and fully restart Studio.
echo See README.md section "Old Android Studio (Windows JAVA_HOME)".
endlocal
