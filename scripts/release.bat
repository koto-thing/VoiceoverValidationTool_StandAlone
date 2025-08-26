@echo off
setlocal

REM Change to repo root (scripts/..)
cd /d %~dp0\..

REM Build distributions (ZIP/TAR) without tests
call gradlew.bat --no-daemon --console=plain clean build -x test
if errorlevel 1 goto :err

set "OUTDIR=build\distributions"
if not exist "%OUTDIR%" (
  echo Distributions directory not found: %OUTDIR%
  goto :err
)

REM Generate SHA-256 checksums via certutil (append mode)
set "SUMFILE=%OUTDIR%\SHA256SUMS.txt"
del /q "%SUMFILE%" 2>nul

for %%F in ("%OUTDIR%\*.zip") do (
  if exist "%%~fF" (
    for /f "tokens=1" %%H in ('certutil -hashfile "%%~fF" SHA256 ^| findstr /R "^[0-9A-F]"') do (
      echo %%H  %%~nxF>>"%SUMFILE%"
    )
  )
)
for %%F in ("%OUTDIR%\*.tar") do (
  if exist "%%~fF" (
    for /f "tokens=1" %%H in ('certutil -hashfile "%%~fF" SHA256 ^| findstr /R "^[0-9A-F]"') do (
      echo %%H  %%~nxF>>"%SUMFILE%"
    )
  )
)

if not exist "%SUMFILE%" goto :err

echo.
echo Release artifacts ready in %OUTDIR%
echo - ZIP/TAR files
echo - SHA256SUMS.txt
exit /b 0

:err
echo Release failed. See messages above.
exit /b 1
