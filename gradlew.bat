@echo off
setlocal EnableExtensions
set "APP_HOME=%~dp0"
set "GRADLE_VERSION=8.11.1"
set "GRADLE_SHA256=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6"
set "BOOT_DIR=%APP_HOME%.gradle-bootstrap"
set "GRADLE_HOME=%BOOT_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP=%BOOT_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
set "TMP_ZIP=%ZIP%.part"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BOOT_DIR%" mkdir "%BOOT_DIR%"
  if not exist "%ZIP%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    if exist "%TMP_ZIP%" del /q "%TMP_ZIP%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%TMP_ZIP%'"
    if errorlevel 1 exit /b 1
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$h=(Get-FileHash -Algorithm SHA256 '%TMP_ZIP%').Hash.ToLower(); if ($h -ne '%GRADLE_SHA256%') { exit 1 }"
    if errorlevel 1 (
      del /q "%TMP_ZIP%" 2>nul
      echo Gradle archive checksum verification failed.
      exit /b 1
    )
    move /y "%TMP_ZIP%" "%ZIP%" >nul
    if errorlevel 1 exit /b 1
  ) else (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$h=(Get-FileHash -Algorithm SHA256 '%ZIP%').Hash.ToLower(); if ($h -ne '%GRADLE_SHA256%') { exit 1 }"
    if errorlevel 1 (
      del /q "%ZIP%" 2>nul
      echo Cached Gradle archive checksum verification failed. Run again to re-download.
      exit /b 1
    )
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%BOOT_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
