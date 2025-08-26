@echo off
echo Voice Validation Tool を起動中...

:: FFmpegのパスを環境変数に追加
set FFMPEG_BIN=%~dp0ffmpeg\bin
set PATH=%FFMPEG_BIN%;%PATH%
set IMAGEIO_FFMPEG_EXE=%FFMPEG_BIN%\ffmpeg.exe

:: Pythonの仮想環境をアクティベート（存在する場合）
if exist "venv\Scripts\activate.bat" (
    echo Python仮想環境をアクティベート中...
    call venv\Scripts\activate.bat
) else (
    echo 注意: Python仮想環境が見つかりません。システムのPythonを使用します。
)

:: Javaアプリケーションを起動
echo アプリケーションを起動中...
if exist "bin\VoiceValidator.bat" (
    call bin\VoiceValidator.bat
) else (
    echo エラー: VoiceValidator.batが見つかりません。
    echo 配布ファイルが正しく展開されているか確認してください。
    pause
    exit /b 1
)

pause
