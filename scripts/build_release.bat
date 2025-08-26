@echo off
setlocal enabledelayedexpansion

echo [INFO] Building VoiceValidator release packages...

REM 既存のビルド成果物をクリーンアップ
echo [INFO] Cleaning previous build...
call gradlew clean

REM リリースパッケージをビルド
echo [INFO] Building release packages...
call gradlew buildRelease

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed with error code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo [INFO] Build completed successfully!
echo [INFO] Distribution packages created in build\distributions\
echo.

REM 作成されたファイルを表示
if exist "build\distributions\" (
    echo [INFO] Created files:
    dir /b "build\distributions\VoiceValidator-*"
    echo.
    
    REM SHA-256チェックサムを生成
    echo [INFO] Generating SHA-256 checksums...
    cd build\distributions
    
    REM SHA256SUMSファイルを作成
    if exist "SHA256SUMS.txt" del "SHA256SUMS.txt"
    
    REM 各ファイルのチェックサムを計算
    for %%f in (VoiceValidator-*.zip VoiceValidator-*.tar.gz) do (
        echo [INFO] Calculating checksum for %%f...
        powershell -Command "Get-FileHash '%%f' -Algorithm SHA256 | Select-Object -ExpandProperty Hash" > temp_hash.txt
        set /p hash=<temp_hash.txt
        echo !hash! *%%f >> SHA256SUMS.txt
        del temp_hash.txt
    )
    
    cd ..\..
    
    echo [INFO] Checksums generated in build\distributions\SHA256SUMS.txt
    echo.
    echo [INFO] Distribution files and checksums:
    type "build\distributions\SHA256SUMS.txt"
    echo.
    
    echo [INFO] You can find the distribution packages at:
    echo %CD%\build\distributions\
) else (
    echo [WARNING] Distribution directory not found
)

echo.
echo [INFO] Release build complete!
echo [INFO] Files ready for distribution:
echo   - VoiceValidator-1.0.0.zip (Windows)
echo   - VoiceValidator-1.0.0.tar.gz (Linux/macOS)
echo   - SHA256SUMS.txt (Integrity verification)
echo.
pause
