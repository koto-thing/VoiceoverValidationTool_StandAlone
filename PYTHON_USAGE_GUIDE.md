## Pythonコードの動作確認と使用方法

### 1. 現在の動作状況
✅ Python環境: 正常動作
✅ SpeechRecognition: 正常動作  
✅ Google音声認識: 正常動作
❌ Whisper: FFmpeg不足で動作不可
❌ 日本語出力: エンコーディング問題

### 2. 基本的な使用方法

#### Google音声認識エンジンを使用：
```bash
python RecognizeAndCompare.py google ja-JP none input.json
```

#### 入力JSONファイルの形式：
```json
{
  "tasks": [
    {
      "id": "001",
      "audioPath": "test_data/audio/sample.wav",
      "scriptText": "こんにちは、これはテストです"
    }
  ]
}
```

### 3. Whisperエンジンを使用するには
FFmpegのインストールが必要です：

#### Windowsの場合：
1. https://ffmpeg.org/download.html からダウンロード
2. 実行ファイルをPATHに追加
3. または Chocolatey を使用: `choco install ffmpeg`

#### FFmpegインストール後：
```bash
python RecognizeAndCompare.py whisper ja tiny input.json
```

### 4. 文字エンコーディング問題の解決
Windows PowerShellで実行する場合：
```powershell
$OutputEncoding = [System.Text.Encoding]::UTF8
python RecognizeAndCompare.py google ja-JP none input.json
```

### 5. 実際の音声ファイルでのテスト
ビープ音ではなく、実際の音声ファイル（.wav, .mp3, .flac）を使用してください。

### 6. 結果の出力形式
```json
{
  "results": [
    {
      "id": "001",
      "similarity": 0.95,
      "script_text": "こんにちは、これはテストです",
      "recognized_text": "こんにちは、これは テストです",
      "diff": [差分情報],
      "error": null
    }
  ]
}
```

### 7. エラーハンドリング
- 音声ファイルが見つからない場合
- ネットワークエラー（Google API）
- 音声認識失敗の場合
すべてerrorフィールドに記録されます。
