@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Logisim Better Mem - multi-port RAM build
cd /d "%~dp0"

rem =====================================================================
rem  Logisim Better Mem launcher
rem  - finds a JDK 21+ (JAVA_HOME first, then common install dirs)
rem  - finds a Gradle installation (cached wrapper distribution first)
rem  - builds and starts the application from this folder (New)
rem
rem  Usage:
rem    run-logisim.bat                 start Logisim
rem    run-logisim.bat file.circ       start and open a project file
rem    set LOGISIM_ONLINE=1            allow Gradle to use the network
rem =====================================================================

rem ---------------------------------------------------------------
rem 1. Locate a JDK
rem ---------------------------------------------------------------
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
  for %%V in (27 26 25 24 23 22 21) do (
    if not defined JAVA_EXE if exist "C:\Program Files\Java\jdk-%%V\bin\java.exe" (
      set "JAVA_HOME=C:\Program Files\Java\jdk-%%V"
      set "JAVA_EXE=!JAVA_HOME!\bin\java.exe"
    )
  )
)
if not defined JAVA_EXE (
  for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%~fJ"
)
if not defined JAVA_EXE (
  echo [ERROR] No Java runtime found. Install JDK 21+ or set JAVA_HOME.
  pause
  exit /b 1
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

rem ---------------------------------------------------------------
rem 2. Locate Gradle: cached distributions first, then wrapper/PATH
rem ---------------------------------------------------------------
set "GRADLE="
for /d %%D in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-9.7.1-bin\*") do (
  if exist "%%~fD\gradle-9.7.1\bin\gradle.bat" set "GRADLE=%%~fD\gradle-9.7.1\bin\gradle.bat"
)
if not defined GRADLE (
  for /d %%D in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-9.8.1-bin\*") do (
    if exist "%%~fD\gradle-9.8.1\bin\gradle.bat" set "GRADLE=%%~fD\gradle-9.8.1\bin\gradle.bat"
  )
)
if not defined GRADLE (
  for /d %%D in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-*") do (
    for /d %%E in ("%%~fD\*") do (
      for /d %%G in ("%%~fE\gradle-*") do (
        if exist "%%~fG\bin\gradle.bat" set "GRADLE=%%~fG\bin\gradle.bat"
      )
    )
  )
)
if not defined GRADLE if exist "%~dp0gradlew.bat" set "GRADLE=%~dp0gradlew.bat"
if not defined GRADLE (
  for /f "delims=" %%G in ('where gradle 2^>nul') do if not defined GRADLE set "GRADLE=%%~fG"
)
if not defined GRADLE (
  echo [ERROR] No Gradle found. Install Gradle or keep the gradle wrapper folder.
  pause
  exit /b 1
)

rem ---------------------------------------------------------------
rem 3. Run
rem ---------------------------------------------------------------
set "OFFLINE=--offline"
if /i "%LOGISIM_ONLINE%"=="1" set "OFFLINE="

echo [info] JAVA_HOME = %JAVA_HOME%
echo [info] Gradle    = %GRADLE%
echo [info] Starting Logisim Better Mem ...
echo.

if "%~1"=="" (
  call "%GRADLE%" %OFFLINE% --console=plain run
) else (
  call "%GRADLE%" %OFFLINE% --console=plain run --args="%*"
)
set "RC=%ERRORLEVEL%"

if not "%RC%"=="0" (
  echo.
  echo [ERROR] Launch/build failed with exit code %RC%.
  echo         If dependencies are missing from the offline cache, run:
  echo             set LOGISIM_ONLINE=1
  echo         and start this script again.
  pause
)
exit /b %RC%
