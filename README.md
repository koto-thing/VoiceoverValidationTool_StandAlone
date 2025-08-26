# Voice Validation Tool - セットアップガイド

## 必要な環境
- Java 17以上
- Python 3.13.3 (インストール済み)
- 必要なライブラリ (インストール済み)

## 配布パッケージについて

このアプリケーションは以下の形式で配布されます：
- **VoiceValidator-1.0.0.zip** - Windows用ZIP形式
- **VoiceValidator-1.0.0.tar.gz** - Linux/macOS用TAR.GZ形式

## インストール方法

### Windows (ZIP形式)
1. `VoiceValidator-1.0.0.zip` をダウンロード
2. 任意のフォルダに解凍
3. 解凍したフォルダ内の `VoiceValidator/bin/VoiceValidator.bat` を実行

### Linux/macOS (TAR.GZ形式)
1. `VoiceValidator-1.0.0.tar.gz` をダウンロード
2. 解凍: `tar -xzf VoiceValidator-1.0.0.tar.gz`
3. 実行権限を付与: `chmod +x VoiceValidator/bin/VoiceValidator`
4. 実行: `./VoiceValidator/bin/VoiceValidator`

## システム要件
- Java 17以上がインストールされている必要があります
- Python 3.13.3以上（音声認識機能を使用する場合）

## インストール済みライブラリ
- SpeechRecognition: Google音声認識とSphinx用
- openai-whisper: OpenAI Whisper音声認識用
- pocketsphinx: オフライン音声認識用
- pydub: 音声ファイル処理用
- torch/torchaudio: Whisper用の機械学習ライブラリ

## 使用方法

### GUI アプリケーション
配布パッケージを解凍後、以下のファイルを実行してください：
- Windows: `VoiceValidator/bin/VoiceValidator.bat`
- Linux/macOS: `VoiceValidator/bin/VoiceValidator`

### コマンドライン（直接Python実行）
```
python RecognizeAndCompare.py <engine> <language_code> <whisper_model> <json_file>
```

### 2. パラメータの説明
- engine: 音声認識エンジン (google, whisper, sphinx)
- language_code: 言語コード (ja-JP, ja, en-US など)
- whisper_model: Whisperモデル名 (tiny, base, small, medium, large)
- json_file: 処理タスクが記述されたJSONファイルのパス

### 3. 使用例
```
# Google音声認識を使用
python RecognizeAndCompare.py google ja-JP none test_input.json

# Whisperを使用
python RecognizeAndCompare.py whisper ja base test_input.json

# Sphinxを使用
python RecognizeAndCompare.py sphinx en-US none test_input.json
```

### 4. 入力JSONファイルの形式
```json
{
  "tasks": [
    {
      "id": "001",
      "audioPath": "path/to/audio.wav",
      "scriptText": "期待されるテキスト内容"
    }
  ]
}
```

## 注意事項
- Google音声認識: インターネット接続が必要
- Whisper: 初回実行時にモデルファイルがダウンロードされます
- Sphinx: 日本語認識には追加の言語モデルが必要

## トラブルシューティング
音声ファイルがエラーになる場合は、以下の形式を確認してください：
- 対応形式: WAV, MP3, FLAC
- 推奨: WAV形式、16kHz、モノラル

---

## リリース手順（Windows）
1) バージョンを確認（build.gradle の version）
2) 配布物の作成とハッシュ生成:
   - scripts/release.bat を実行すると ZIP/TAR を作成し、SHA-256 のチェックサムを出力します。
3) 成果物の場所:
   - build/distributions/VoiceValidator-<version>.zip
   - build/distributions/VoiceValidator-<version>.tar
   - build/distributions/SHA256SUMS.txt
4) 起動確認:
   - ZIP を展開 → bin/VoiceValidator.bat で GUI を起動
   - start_voice_validator.bat でも起動可（FFmpeg と Python venv をセットしてから gradle run）
