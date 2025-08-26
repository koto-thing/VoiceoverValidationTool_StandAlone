# FFmpeg自動インストールスクリプト for Windows
# 管理者権限で実行することを推奨

param(
    [string]$InstallPath = "C:\ffmpeg"
)

Write-Host "FFmpeg自動インストールスクリプト" -ForegroundColor Green
Write-Host "インストール先: $InstallPath" -ForegroundColor Yellow

# 既存のFFmpegをチェック
$ffmpegExe = Get-Command ffmpeg -ErrorAction SilentlyContinue
if ($ffmpegExe) {
    Write-Host "FFmpegは既にインストールされています: $($ffmpegExe.Source)" -ForegroundColor Green
    ffmpeg -version
    exit 0
}

# FFmpegダウンロードURL（Windows用の公式ビルド）
$ffmpegUrl = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
$tempDir = [System.IO.Path]::GetTempPath()
$zipFile = Join-Path $tempDir "ffmpeg.zip"
$extractDir = Join-Path $tempDir "ffmpeg_extract"

try {
    Write-Host "FFmpegをダウンロード中..." -ForegroundColor Yellow
    
    # プログレスバー付きでダウンロード
    $webClient = New-Object System.Net.WebClient
    $webClient.DownloadFile($ffmpegUrl, $zipFile)
    
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
    
    # 環境変数PATHに追加
    $binPath = Join-Path $InstallPath "bin"
    $currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
    
    if ($currentPath -notlike "*$binPath*") {
        Write-Host "環境変数PATHに追加中..." -ForegroundColor Yellow
        $newPath = "$currentPath;$binPath"
        [Environment]::SetEnvironmentVariable("PATH", $newPath, "Machine")
        
        # 現在のセッションでも有効にする
        $env:PATH = "$env:PATH;$binPath"
    }
    
    # クリーンアップ
    Remove-Item -Force $zipFile -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $extractDir -ErrorAction SilentlyContinue
    
    Write-Host "FFmpegのインストールが完了しました！" -ForegroundColor Green
    Write-Host "バージョンを確認中..." -ForegroundColor Yellow
    
    # インストール確認
    & "$binPath\ffmpeg.exe" -version | Select-Object -First 1
    
    Write-Host ""
    Write-Host "注意: 新しいPowerShellセッションを開始するか、コンピューターを再起動してください。" -ForegroundColor Red
    Write-Host "環境変数の変更を反映させるために必要です。" -ForegroundColor Red
    
} catch {
    Write-Host "エラーが発生しました: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
