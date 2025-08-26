#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import os
import json
from datetime import datetime

def write_test_log(message):
    """テストログをファイルに書き込む"""
    with open("test_log_sr.txt", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now()}] {message}\n")

def test_speech_recognition():
    """SpeechRecognitionライブラリでのテストを実行"""
    write_test_log("=== Starting SpeechRecognition Test ===")
    
    try:
        import speech_recognition as sr
        write_test_log("✓ SpeechRecognition imported successfully")
        
        # テスト音声ファイル
        test_audio = "test_data/audio/test_beep.wav"  # ビープ音で試す
        
        if os.path.exists(test_audio):
            write_test_log(f"Testing recognition on: {test_audio}")
            
            recognizer = sr.Recognizer()
            try:
                with sr.AudioFile(test_audio) as source:
                    audio = recognizer.record(source)
                    write_test_log("✓ Audio file loaded successfully")
                    
                    # Google音声認識を試す（インターネット接続が必要）
                    try:
                        result = recognizer.recognize_google(audio, language="ja-JP")
                        write_test_log(f"Google recognition result: '{result}'")
                    except sr.UnknownValueError:
                        write_test_log("Google could not understand the audio")
                    except sr.RequestError as e:
                        write_test_log(f"Google recognition request failed: {e}")
                        
            except Exception as audio_error:
                write_test_log(f"Audio processing error: {audio_error}")
        else:
            write_test_log(f"Audio file not found: {test_audio}")
            
    except ImportError as e:
        write_test_log(f"✗ SpeechRecognition import failed: {e}")
        return False
    
    write_test_log("=== SpeechRecognition Test completed ===")
    return True

if __name__ == "__main__":
    # ログファイルをクリア
    if os.path.exists("test_log_sr.txt"):
        os.remove("test_log_sr.txt")
    
    test_speech_recognition()
    print("SpeechRecognition test completed. Check test_log_sr.txt for results.")
