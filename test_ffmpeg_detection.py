#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys

def test_ffmpeg_detection():
    """FFmpegの検出機能をテストする"""
    print("=== FFmpeg Detection Test ===")
    
    # 現在のスクリプトのディレクトリを取得
    script_dir = os.path.dirname(os.path.abspath(__file__))
    print(f"Script directory: {script_dir}")
    
    # ローカルインストールのFFmpegパスを確認
    local_ffmpeg = os.path.join(script_dir, "ffmpeg", "bin", "ffmpeg.exe")
    print(f"Local FFmpeg path: {local_ffmpeg}")
    print(f"Local FFmpeg exists: {os.path.exists(local_ffmpeg)}")
    
    if os.path.exists(local_ffmpeg):
        print(f"Local FFmpeg is executable: {os.access(local_ffmpeg, os.X_OK)}")
    
    # RecognizeAndCompareをインポートしてFFmpeg設定をテスト
    try:
        import RecognizeAndCompare
        ffmpeg_path = RecognizeAndCompare.configure_ffmpeg(verbose=True)
        print(f"Detected FFmpeg: {ffmpeg_path}")
        
        # 環境変数をチェック
        print(f"PATH contains ffmpeg dir: {'ffmpeg' in os.environ.get('PATH', '')}")
        print(f"IMAGEIO_FFMPEG_EXE: {os.environ.get('IMAGEIO_FFMPEG_EXE', 'Not set')}")
        
    except Exception as e:
        print(f"Error importing RecognizeAndCompare: {e}")
    
    # pydubがFFmpegを認識できるかテスト
    try:
        from pydub import AudioSegment
        print(f"pydub converter: {getattr(AudioSegment, 'converter', 'Not set')}")
        print(f"pydub ffprobe: {getattr(AudioSegment, 'ffprobe', 'Not set')}")
    except ImportError:
        print("pydub is not available")
    except Exception as e:
        print(f"Error testing pydub: {e}")

if __name__ == "__main__":
    test_ffmpeg_detection()
