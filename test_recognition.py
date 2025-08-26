#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import os
import json
from datetime import datetime

def write_test_log(message):
    """テストログをファイルに書き込む"""
    with open("test_log.txt", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now()}] {message}\n")

def test_simple_recognition():
    """シンプルな音声認識テストを実行"""
    write_test_log("=== Starting Python Environment Test ===")
    write_test_log(f"Python version: {sys.version}")
    write_test_log(f"Current working directory: {os.getcwd()}")
    
    # 必要なライブラリのインポートテスト
    try:
        import speech_recognition as sr
        write_test_log("✓ SpeechRecognition imported successfully")
    except ImportError as e:
        write_test_log(f"✗ SpeechRecognition import failed: {e}")
        return False
    
    try:
        import whisper
        write_test_log("✓ Whisper imported successfully")
    except ImportError as e:
        write_test_log(f"✗ Whisper import failed: {e}")
        return False
    
    # ファイル存在確認
    test_files = [
        "simple_test.json",
        "test_data/audio/test_silence.wav",
        "RecognizeAndCompare.py"
    ]
    
    for file_path in test_files:
        if os.path.exists(file_path):
            write_test_log(f"✓ File exists: {file_path}")
        else:
            write_test_log(f"✗ File not found: {file_path}")
    
    # simple_test.jsonの内容確認
    try:
        with open("simple_test.json", "r", encoding="utf-8-sig") as f:
            data = json.load(f)
            write_test_log(f"✓ JSON loaded successfully: {json.dumps(data, ensure_ascii=False)}")
    except Exception as e:
        write_test_log(f"✗ Failed to load JSON: {e}")
    
    # Whisperモデルロードテスト（軽量モデル）
    try:
        write_test_log("Loading Whisper tiny model...")
        model = whisper.load_model("tiny")
        write_test_log("✓ Whisper tiny model loaded successfully")
        
        # テスト音声ファイルが存在する場合は認識テスト
        test_audio = "test_data/audio/test_silence.wav"
        test_audio_abs = os.path.abspath(test_audio)
        write_test_log(f"Audio file path: {test_audio}")
        write_test_log(f"Absolute audio file path: {test_audio_abs}")
        
        if os.path.exists(test_audio):
            write_test_log(f"Testing recognition on: {test_audio_abs}")
            try:
                result = model.transcribe(test_audio_abs, language="ja")
                write_test_log(f"Recognition result: '{result['text']}'")
            except Exception as transcribe_error:
                write_test_log(f"Transcription error: {transcribe_error}")
        else:
            write_test_log(f"Audio file not found: {test_audio}")
    
    except Exception as e:
        write_test_log(f"✗ Whisper test failed: {e}")
    
    write_test_log("=== Test completed ===")
    return True

if __name__ == "__main__":
    # ログファイルをクリア
    if os.path.exists("test_log.txt"):
        os.remove("test_log.txt")
    
    test_simple_recognition()
    print("Test completed. Check test_log.txt for results.")
