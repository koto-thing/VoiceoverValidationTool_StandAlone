# FFmpeg Portable インストールスクリプト (管理者権限不要)
param(
    [string]$InstallPath = "D:\Pandd\VoiceValidationTool\VoiceValidator\ffmpeg"
)

Write-Host "FFmpeg Portable インストールスクリプト" -ForegroundColor Green
Write-Host "インストール先: $InstallPath" -ForegroundColor Yellow

# FFmpegダウンロードURL（Windows用の公式ビルド）
$ffmpegUrl = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
$tempDir = [System.IO.Path]::GetTempPath()
$zipFile = Join-Path $tempDir "ffmpeg.zip"
$extractDir = Join-Path $tempDir "ffmpeg_extract"

try {
    Write-Host "FFmpegをダウンロード中..." -ForegroundColor Yellow
    
    # ダウンロード
    Invoke-WebRequest -Uri $ffmpegUrl -OutFile $zipFile -UseBasicParsing
    
    Write-Host "ダウンロード完了。展開中..." -ForegroundColor Yellow
    
    # 展開
    if (Test-Path $extractDir) {
        Remove-Item -Recurse -Force $extractDir
    }
    Expand-Archive -Path $zipFile -DestinationPath $extractDir -Force
    
    # FFmpegフォルダを探す
    $ffmpegFolder = Get-ChildItem -Path $extractDir -Directory | Where-Object { $_.Name -like "ffmpeg-*" } | Select-Object -First 1
    
    if (-not $ffmpegFolder) {
        throw "FFmpegフォルダが見つかりません"
    }
    
    # インストール先にコピー
    Write-Host "FFmpegを $InstallPath にインストール中..." -ForegroundColor Yellow
    
    if (Test-Path $InstallPath) {
        Remove-Item -Recurse -Force $InstallPath
    }
    
    New-Item -ItemType Directory -Path $InstallPath -Force | Out-Null
    Copy-Item -Recurse -Path "$($ffmpegFolder.FullName)\*" -Destination $InstallPath -Force
    
    # クリーンアップ
    Remove-Item -Force $zipFile -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $extractDir -ErrorAction SilentlyContinue
    
    # インストール確認
    $ffmpegExe = Join-Path $InstallPath "bin\ffmpeg.exe"
    if (Test-Path $ffmpegExe) {
        Write-Host "FFmpegのインストールが完了しました！" -ForegroundColor Green
        Write-Host "場所: $ffmpegExe" -ForegroundColor Yellow
        
        # バージョン確認
        & $ffmpegExe -version | Select-Object -First 1
        
        # 設定ファイルを作成
        $configPath = "D:\Pandd\VoiceValidationTool\VoiceValidator\ffmpeg_config.txt"
        $ffmpegExe | Out-File -FilePath $configPath -Encoding utf8
        Write-Host "設定ファイルを作成しました: $configPath" -ForegroundColor Green
        
    } else {
        throw "FFmpegの実行ファイルが見つかりません"
    }
    
} catch {
    Write-Host "エラーが発生しました: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
