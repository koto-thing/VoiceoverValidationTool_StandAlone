#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import numpy as np
import wave
from scipy.io.wavfile import write

def create_test_audio_files():
    """テスト用の音声ファイルを作成"""
    
    # ディレクトリが存在しない場合は作成
    audio_dir = "test_data/audio"
    os.makedirs(audio_dir, exist_ok=True)
    
    # 1. 無音ファイル（1秒間）
    silence_duration = 1.0  # 秒
    sample_rate = 16000  # Hz
    silence_samples = int(silence_duration * sample_rate)
    silence_data = np.zeros(silence_samples, dtype=np.int16)
    
    silence_path = os.path.join(audio_dir, "test_silence.wav")
    write(silence_path, sample_rate, silence_data)
    print(f"Created: {silence_path}")
    
    # 2. 簡単なビープ音ファイル（1秒間、440Hz）
    beep_duration = 1.0  # 秒
    frequency = 440  # Hz (A音)
    beep_samples = int(beep_duration * sample_rate)
    t = np.linspace(0, beep_duration, beep_samples, False)
    beep_data = (np.sin(2 * np.pi * frequency * t) * 32767 * 0.3).astype(np.int16)
    
    beep_path = os.path.join(audio_dir, "test_beep.wav")
    write(beep_path, sample_rate, beep_data)
    print(f"Created: {beep_path}")
    
    # 3. ファイル情報を表示
    for filename in ["test_silence.wav", "test_beep.wav"]:
        filepath = os.path.join(audio_dir, filename)
        if os.path.exists(filepath):
            size = os.path.getsize(filepath)
            print(f"File: {filename}, Size: {size} bytes")
        else:
            print(f"Failed to create: {filename}")

if __name__ == "__main__":
    try:
        import scipy.io.wavfile
        create_test_audio_files()
    except ImportError:
        print("scipy is required. Installing...")
        import subprocess
        subprocess.check_call(["pip", "install", "scipy"])
        import scipy.io.wavfile
        create_test_audio_files()
