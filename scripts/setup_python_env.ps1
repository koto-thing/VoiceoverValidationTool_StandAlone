#Requires -Version 5.1
[CmdletBinding()]
param(
  [string]$PythonExe = "python",
  [switch]$CpuOnly = $true,
  [switch]$Force = $false
)

$ErrorActionPreference = 'Stop'

Write-Host "=== VoiceValidator Python 環境セットアップ ===" -ForegroundColor Cyan
Write-Host "作業ディレクトリ: $(Get-Location)"

# 1) Python の存在確認とバージョン
try {
  $pyv = & $PythonExe -c "import sys;import platform;print(sys.version.split()[0])"
  Write-Host "Python: $pyv" -ForegroundColor Green
  $verParts = $pyv.Split('.') | ForEach-Object {[int]$_}
  if ($verParts[0] -lt 3 -or ($verParts[0] -eq 3 -and $verParts[1] -lt 10)) {
    throw "Python 3.10 以上を推奨します (検出: $pyv)"
  }
} catch {
  Write-Error "Python が見つかりません。PATH を確認するか、Microsoft Store/公式サイトからインストールしてください。"
  exit 1
}

# 2) venv 作成/再作成
$venvPath = Join-Path -Path (Join-Path -Path (Get-Location) -ChildPath 'python') -ChildPath 'venv'
$venvPython = Join-Path $venvPath 'Scripts\python.exe'
if (Test-Path $venvPath) {
  if ($Force) {
    Write-Host "既存の venv を削除します: $venvPath" -ForegroundColor Yellow
    Remove-Item -Recurse -Force $venvPath
  } else {
    Write-Host "既存の venv を利用します: $venvPath" -ForegroundColor Yellow
  }
}
if (!(Test-Path $venvPath)) {
  Write-Host "venv を作成します..." -ForegroundColor Cyan
  & $PythonExe -m venv $venvPath
}

if (!(Test-Path $venvPython)) {
  Write-Error "venv の作成に失敗しました: $venvPython が見つかりません。"
  exit 1
}

# 3) pip を最新化
Write-Host "pip を更新中..." -ForegroundColor Cyan
& $venvPython -m pip install --upgrade pip setuptools wheel

# 4) PyTorch のインストール (CPU のみが既定)
if ($CpuOnly) {
  Write-Host "PyTorch (CPU Only) をインストール中..." -ForegroundColor Cyan
  & $venvPython -m pip install --index-url https://download.pytorch.org/whl/cpu torch torchaudio
} else {
  Write-Host "PyTorch (デフォルトリポジトリ) をインストール中..." -ForegroundColor Cyan
  & $venvPython -m pip install torch torchaudio
}

# 5) 残りの依存をインストール
$req = Join-Path (Get-Location) 'requirements.txt'
if (Test-Path $req) {
  Write-Host "requirements.txt をインストール中..." -ForegroundColor Cyan
  & $venvPython -m pip install -r $req
} else {
  Write-Host "requirements.txt が見つかりません。主要パッケージを個別インストールします。" -ForegroundColor Yellow
  & $venvPython -m pip install SpeechRecognition openai-whisper pocketsphinx pydub numpy
}

# 6) FFmpeg の確認
try {
  $ffv = ffmpeg -version 2>$null
  Write-Host "FFmpeg 検出: OK" -ForegroundColor Green
} catch {
  Write-Warning "FFmpeg が見つかりません。mp3 入力や Whisper で必要です。以下のいずれかで導入してください:
  - winget:  winget install --id=Gyan.FFmpeg  --source=winget
  - choco :  choco install ffmpeg"
}

# 7) 簡易動作確認（埋め込み Python を一時ファイルで実行）
Write-Host "簡易動作確認を実行します..." -ForegroundColor Cyan
$code = @"
import sys
ok = True
for m in ("speech_recognition","whisper","pydub"):
    try:
        __import__(m)
        print(f"\u2713 imported {m}")
    except Exception as e:
        ok = False
        print(f"\u2717 failed {m}: {e}")
print("Python:", sys.version)
"@

$tmpPy = New-TemporaryFile
try {
  Set-Content -Path $tmpPy -Value $code -Encoding UTF8
  & $venvPython $tmpPy
} finally {
  Remove-Item -Force $tmpPy -ErrorAction SilentlyContinue
}

Write-Host "完了: venv Python は以下です:" -ForegroundColor Green
Write-Host $venvPython
Write-Host "アプリの Python パス欄には上記 python.exe を指定してください。" -ForegroundColor Yellow
