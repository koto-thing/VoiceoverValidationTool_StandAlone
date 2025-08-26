#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import os

def test_environment():
    """Python環境をテストする"""
    print("=== Python Environment Test ===")
    print(f"Python version: {sys.version}")
    print(f"Current working directory: {os.getcwd()}")
    print(f"Python executable: {sys.executable}")
    
    # 必要なライブラリのインポートテスト
    try:
        import speech_recognition as sr
        print("✓ SpeechRecognition imported successfully")
    except ImportError as e:
        print(f"✗ SpeechRecognition import failed: {e}")
    
    try:
        import whisper
        print("✓ Whisper imported successfully")
    except ImportError as e:
        print(f"✗ Whisper import failed: {e}")
    
    try:
        import json
        print("✓ JSON module imported successfully")
    except ImportError as e:
        print(f"✗ JSON import failed: {e}")
    
    # テストファイルの存在確認
    test_files = [
        "simple_test.json",
        "test_data/audio/test_silence.wav",
        "RecognizeAndCompare.py"
    ]
    
    for file_path in test_files:
        if os.path.exists(file_path):
            print(f"✓ File exists: {file_path}")
        else:
            print(f"✗ File not found: {file_path}")

if __name__ == "__main__":
    test_environment()
