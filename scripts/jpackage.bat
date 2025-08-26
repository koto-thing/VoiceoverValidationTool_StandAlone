@echo off
setlocal EnableExtensions

REM Move to repo root (scripts\..)
cd /d %~dp0\..

REM ----------------------------------------
REM Args: --jpackage <path-to-jpackage.exe> | --jdk <jdk-home> | --type <exe|msi|app-image> | --help
REM ----------------------------------------
set "JP_CMD="
set "PKG_TYPE=exe"
if "%~1"=="" goto :args_done
:parse_args
if /I "%~1"=="--help" goto :usage
if /I "%~1"=="-h" goto :usage
if /I "%~1"=="--jpackage" (
  if "%~2"=="" goto :usage
  set "JP_CMD=%~2"
  shift & shift & goto :parse_args
)
if /I "%~1"=="--jdk" (
  if "%~2"=="" goto :usage
  set "JP_CMD=%~2\bin\jpackage.exe"
  shift & shift & goto :parse_args
)
if /I "%~1"=="--type" (
  if "%~2"=="" goto :usage
  set "PKG_TYPE=%~2"
  shift & shift & goto :parse_args
)
if not "%~1"=="" (
  echo [ERROR] Unknown argument: %~1
  goto :usage
)
:args_done

echo [INFO] Preparing dependencies for jpackage...
call gradlew.bat --no-daemon --console=plain clean prepareJpackage
if errorlevel 1 (
  echo [WARN] clean failed. Retrying without clean...
  call gradlew.bat --no-daemon --console=plain prepareJpackage
  if errorlevel 1 goto :err_prep
)

echo [INFO] Reading app version from Gradle...
set "VERSION_FILE=%TEMP%\version.tmp"
call gradlew.bat --no-daemon --console=plain -q printVersion > "%VERSION_FILE%"
if errorlevel 1 goto :err_ver
set /p APP_VERSION=<"%VERSION_FILE%"
del "%VERSION_FILE%" 2>nul
if not defined APP_VERSION set APP_VERSION=1.0.0

echo [INFO] Version=%APP_VERSION%

echo [TRACE] Setting variables
set "APP_NAME=VoiceValidator"
set "APP_VENDOR=koto_thing"
set "MAIN_JAR=%APP_NAME%-%APP_VERSION%.jar"
set "MAIN_CLASS=koto_thing.voiceover_validator.VoiceoverValidationApp"

set "INPUT_DIR=build\jpackage\lib"
set "OUT_DIR=build\jpackage"

echo [TRACE] Checking main jar exists: %INPUT_DIR%\%MAIN_JAR%
if not exist "%INPUT_DIR%\%MAIN_JAR%" goto :err_nojar

echo [TRACE] Locating jpackage
REM JP_CMD override (validate if provided)
if defined JP_CMD goto :check_jpcmd

REM PATH check via where
where /Q jpackage.exe
if not errorlevel 1 set "JP_CMD=jpackage.exe"
if defined JP_CMD goto :have_jpcmd

REM JAVA_HOME fallback
if defined JAVA_HOME (
  set "JP_CMD=%JAVA_HOME%\bin\jpackage.exe"
  if exist "%JP_CMD%" goto :have_jpcmd
)

REM Gradle java.home fallback
set "JH_FILE=%TEMP%\java_home.tmp"
call gradlew.bat --no-daemon --console=plain -q printJavaHome > "%JH_FILE%"
if not errorlevel 1 (
  set "GRADLE_JAVA_HOME="
  set /p GRADLE_JAVA_HOME=<"%JH_FILE%"
  del "%JH_FILE%" 2>nul
  if defined GRADLE_JAVA_HOME (
    set "JP_CMD=%GRADLE_JAVA_HOME%\bin\jpackage.exe"
    if exist "%JP_CMD%" goto :have_jpcmd
  )
) else (
  del "%JH_FILE%" 2>nul
)

REM Probe common JDK install locations (Program Files)
set "CAND="
if defined ProgramFiles call :probe_root "%ProgramFiles%"
if not defined CAND if defined ProgramW6432 call :probe_root "%ProgramW6432%"
if not defined CAND if defined ProgramFiles(x86) call :probe_root "%ProgramFiles(x86)%"
if defined CAND (
  set "JP_CMD=%CAND%"
  goto :have_jpcmd
)

REM No jpackage found
goto :err_nojp

:probe_root
setlocal
set "ROOT=%~1"
for /d %%V in ("%ROOT%\Java\jdk-*") do if exist "%%~fV\bin\jpackage.exe" set "FOUND=%%~fV\bin\jpackage.exe"
for /d %%V in ("%ROOT%\Microsoft\jdk-*") do if exist "%%~fV\bin\jpackage.exe" set "FOUND=%%~fV\bin\jpackage.exe"
for /d %%V in ("%ROOT%\Eclipse Adoptium\jdk-*") do if exist "%%~fV\bin\jpackage.exe" set "FOUND=%%~fV\bin\jpackage.exe"
for /d %%V in ("%ROOT%\Zulu\zulu-*") do if exist "%%~fV\bin\jpackage.exe" set "FOUND=%%~fV\bin\jpackage.exe"
for /d %%V in ("%ROOT%\AdoptOpenJDK\jdk-*") do if exist "%%~fV\bin\jpackage.exe" set "FOUND=%%~fV\bin\jpackage.exe"
endlocal & if defined FOUND set "CAND=%FOUND%"
exit /b 0

:have_jpcmd
echo [INFO] Using jpackage: %JP_CMD%

REM Build args file to avoid quoting/whitespace issues
echo [TRACE] Preparing args file
set "ARGS_FILE=%TEMP%\jpackage_args_%RANDOM%%RANDOM%.txt"
> "%ARGS_FILE%" (
  echo --type
  echo %PKG_TYPE%
  echo --name
  echo %APP_NAME%
  echo --app-version
  echo %APP_VERSION%
  echo --vendor
  echo %APP_VENDOR%
  echo --input
  echo %INPUT_DIR%
  echo --main-jar
  echo %MAIN_JAR%
  echo --main-class
  echo %MAIN_CLASS%
  echo --dest
  echo %OUT_DIR%
)

REM Add Windows installer-only options when not building app-image
if /I not "%PKG_TYPE%"=="app-image" (
  >> "%ARGS_FILE%" echo --win-menu
  >> "%ARGS_FILE%" echo --win-shortcut
  >> "%ARGS_FILE%" echo --win-console
)

echo [TRACE] Args file: %ARGS_FILE%

echo [TRACE] Running jpackage
"%JP_CMD%" @"%ARGS_FILE%"
set "JP_EXIT=%ERRORLEVEL%"
del "%ARGS_FILE%" 2>nul
if not "%JP_EXIT%"=="0" goto :err_jpackage


echo.
echo [OK] Installer created in %OUT_DIR%
exit /b 0

:check_jpcmd
"%JP_CMD%" --version 1>nul 2>nul
if errorlevel 1 goto :err_bad_jpcmd
goto :have_jpcmd

:usage
echo Usage: %~nx0 [--jpackage "C:\\Path\\to\\jpackage.exe"] ^| [--jdk "C:\\Path\\to\\JDK"] [--type exe^|msi^|app-image]
echo Example: %~nx0 --jpackage "C:\\Program Files\\Java\\jdk-21\\bin\\jpackage.exe" --type exe
echo Example: %~nx0 --jdk "C:\\Program Files\\Java\\jdk-21" --type msi
exit /b 0

:err_prep
echo [ERROR] Gradle prepareJpackage failed.
exit /b 1

:err_ver
echo [ERROR] Failed to get version from Gradle.
del "%VERSION_FILE%" 2>nul
exit /b 1

:err_nojar
echo [ERROR] Main jar not found: %INPUT_DIR%\%MAIN_JAR%
exit /b 1

:err_bad_jpcmd
echo [ERROR] JP_CMD is set but not working: %JP_CMD%
echo         Verify the jpackage path/command.
exit /b 1

:err_nojp
echo [ERROR] jpackage not found via PATH, JP_CMD, JAVA_HOME, Gradle java.home, or common JDK locations.
echo         Options:
echo          - Put jpackage.exe on PATH, or

echo          - Set JP_CMD to full path of jpackage.exe, or

echo          - Set JAVA_HOME to a JDK that includes jpackage, or

echo          - Ensure Gradle toolchain uses a JDK with jpackage.
exit /b 1

:err_nojp_path
echo [ERROR] jpackage not found at: %JP_CMD%
echo         Use a JDK that ships jpackage (Oracle JDK / MS Build of OpenJDK), or put jpackage on PATH.
exit /b 1

:err_jpackage
echo [ERROR] jpackage failed.
exit /b 1
